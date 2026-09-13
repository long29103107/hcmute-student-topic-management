package com.hcmute.topicmanagement.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.EvaluationEntity;
import com.hcmute.topicmanagement.model.ReviewBoardEntity;
import com.hcmute.topicmanagement.model.ReviewBoardMemberEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.ReviewBoardMemberRole;
import com.hcmute.topicmanagement.model.enums.ReviewBoardStatus;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
import com.hcmute.topicmanagement.repository.EvaluationRepository;
import com.hcmute.topicmanagement.repository.ReviewBoardMemberRepository;
import com.hcmute.topicmanagement.repository.ReviewBoardRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class ReviewBoardService {

    private static final DateTimeFormatter FORM_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int MIN_MEMBERS = 3;
    private static final int MAX_MEMBERS = 5;

    private final ReviewBoardRepository reviewBoardRepository;
    private final ReviewBoardMemberRepository memberRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EvaluationRepository evaluationRepository;

    public ReviewBoardService(
            ReviewBoardRepository reviewBoardRepository,
            ReviewBoardMemberRepository memberRepository,
            TopicRegistrationRepository registrationRepository,
            UserRepository userRepository,
            EvaluationRepository evaluationRepository) {
        this.reviewBoardRepository = reviewBoardRepository;
        this.memberRepository = memberRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.evaluationRepository = evaluationRepository;
    }

    @PreAuthorize("hasAuthority('REVIEW_BOARD_VIEW')")
    public BoardPage page(String actorEmail) {
        return page(actorEmail, "");
    }

    @PreAuthorize("hasAuthority('REVIEW_BOARD_VIEW')")
    public BoardPage page(String actorEmail, String search) {
        UserEntity actor = findActiveActor(actorEmail);
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<ReviewBoardSummary> boards = reviewBoardRepository.findAll().stream()
                .filter(board -> canView(actor, board))
                .map(this::toSummary)
                .filter(board -> normalizedSearch.isEmpty() || matchesSearch(board, normalizedSearch))
                .sorted(Comparator.comparing(ReviewBoardSummary::scheduledAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<RegistrationOption> registrations = registrationRepository
                .findByStatusOrderBySubmittedAtDesc(TopicRegistrationStatus.APPROVED).stream()
                .filter(registration -> canManage(actor, registration))
                .filter(registration -> !reviewBoardRepository.existsByTopicRegistration_Id(registration.getId()))
                .map(ReviewBoardService::toRegistrationOption)
                .toList();
        List<LecturerOption> candidates = lecturerOptions(actor);
        return new BoardPage(boards, registrations, candidates, scopeLabel(actor));
    }

    private static boolean matchesSearch(ReviewBoardSummary board, String search) {
        return contains(board.topicTitle(), search)
                || contains(board.groupName(), search)
                || contains(board.departmentCode(), search)
                || contains(board.departmentName(), search)
                || contains(board.periodName(), search)
                || contains(board.status(), search)
                || board.members().stream().anyMatch(member -> contains(member.fullName(), search)
                        || contains(member.email(), search)
                        || contains(member.role(), search));
    }

    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    @PreAuthorize("hasAuthority('REVIEW_BOARD_VIEW')")
    public ReviewBoardSummary get(String actorEmail, Long boardId) {
        UserEntity actor = findActiveActor(actorEmail);
        ReviewBoardEntity board = findBoard(boardId);
        if (!canView(actor, board)) {
            throw new ReviewBoardAccessException("You cannot view this review board.");
        }
        return toSummary(board);
    }

    @Transactional
    @PreAuthorize("hasAuthority('REVIEW_BOARD_MANAGE')")
    public ReviewBoardSummary save(
            String actorEmail,
            Long boardId,
            Long registrationId,
            String scheduledAt,
            String status,
            List<Long> lecturerIds,
            Long chairId,
            Long secretaryId) {
        UserEntity actor = findActiveActor(actorEmail);
        TopicRegistrationEntity registration;
        ReviewBoardEntity board;
        if (boardId == null) {
            registration = findRegistration(registrationId);
            assertCanManage(actor, registration);
            if (reviewBoardRepository.existsByTopicRegistration_Id(registrationId)) {
                throw new ReviewBoardValidationException("This approved registration already has a review board.");
            }
            board = new ReviewBoardEntity(registration, actor);
        } else {
            board = findBoard(boardId);
            if (!canManage(actor, board)) {
                throw new ReviewBoardAccessException("You cannot manage this review board.");
            }
            registration = board.getTopicRegistration();
            if (registrationId != null && !Objects.equals(registrationId, registration.getId())) {
                throw new ReviewBoardValidationException("A review board cannot be moved to another registration.");
            }
        }

        List<MemberInput> inputs = normalizeMembers(lecturerIds, chairId, secretaryId);
        Map<Long, UserEntity> candidates = validateMembers(registration, inputs);
        ReviewBoardStatus targetStatus = parseStatus(status);
        if (!board.getStatus().canTransitionTo(targetStatus)) {
            throw new ReviewBoardValidationException(
                    "Invalid review board transition: " + board.getStatus() + " -> " + targetStatus + ".");
        }
        board.setScheduledAt(parseDateTime(scheduledAt));
        board.setStatus(targetStatus);
        board = reviewBoardRepository.saveAndFlush(board);
        syncMembers(board, inputs, candidates);
        ensureEvaluations(board);
        return toSummary(board);
    }

    @Transactional
    @PreAuthorize("hasAuthority('REVIEW_BOARD_MANAGE')")
    public ReviewBoardSummary changeStatus(String actorEmail, Long boardId, String status) {
        UserEntity actor = findActiveActor(actorEmail);
        ReviewBoardEntity board = findBoard(boardId);
        if (!canManage(actor, board)) {
            throw new ReviewBoardAccessException("You cannot manage this review board.");
        }
        ReviewBoardStatus target = parseStatus(status);
        if (!board.getStatus().canTransitionTo(target)) {
            throw new ReviewBoardValidationException(
                    "Invalid review board transition: " + board.getStatus() + " -> " + target + ".");
        }
        board.setStatus(target);
        return toSummary(reviewBoardRepository.saveAndFlush(board));
    }

    private void syncMembers(ReviewBoardEntity board, List<MemberInput> inputs, Map<Long, UserEntity> candidates) {
        Map<Long, ReviewBoardMemberEntity> existing = memberRepository.findByBoard_IdOrderByAssignedAtAsc(board.getId())
                .stream().collect(Collectors.toMap(member -> member.getLecturer().getId(), member -> member,
                        (first, second) -> first, LinkedHashMap::new));
        Set<Long> selected = inputs.stream().map(MemberInput::lecturerId).collect(Collectors.toCollection(LinkedHashSet::new));
        LocalDateTime now = LocalDateTime.now();
        existing.values().forEach(member -> {
            if (member.isActive() && !selected.contains(member.getLecturer().getId())) {
                member.setActive(false);
                member.setEndedAt(now);
            }
        });
        List<ReviewBoardMemberEntity> toSave = new ArrayList<>();
        for (MemberInput input : inputs) {
            ReviewBoardMemberEntity member = existing.get(input.lecturerId());
            if (member == null) {
                member = new ReviewBoardMemberEntity(board, candidates.get(input.lecturerId()));
            }
            member.setActive(true);
            member.setEndedAt(null);
            member.setMemberRole(input.role());
            toSave.add(member);
        }
        memberRepository.saveAllAndFlush(toSave);
    }

    private void ensureEvaluations(ReviewBoardEntity board) {
        List<ReviewBoardMemberEntity> members = memberRepository
                .findByBoard_IdAndActiveTrueOrderByMemberRoleAscAssignedAtAsc(board.getId());
        for (ReviewBoardMemberEntity member : members) {
            EvaluationEntity evaluation = evaluationRepository
                    .findByTopicRegistration_IdAndLecturer_IdAndBoard_Id(
                            board.getTopicRegistration().getId(), member.getLecturer().getId(), board.getId())
                    .orElseGet(() -> evaluationRepository
                            .findByTopicRegistration_IdAndLecturer_Id(
                                    board.getTopicRegistration().getId(), member.getLecturer().getId())
                            .orElseGet(() -> new EvaluationEntity(
                                    board.getTopicRegistration(), member.getLecturer())));
            evaluation.setBoard(board);
            evaluation.setBoardMember(member);
            evaluationRepository.save(evaluation);
        }
        evaluationRepository.flush();
    }

    private Map<Long, UserEntity> validateMembers(TopicRegistrationEntity registration, List<MemberInput> inputs) {
        if (inputs.size() < MIN_MEMBERS || inputs.size() > MAX_MEMBERS) {
            throw new ReviewBoardValidationException("A review board must have between 3 and 5 unique lecturers.");
        }
        long chairCount = inputs.stream().filter(input -> input.role() == ReviewBoardMemberRole.CHAIR).count();
        long secretaryCount = inputs.stream().filter(input -> input.role() == ReviewBoardMemberRole.SECRETARY).count();
        if (chairCount != 1 || secretaryCount != 1) {
            throw new ReviewBoardValidationException("A review board must have exactly one Chair and one Secretary.");
        }
        List<Long> ids = inputs.stream().map(MemberInput::lecturerId).toList();
        List<UserEntity> users = userRepository.findActiveLecturerCapabilitiesByIdIn(ids);
        Map<Long, UserEntity> candidates = users.stream().collect(Collectors.toMap(UserEntity::getId, user -> user));
        if (candidates.size() != ids.size()) {
            throw new ReviewBoardValidationException("Every board member must be an active Lecturer or Faculty Head.");
        }
        Long departmentId = registration.getTopic().getDepartment() == null
                ? null : registration.getTopic().getDepartment().getId();
        if (departmentId == null || users.stream().anyMatch(user -> user.getDepartment() == null
                || !departmentId.equals(user.getDepartment().getId()))) {
            throw new ReviewBoardValidationException("Every board member must belong to the topic's department.");
        }
        if (registration.getTopic().getSupervisors().stream().anyMatch(supervisor -> ids.contains(supervisor.getId()))) {
            throw new ReviewBoardValidationException("A topic supervisor cannot sit on that topic's review board.");
        }
        return candidates;
    }

    private List<MemberInput> normalizeMembers(List<Long> lecturerIds, Long chairId, Long secretaryId) {
        if (lecturerIds == null) {
            throw new ReviewBoardValidationException("Select board members.");
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long id : lecturerIds) {
            if (id == null || id <= 0 || !unique.add(id)) {
                throw new ReviewBoardValidationException("Board members must be unique valid lecturer ids.");
            }
        }
        if (chairId == null || secretaryId == null || Objects.equals(chairId, secretaryId)
                || !unique.contains(chairId) || !unique.contains(secretaryId)) {
            throw new ReviewBoardValidationException("Chair and Secretary must be two different selected members.");
        }
        return unique.stream().map(id -> new MemberInput(id,
                Objects.equals(id, chairId) ? ReviewBoardMemberRole.CHAIR
                        : Objects.equals(id, secretaryId) ? ReviewBoardMemberRole.SECRETARY
                        : ReviewBoardMemberRole.MEMBER)).toList();
    }

    private ReviewBoardStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return ReviewBoardStatus.DRAFT;
        }
        try {
            return ReviewBoardStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ReviewBoardValidationException("Review board status is invalid: " + value + ".");
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), FORM_DATE_TIME);
        } catch (DateTimeParseException exception) {
            throw new ReviewBoardValidationException("Schedule must use a valid date and time.");
        }
    }

    private UserEntity findActiveActor(String email) {
        return userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .filter(user -> hasRole(user, "ADMIN") || hasRole(user, "FACULTY_HEAD") || hasRole(user, "LECTURER"))
                .orElseThrow(() -> new ReviewBoardAccessException("The review board account is unavailable."));
    }

    private ReviewBoardEntity findBoard(Long boardId) {
        if (boardId == null || boardId <= 0) {
            throw new ReviewBoardValidationException("Board id is invalid.");
        }
        return reviewBoardRepository.findById(boardId)
                .orElseThrow(() -> new ReviewBoardNotFoundException(boardId));
    }

    private TopicRegistrationEntity findRegistration(Long registrationId) {
        if (registrationId == null || registrationId <= 0) {
            throw new ReviewBoardValidationException("Approved registration is required.");
        }
        TopicRegistrationEntity registration = registrationRepository.findByIdForReview(registrationId)
                .orElseThrow(() -> new ReviewBoardNotFoundException(registrationId));
        if (registration.getStatus() != TopicRegistrationStatus.APPROVED) {
            throw new ReviewBoardValidationException("A review board can only be created for an approved registration.");
        }
        return registration;
    }

    private boolean canView(UserEntity actor, ReviewBoardEntity board) {
        if (hasRole(actor, "ADMIN")) {
            return true;
        }
        if (hasRole(actor, "FACULTY_HEAD")) {
            return board.getTopicRegistration().getTopic().getDepartment() != null
                    && actor.getDepartment() != null
                    && Objects.equals(actor.getDepartment().getId(),
                            board.getTopicRegistration().getTopic().getDepartment().getId());
        }
        return memberRepository.existsByBoard_IdAndLecturer_IdAndActiveTrue(board.getId(), actor.getId());
    }

    private boolean canManage(UserEntity actor, ReviewBoardEntity board) {
        return canManage(actor, board.getTopicRegistration());
    }

    private boolean canManage(UserEntity actor, TopicRegistrationEntity registration) {
        if (hasRole(actor, "ADMIN")) {
            return true;
        }
        return hasRole(actor, "FACULTY_HEAD") && actor.getDepartment() != null
                && registration.getTopic().getDepartment() != null
                && Objects.equals(actor.getDepartment().getId(), registration.getTopic().getDepartment().getId());
    }

    private void assertCanManage(UserEntity actor, TopicRegistrationEntity registration) {
        if (!canManage(actor, registration)) {
            throw new ReviewBoardAccessException("Faculty Heads can only manage boards for their department.");
        }
    }

    private List<LecturerOption> lecturerOptions(UserEntity actor) {
        List<UserEntity> users = hasRole(actor, "ADMIN")
                ? userRepository.findActiveLecturerCapabilitiesOrderByFullName()
                : actor.getDepartment() == null ? List.of()
                        : userRepository.findActiveLecturerCapabilitiesByDepartmentIdOrderByFullName(
                                actor.getDepartment().getId());
        return users.stream().map(ReviewBoardService::toLecturerOption).toList();
    }

    private ReviewBoardSummary toSummary(ReviewBoardEntity board) {
        List<MemberSummary> members = memberRepository
                .findByBoard_IdOrderByAssignedAtAsc(board.getId()).stream()
                .map(member -> new MemberSummary(member.getId(), member.getLecturer().getId(),
                        member.getLecturer().getFullName(), member.getLecturer().getEmailOrCode(),
                        member.getMemberRole().name(), member.isActive(), member.getEndedAt()))
                .toList();
        TopicRegistrationEntity registration = board.getTopicRegistration();
        return new ReviewBoardSummary(board.getId(), registration.getId(), registration.getStudentGroup().getName(),
                registration.getTopic().getId(), registration.getTopic().getTitle(),
                registration.getTopic().getDepartment().getCode(), registration.getTopic().getDepartment().getName(),
                registration.getRegistrationPeriod().getName(), board.getStatus().name(), board.getScheduledAt(), members);
    }

    private static RegistrationOption toRegistrationOption(TopicRegistrationEntity registration) {
        return new RegistrationOption(registration.getId(), registration.getStudentGroup().getName(),
                registration.getTopic().getTitle(), registration.getTopic().getDepartment().getCode(),
                registration.getRegistrationPeriod().getName());
    }

    private static LecturerOption toLecturerOption(UserEntity user) {
        return new LecturerOption(user.getId(), user.getFullName(), user.getEmailOrCode(),
                user.getDepartment() == null ? null : user.getDepartment().getCode());
    }

    private static boolean hasRole(UserEntity user, String roleCode) {
        return user.getUserRoles().stream().filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole).anyMatch(role -> role != null && role.isActive()
                        && roleCode.equalsIgnoreCase(role.getCode()));
    }

    private static String scopeLabel(UserEntity actor) {
        if (hasRole(actor, "ADMIN")) {
            return "Admin · all departments";
        }
        DepartmentEntity department = actor.getDepartment();
        return department == null ? "Faculty · no department assigned"
                : actor.getFullName() + " · " + department.getCode() + " · " + department.getName();
    }

    private record MemberInput(Long lecturerId, ReviewBoardMemberRole role) {
    }

    public record BoardPage(List<ReviewBoardSummary> boards, List<RegistrationOption> registrations,
                            List<LecturerOption> lecturerOptions, String scopeLabel) {
        public BoardPage {
            boards = List.copyOf(boards);
            registrations = List.copyOf(registrations);
            lecturerOptions = List.copyOf(lecturerOptions);
        }
    }

    public record ReviewBoardSummary(Long id, Long registrationId, String groupName, Long topicId,
                                     String topicTitle, String departmentCode, String departmentName,
                                     String periodName, String status, LocalDateTime scheduledAt,
                                     List<MemberSummary> members) {
        public ReviewBoardSummary {
            members = List.copyOf(members);
        }
    }

    public record MemberSummary(Long id, Long lecturerId, String fullName, String email,
                                 String role, boolean active, LocalDateTime endedAt) {
    }

    public record RegistrationOption(Long id, String groupName, String topicTitle,
                                     String departmentCode, String periodName) {
    }

    public record LecturerOption(Long id, String fullName, String email, String departmentCode) {
    }

    public static class ReviewBoardNotFoundException extends RuntimeException {
        public ReviewBoardNotFoundException(Long id) { super("Review board or registration not found: " + id); }
    }

    public static class ReviewBoardValidationException extends RuntimeException {
        public ReviewBoardValidationException(String message) { super(message); }
    }

    public static class ReviewBoardAccessException extends RuntimeException {
        public ReviewBoardAccessException(String message) { super(message); }
    }
}
