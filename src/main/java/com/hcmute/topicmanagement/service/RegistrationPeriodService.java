package com.hcmute.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class RegistrationPeriodService {

    private static final int MAX_NAME_LENGTH = 200;
    public static final String PERIOD_NOT_FOUND = "REGISTRATION_PERIOD_NOT_FOUND";
    public static final String PERIOD_NOT_OPEN = "REGISTRATION_PERIOD_NOT_OPEN";
    public static final String LECTURER_WINDOW_CLOSED = "LECTURER_REGISTRATION_WINDOW_CLOSED";
    public static final String STUDENT_WINDOW_CLOSED = "STUDENT_REGISTRATION_WINDOW_CLOSED";

    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final UserRepository userRepository;

    public RegistrationPeriodService(
            RegistrationPeriodRepository registrationPeriodRepository,
            UserRepository userRepository) {
        this.registrationPeriodRepository = registrationPeriodRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAuthority('PERIOD_MANAGE')")
    public List<PeriodSummary> listPeriods() {
        return registrationPeriodRepository.findAllByOrderByLecturerRegistrationStartDesc().stream()
                .map(RegistrationPeriodService::toSummary)
                .toList();
    }

    @PreAuthorize("hasAuthority('PERIOD_MANAGE')")
    public PeriodPage listPeriodsPage(String search, int page, int size, String sort, String direction) {
        String normalizedSearch = normalizeSearch(search);
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        int safeSize = Math.min(Math.max(size, 5), 100);
        List<PeriodSummary> filtered = listPeriods().stream()
                .filter(period -> matchesSearch(period, normalizedSearch))
                .sorted(periodComparator(normalizedSort, normalizedDirection))
                .toList();
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        return new PeriodPage(
                filtered.subList(from, to), safePage, safeSize, totalItems, totalPages,
                normalizedSearch, normalizedSort, normalizedDirection);
    }

    /** Read-only contract for topic and topic-registration modules. */
    public PeriodAccess inspect(Long id, LocalDateTime now) {
        RegistrationPeriodEntity period = findPeriod(id);
        return toAccess(period, normalizeNow(now));
    }

    /** Returns open periods whose lecturer window contains the supplied instant. */
    public List<PeriodSummary> listOpenForLecturer(LocalDateTime now) {
        return registrationPeriodRepository.findOpenForLecturer(
                        RegistrationPeriodStatus.OPEN, normalizeNow(now)).stream()
                .map(RegistrationPeriodService::toSummary)
                .toList();
    }

    /** Returns open periods whose student window contains the supplied instant. */
    public List<PeriodSummary> listOpenForStudent(LocalDateTime now) {
        return registrationPeriodRepository.findOpenForStudent(
                        RegistrationPeriodStatus.OPEN, normalizeNow(now)).stream()
                .map(RegistrationPeriodService::toSummary)
                .toList();
    }

    /**
     * Read-only handoff for a lecturer topic module. Window boundaries are
     * inclusive and failures expose a stable domain error code.
     */
    public RegistrationPeriodEntity requireOpenForLecturer(Long id, LocalDateTime now) {
        RegistrationPeriodEntity period = findPeriod(id);
        PeriodAccess access = toAccess(period, normalizeNow(now));
        if (!access.isStatusOpen()) {
            throw new RegistrationPeriodAccessException(
                    PERIOD_NOT_OPEN, "The registration period is not open.");
        }
        if (!access.isLecturerRegistrationOpen()) {
            throw new RegistrationPeriodAccessException(
                    LECTURER_WINDOW_CLOSED, "The lecturer registration window is closed.");
        }
        return period;
    }

    /** Read-only handoff for a student topic-registration module. */
    public RegistrationPeriodEntity requireOpenForStudent(Long id, LocalDateTime now) {
        RegistrationPeriodEntity period = findPeriod(id);
        PeriodAccess access = toAccess(period, normalizeNow(now));
        if (!access.isStatusOpen()) {
            throw new RegistrationPeriodAccessException(
                    PERIOD_NOT_OPEN, "The registration period is not open.");
        }
        if (!access.isStudentRegistrationOpen()) {
            throw new RegistrationPeriodAccessException(
                    STUDENT_WINDOW_CLOSED, "The student registration window is closed.");
        }
        return period;
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERIOD_MANAGE')")
    public PeriodSummary create(
            String creatorIdentifier,
            String name,
            PeriodType type,
            LocalDateTime lecturerRegistrationStart,
            LocalDateTime lecturerRegistrationEnd,
            LocalDateTime studentRegistrationStart,
            LocalDateTime studentRegistrationEnd,
            LocalDateTime reviewerScoreDeadline,
            LocalDateTime councilReportDate,
            RegistrationPeriodStatus status) {
        PeriodInput input = validateAndNormalize(
                name, type, lecturerRegistrationStart, lecturerRegistrationEnd,
                studentRegistrationStart, studentRegistrationEnd, reviewerScoreDeadline,
                councilReportDate, status);
        RegistrationPeriodEntity period = new RegistrationPeriodEntity(
                input.name(), input.type(), input.lecturerRegistrationStart(), input.lecturerRegistrationEnd(),
                input.studentRegistrationStart(), input.studentRegistrationEnd());
        apply(period, input);
        period.setCreatedBy(findCreator(creatorIdentifier));
        return toSummary(registrationPeriodRepository.saveAndFlush(period));
    }

    @Transactional
    @PreAuthorize("hasAuthority('PERIOD_MANAGE')")
    public PeriodSummary update(
            Long id,
            String name,
            PeriodType type,
            LocalDateTime lecturerRegistrationStart,
            LocalDateTime lecturerRegistrationEnd,
            LocalDateTime studentRegistrationStart,
            LocalDateTime studentRegistrationEnd,
            LocalDateTime reviewerScoreDeadline,
            LocalDateTime councilReportDate,
            RegistrationPeriodStatus status) {
        RegistrationPeriodEntity period = findPeriod(id);
        PeriodInput input = validateAndNormalize(
                name, type, lecturerRegistrationStart, lecturerRegistrationEnd,
                studentRegistrationStart, studentRegistrationEnd, reviewerScoreDeadline,
                councilReportDate, status);
        apply(period, input);
        return toSummary(registrationPeriodRepository.saveAndFlush(period));
    }

    private UserEntity findCreator(String identifier) {
        return userRepository.findByLoginIdentifierOrEmailOrCodeIgnoreCase(identifier)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new RegistrationPeriodValidationException(
                        "The account creating this period is not available."));
    }

    private RegistrationPeriodEntity findPeriod(Long id) {
        if (id == null) {
            throw new RegistrationPeriodNotFoundException(null);
        }
        return registrationPeriodRepository.findById(id)
                .orElseThrow(() -> new RegistrationPeriodNotFoundException(id));
    }

    private static void apply(RegistrationPeriodEntity period, PeriodInput input) {
        if (!period.canTransitionTo(input.status())) {
            throw new RegistrationPeriodValidationException(
                    "Invalid registration period status transition: "
                            + period.getStatus() + " -> " + input.status() + ".");
        }
        period.setName(input.name());
        period.setType(input.type());
        period.setLecturerRegistrationStart(input.lecturerRegistrationStart());
        period.setLecturerRegistrationEnd(input.lecturerRegistrationEnd());
        period.setStudentRegistrationStart(input.studentRegistrationStart());
        period.setStudentRegistrationEnd(input.studentRegistrationEnd());
        period.setReviewerScoreDeadline(input.reviewerScoreDeadline());
        period.setCouncilReportDate(input.councilReportDate());
        period.setStatus(input.status());
    }

    private static PeriodInput validateAndNormalize(
            String name,
            PeriodType type,
            LocalDateTime lecturerRegistrationStart,
            LocalDateTime lecturerRegistrationEnd,
            LocalDateTime studentRegistrationStart,
            LocalDateTime studentRegistrationEnd,
            LocalDateTime reviewerScoreDeadline,
            LocalDateTime councilReportDate,
            RegistrationPeriodStatus status) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isBlank()) {
            throw new RegistrationPeriodValidationException("Registration period name is required.");
        }
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new RegistrationPeriodValidationException(
                    "Registration period name must be at most 200 characters.");
        }
        if (type == null) {
            throw new RegistrationPeriodValidationException("Select a period type.");
        }
        requireDate(lecturerRegistrationStart, "Lecturer registration start is required.");
        requireDate(lecturerRegistrationEnd, "Lecturer registration end is required.");
        requireDate(studentRegistrationStart, "Student registration start is required.");
        requireDate(studentRegistrationEnd, "Student registration end is required.");
        requireOrdered(lecturerRegistrationStart, lecturerRegistrationEnd,
                "Lecturer registration start must be before or equal to the end.");
        requireOrdered(studentRegistrationStart, studentRegistrationEnd,
                "Student registration start must be before or equal to the end.");
        if (reviewerScoreDeadline != null && type != PeriodType.GRADUATION_THESIS) {
            throw new RegistrationPeriodValidationException(
                    "Reviewer score deadline only applies to graduation thesis periods.");
        }
        if (councilReportDate != null && type != PeriodType.GRADUATION_THESIS) {
            throw new RegistrationPeriodValidationException(
                    "Council report date only applies to graduation thesis periods.");
        }
        return new PeriodInput(
                normalizedName, type, lecturerRegistrationStart, lecturerRegistrationEnd,
                studentRegistrationStart, studentRegistrationEnd, reviewerScoreDeadline,
                councilReportDate, status == null ? RegistrationPeriodStatus.DRAFT : status);
    }

    private static void requireDate(LocalDateTime value, String message) {
        if (value == null) {
            throw new RegistrationPeriodValidationException(message);
        }
    }

    private static void requireOrdered(LocalDateTime start, LocalDateTime end, String message) {
        if (start.isAfter(end)) {
            throw new RegistrationPeriodValidationException(message);
        }
    }

    private static LocalDateTime normalizeNow(LocalDateTime now) {
        return now == null ? LocalDateTime.now() : now;
    }

    private static PeriodAccess toAccess(RegistrationPeriodEntity period, LocalDateTime now) {
        boolean statusOpen = period.getStatus() == RegistrationPeriodStatus.OPEN;
        return new PeriodAccess(
                period.getId(),
                period.getStatus(),
                period.getLecturerRegistrationStart(),
                period.getLecturerRegistrationEnd(),
                period.getStudentRegistrationStart(),
                period.getStudentRegistrationEnd(),
                statusOpen,
                statusOpen && isWithin(now, period.getLecturerRegistrationStart(), period.getLecturerRegistrationEnd()),
                statusOpen && isWithin(now, period.getStudentRegistrationStart(), period.getStudentRegistrationEnd()));
    }

    private static boolean isWithin(LocalDateTime now, LocalDateTime start, LocalDateTime end) {
        return start != null && end != null
                && !now.isBefore(start)
                && !now.isAfter(end);
    }

    private static PeriodSummary toSummary(RegistrationPeriodEntity period) {
        return new PeriodSummary(
                period.getId(), period.getName(), period.getType(), period.getStatus(),
                period.getLecturerRegistrationStart(), period.getLecturerRegistrationEnd(),
                period.getStudentRegistrationStart(), period.getStudentRegistrationEnd(),
                period.getReviewerScoreDeadline(), period.getCouncilReportDate(),
                period.getCreatedBy() == null ? null : period.getCreatedBy().getFullName());
    }

    private static boolean matchesSearch(PeriodSummary period, String search) {
        if (search.isBlank()) {
            return true;
        }
        return containsIgnoreCase(period.getName(), search)
                || containsIgnoreCase(period.getType() == null ? null : period.getType().name(), search)
                || containsIgnoreCase(period.getStatus() == null ? null : period.getStatus().name(), search)
                || containsIgnoreCase(period.getCreatedByName(), search);
    }

    private static boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private static Comparator<PeriodSummary> periodComparator(String sort, String direction) {
        Comparator<PeriodSummary> comparator = switch (sort) {
            case "lecturer" -> Comparator.comparing(
                    PeriodSummary::getLecturerRegistrationStart,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(PeriodSummary::getName, String.CASE_INSENSITIVE_ORDER);
            case "student" -> Comparator.comparing(
                    PeriodSummary::getStudentRegistrationStart,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(PeriodSummary::getName, String.CASE_INSENSITIVE_ORDER);
            case "status" -> Comparator.comparing(
                    RegistrationPeriodService::statusName,
                    String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(PeriodSummary::getName, String.CASE_INSENSITIVE_ORDER);
            case "creator" -> Comparator.comparing(
                    PeriodSummary::getCreatedByName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(PeriodSummary::getName, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(
                    PeriodSummary::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(PeriodSummary::getLecturerRegistrationStart,
                            Comparator.nullsLast(Comparator.naturalOrder()));
        };
        if ("desc".equals(direction)) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private static String statusName(PeriodSummary period) {
        return period.getStatus() == null ? "" : period.getStatus().name();
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeSort(String sort) {
        return switch (sort == null ? "" : sort.trim().toLowerCase(Locale.ROOT)) {
            case "lecturer", "student", "status", "creator" -> sort.trim().toLowerCase(Locale.ROOT);
            default -> "period";
        };
    }

    private static String normalizeDirection(String direction) {
        return "desc".equalsIgnoreCase(direction == null ? "" : direction.trim()) ? "desc" : "asc";
    }

    public static final class PeriodSummary {
        private final Long id;
        private final String name;
        private final PeriodType type;
        private final RegistrationPeriodStatus status;
        private final LocalDateTime lecturerRegistrationStart;
        private final LocalDateTime lecturerRegistrationEnd;
        private final LocalDateTime studentRegistrationStart;
        private final LocalDateTime studentRegistrationEnd;
        private final LocalDateTime reviewerScoreDeadline;
        private final LocalDateTime councilReportDate;
        private final String createdByName;

        public PeriodSummary(
                Long id,
                String name,
                PeriodType type,
                RegistrationPeriodStatus status,
                LocalDateTime lecturerRegistrationStart,
                LocalDateTime lecturerRegistrationEnd,
                LocalDateTime studentRegistrationStart,
                LocalDateTime studentRegistrationEnd,
                LocalDateTime reviewerScoreDeadline,
                LocalDateTime councilReportDate,
                String createdByName) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.status = status;
            this.lecturerRegistrationStart = lecturerRegistrationStart;
            this.lecturerRegistrationEnd = lecturerRegistrationEnd;
            this.studentRegistrationStart = studentRegistrationStart;
            this.studentRegistrationEnd = studentRegistrationEnd;
            this.reviewerScoreDeadline = reviewerScoreDeadline;
            this.councilReportDate = councilReportDate;
            this.createdByName = createdByName;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public PeriodType getType() { return type; }
        public RegistrationPeriodStatus getStatus() { return status; }
        public LocalDateTime getLecturerRegistrationStart() { return lecturerRegistrationStart; }
        public LocalDateTime getLecturerRegistrationEnd() { return lecturerRegistrationEnd; }
        public LocalDateTime getStudentRegistrationStart() { return studentRegistrationStart; }
        public LocalDateTime getStudentRegistrationEnd() { return studentRegistrationEnd; }
        public LocalDateTime getReviewerScoreDeadline() { return reviewerScoreDeadline; }
        public LocalDateTime getCouncilReportDate() { return councilReportDate; }
        public String getCreatedByName() { return createdByName; }
    }

    public static final class PeriodPage {
        private final List<PeriodSummary> periods;
        private final int page;
        private final int size;
        private final int totalItems;
        private final int totalPages;
        private final String search;
        private final String sort;
        private final String direction;

        public PeriodPage(
                List<PeriodSummary> periods, int page, int size, int totalItems, int totalPages,
                String search, String sort, String direction) {
            this.periods = List.copyOf(periods);
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
            this.search = search;
            this.sort = sort;
            this.direction = direction;
        }

        public List<PeriodSummary> getPeriods() { return periods; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public String getSearch() { return search; }
        public String getSort() { return sort; }
        public String getDirection() { return direction; }
        public boolean isHasPrevious() { return page > 0; }
        public boolean isHasNext() { return page + 1 < totalPages; }
    }

    /** Stable read-only period/timeline contract for downstream modules. */
    public static final class PeriodAccess {
        private final Long id;
        private final RegistrationPeriodStatus status;
        private final LocalDateTime lecturerRegistrationStart;
        private final LocalDateTime lecturerRegistrationEnd;
        private final LocalDateTime studentRegistrationStart;
        private final LocalDateTime studentRegistrationEnd;
        private final boolean statusOpen;
        private final boolean lecturerRegistrationOpen;
        private final boolean studentRegistrationOpen;

        public PeriodAccess(
                Long id,
                RegistrationPeriodStatus status,
                LocalDateTime lecturerRegistrationStart,
                LocalDateTime lecturerRegistrationEnd,
                LocalDateTime studentRegistrationStart,
                LocalDateTime studentRegistrationEnd,
                boolean statusOpen,
                boolean lecturerRegistrationOpen,
                boolean studentRegistrationOpen) {
            this.id = id;
            this.status = status;
            this.lecturerRegistrationStart = lecturerRegistrationStart;
            this.lecturerRegistrationEnd = lecturerRegistrationEnd;
            this.studentRegistrationStart = studentRegistrationStart;
            this.studentRegistrationEnd = studentRegistrationEnd;
            this.statusOpen = statusOpen;
            this.lecturerRegistrationOpen = lecturerRegistrationOpen;
            this.studentRegistrationOpen = studentRegistrationOpen;
        }

        public Long getId() { return id; }
        public RegistrationPeriodStatus getStatus() { return status; }
        public LocalDateTime getLecturerRegistrationStart() { return lecturerRegistrationStart; }
        public LocalDateTime getLecturerRegistrationEnd() { return lecturerRegistrationEnd; }
        public LocalDateTime getStudentRegistrationStart() { return studentRegistrationStart; }
        public LocalDateTime getStudentRegistrationEnd() { return studentRegistrationEnd; }
        public boolean isStatusOpen() { return statusOpen; }
        public boolean isLecturerRegistrationOpen() { return lecturerRegistrationOpen; }
        public boolean isStudentRegistrationOpen() { return studentRegistrationOpen; }
    }

    private record PeriodInput(
            String name,
            PeriodType type,
            LocalDateTime lecturerRegistrationStart,
            LocalDateTime lecturerRegistrationEnd,
            LocalDateTime studentRegistrationStart,
            LocalDateTime studentRegistrationEnd,
            LocalDateTime reviewerScoreDeadline,
            LocalDateTime councilReportDate,
            RegistrationPeriodStatus status) {
    }

    public static class RegistrationPeriodNotFoundException extends RuntimeException {
        public RegistrationPeriodNotFoundException(Long id) {
            super(id == null ? "Registration period id is required." : "Registration period not found: " + id);
        }

        public String getCode() {
            return PERIOD_NOT_FOUND;
        }
    }

    public static class RegistrationPeriodAccessException extends RuntimeException {
        private final String code;

        public RegistrationPeriodAccessException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public static class RegistrationPeriodValidationException extends RuntimeException {
        public RegistrationPeriodValidationException(String message) {
            super(message);
        }
    }
}
