package com.hcmute.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.AnnouncementEntity;
import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.AnnouncementScope;
import com.hcmute.topicmanagement.model.enums.AnnouncementStatus;
import com.hcmute.topicmanagement.repository.AnnouncementRepository;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class AnnouncementService {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_CONTENT_LENGTH = 20_000;

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public AnnouncementService(
            AnnouncementRepository announcementRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    /** Lists management-visible announcements for an Admin or Faculty Head. */
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public List<AnnouncementSummary> listForManagement(String managerEmail) {
        UserEntity manager = findActiveManager(managerEmail);
        return managementAnnouncements(manager).stream()
                .map(AnnouncementService::toSummary)
                .toList();
    }

    /** Returns a searchable, sortable, filterable and paginated management queue. */
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public AnnouncementManagementPage listForManagementPage(
            String managerEmail, String search, String status, Long departmentId,
            int page, int size, String sort, String direction) {
        UserEntity manager = findActiveManager(managerEmail);
        String normalizedSearch = normalizeSearch(search);
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        String normalizedStatus = (status == null || status.isBlank()) ? "" : status.trim().toUpperCase();
        List<AnnouncementSummary> filtered = managementAnnouncements(manager).stream()
                .filter(announcement -> isInManagementScope(manager, announcement))
                .map(AnnouncementService::toSummary)
                .filter(announcement -> matchesSearch(announcement, normalizedSearch))
                .filter(announcement -> normalizedStatus.isBlank()
                        || normalizedStatus.equalsIgnoreCase(announcement.getStatusCode()))
                .filter(announcement -> departmentId == null
                        || departmentId.equals(announcement.getDepartmentId()))
                .sorted(announcementComparator(normalizedSort, normalizedDirection))
                .toList();
        int safeSize = Math.min(Math.max(size, 5), 50);
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);
        int from = Math.min(safePage * safeSize, totalItems);
        int to = Math.min(from + safeSize, totalItems);
        return new AnnouncementManagementPage(
                filtered.subList(from, to), safePage, safeSize, totalItems, totalPages,
                search == null ? "" : search.trim(), normalizedSort, normalizedDirection,
                normalizedStatus, departmentId);
    }

    private List<AnnouncementEntity> managementAnnouncements(UserEntity manager) {
        return announcementRepository.findAllWithDetailsOrderByUpdatedAtDesc().stream()
                .filter(announcement -> isInManagementScope(manager, announcement))
                .toList();
    }

    /** Creates a new announcement in DRAFT state. */
    @Transactional
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public AnnouncementSummary create(
            String managerEmail,
            String title,
            String content,
            AnnouncementScope scope,
            Long departmentId) {
        UserEntity manager = findActiveManager(managerEmail);
        AnnouncementInput input = validateInput(title, content);
        DepartmentEntity department = resolveDepartment(manager, scope, departmentId);

        AnnouncementEntity announcement = new AnnouncementEntity(
                input.title(), input.content(), scope, department, manager);
        announcement.setStatus(AnnouncementStatus.DRAFT);
        return toSummary(announcementRepository.saveAndFlush(announcement));
    }

    /** Updates announcement content and scope without changing its lifecycle state. */
    @Transactional
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public AnnouncementSummary update(
            Long announcementId,
            String managerEmail,
            String title,
            String content,
            AnnouncementScope scope,
            Long departmentId) {
        UserEntity manager = findActiveManager(managerEmail);
        AnnouncementEntity announcement = findAnnouncement(announcementId);
        assertCanManage(manager, announcement);
        AnnouncementInput input = validateInput(title, content);
        DepartmentEntity department = resolveDepartment(manager, scope, departmentId);

        announcement.setTitle(input.title());
        announcement.setContent(input.content());
        announcement.setScope(scope);
        announcement.setDepartment(department);
        return toSummary(announcementRepository.saveAndFlush(announcement));
    }

    /** Publishes a draft or previously hidden announcement. */
    @Transactional
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public AnnouncementSummary publish(String managerEmail, Long announcementId) {
        UserEntity manager = findActiveManager(managerEmail);
        AnnouncementEntity announcement = findAnnouncement(announcementId);
        assertCanManage(manager, announcement);
        if (announcement.getStatus() != AnnouncementStatus.DRAFT
                && announcement.getStatus() != AnnouncementStatus.HIDDEN) {
            throw new AnnouncementValidationException(
                    "Only draft or hidden announcements can be published.");
        }

        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setPublishedAt(LocalDateTime.now());
        return toSummary(announcementRepository.saveAndFlush(announcement));
    }

    /** Hides a currently published announcement while retaining its publication audit time. */
    @Transactional
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public AnnouncementSummary hide(String managerEmail, Long announcementId) {
        UserEntity manager = findActiveManager(managerEmail);
        AnnouncementEntity announcement = findAnnouncement(announcementId);
        assertCanManage(manager, announcement);
        if (announcement.getStatus() != AnnouncementStatus.PUBLISHED) {
            throw new AnnouncementValidationException(
                    "Only published announcements can be hidden.");
        }

        announcement.setStatus(AnnouncementStatus.HIDDEN);
        return toSummary(announcementRepository.saveAndFlush(announcement));
    }

    /** Returns published announcements visible to the authenticated user's scope. */
    @PreAuthorize("isAuthenticated()")
    public List<AnnouncementSummary> listPublished(String viewerEmail) {
        UserEntity viewer = findActiveViewer(viewerEmail);
        List<AnnouncementEntity> announcements = isAdmin(viewer)
                ? announcementRepository.findPublishedForAdmin(AnnouncementStatus.PUBLISHED)
                : announcementRepository.findPublishedForDepartment(
                        AnnouncementStatus.PUBLISHED,
                        AnnouncementScope.SCHOOL,
                        AnnouncementScope.DEPARTMENT,
                        viewer.getDepartment() == null ? null : viewer.getDepartment().getId());
        return announcements.stream().map(AnnouncementService::toSummary).toList();
    }

    private AnnouncementEntity findAnnouncement(Long announcementId) {
        if (announcementId == null) {
            throw new AnnouncementNotFoundException(null);
        }
        return announcementRepository.findByIdWithDetails(announcementId)
                .orElseThrow(() -> new AnnouncementNotFoundException(announcementId));
    }

    private UserEntity findActiveManager(String email) {
        UserEntity manager = userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new AnnouncementAccessException(
                        "Only an active Admin or Faculty Head can manage announcements."));
        if (!isAdmin(manager) && !isFacultyHead(manager)) {
            throw new AnnouncementAccessException(
                    "Only an active Admin or Faculty Head can manage announcements.");
        }
        return manager;
    }

    private UserEntity findActiveViewer(String email) {
        return userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new AnnouncementAccessException("Viewer account is not available."));
    }

    private DepartmentEntity resolveDepartment(UserEntity manager, AnnouncementScope scope, Long departmentId) {
        if (scope == null) {
            throw new AnnouncementValidationException("Announcement scope is required.");
        }
        if (scope == AnnouncementScope.SCHOOL) {
            if (!isAdmin(manager)) {
                throw new AnnouncementAccessException(
                        "Faculty Heads can only manage announcements in their department.");
            }
            if (departmentId != null) {
                throw new AnnouncementValidationException(
                        "School-wide announcements cannot have a department.");
            }
            return null;
        }

        if (departmentId == null) {
            throw new AnnouncementValidationException(
                    "A department is required for a department-scoped announcement.");
        }
        DepartmentEntity department = departmentRepository.findById(departmentId)
                .filter(DepartmentEntity::isActive)
                .orElseThrow(() -> new AnnouncementValidationException(
                        "The selected department is not active or does not exist."));
        if (!isAdmin(manager) && !sameDepartment(manager, department)) {
            throw new AnnouncementAccessException(
                    "Faculty Heads can only manage announcements in their department.");
        }
        return department;
    }

    private static void assertCanManage(UserEntity manager, AnnouncementEntity announcement) {
        if (isAdmin(manager)) {
            return;
        }
        if (!isFacultyHead(manager) || announcement.getScope() != AnnouncementScope.DEPARTMENT
                || announcement.getDepartment() == null || !sameDepartment(manager, announcement.getDepartment())) {
            throw new AnnouncementAccessException(
                    "Faculty Heads can only manage announcements in their department.");
        }
    }

    private static boolean isInManagementScope(UserEntity manager, AnnouncementEntity announcement) {
        return isAdmin(manager) || (announcement.getScope() == AnnouncementScope.DEPARTMENT
                && announcement.getDepartment() != null
                && sameDepartment(manager, announcement.getDepartment()));
    }

    private static boolean sameDepartment(UserEntity user, DepartmentEntity department) {
        return user.getDepartment() != null && user.getDepartment().getId() != null
                && department != null && user.getDepartment().getId().equals(department.getId());
    }

    private static boolean isAdmin(UserEntity user) {
        return hasActiveRole(user, "ADMIN");
    }

    private static boolean isFacultyHead(UserEntity user) {
        return hasActiveRole(user, "FACULTY_HEAD");
    }

    private static boolean hasActiveRole(UserEntity user, String roleCode) {
        return user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .filter(role -> role != null && role.isActive())
                .anyMatch(role -> roleCode.equalsIgnoreCase(role.getCode()));
    }

    private static boolean matchesSearch(AnnouncementSummary announcement, String search) {
        return search.isBlank()
                || containsIgnoreCase(announcement.getTitle(), search)
                || containsIgnoreCase(announcement.getContent(), search)
                || containsIgnoreCase(announcement.getStatusCode(), search)
                || containsIgnoreCase(announcement.getScopeCode(), search)
                || containsIgnoreCase(announcement.getDepartmentCode(), search)
                || containsIgnoreCase(announcement.getDepartmentName(), search)
                || containsIgnoreCase(announcement.getAuthorName(), search)
                || containsIgnoreCase(announcement.getAuthorEmail(), search);
    }

    private static boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private static Comparator<AnnouncementSummary> announcementComparator(String sort, String direction) {
        Comparator<String> text = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
        Comparator<AnnouncementSummary> comparator = switch (sort) {
            case "status" -> Comparator.comparing(AnnouncementSummary::getStatusCode, text)
                    .thenComparing(AnnouncementSummary::getTitle, text);
            case "scope" -> Comparator.comparing(AnnouncementSummary::getScopeCode, text)
                    .thenComparing(AnnouncementSummary::getTitle, text);
            case "department" -> Comparator.comparing(AnnouncementSummary::getDepartmentCode, text)
                    .thenComparing(AnnouncementSummary::getDepartmentName, text)
                    .thenComparing(AnnouncementSummary::getTitle, text);
            case "author" -> Comparator.comparing(AnnouncementSummary::getAuthorName, text)
                    .thenComparing(AnnouncementSummary::getTitle, text);
            case "updated" -> Comparator.comparing(
                    AnnouncementSummary::getUpdatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(AnnouncementSummary::getTitle, text);
            default -> Comparator.comparing(AnnouncementSummary::getTitle, text)
                    .thenComparing(AnnouncementSummary::getUpdatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder()));
        };
        return "desc".equals(direction) ? comparator.reversed() : comparator;
    }

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeSort(String sort) {
        return Set.of("title", "status", "scope", "department", "author", "updated")
                .contains(sort) ? sort : "updated";
    }

    private static String normalizeDirection(String direction) {
        return "asc".equalsIgnoreCase(direction) ? "asc" : "desc";
    }

    private static AnnouncementInput validateInput(String title, String content) {
        String normalizedTitle = title == null ? "" : title.trim();
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedTitle.isBlank()) {
            throw new AnnouncementValidationException("Announcement title is required.");
        }
        if (normalizedTitle.length() > MAX_TITLE_LENGTH) {
            throw new AnnouncementValidationException("Announcement title must be at most 255 characters.");
        }
        if (normalizedContent.isBlank()) {
            throw new AnnouncementValidationException("Announcement content is required.");
        }
        if (normalizedContent.length() > MAX_CONTENT_LENGTH) {
            throw new AnnouncementValidationException(
                    "Announcement content must be at most 20000 characters.");
        }
        return new AnnouncementInput(normalizedTitle, normalizedContent);
    }

    private static AnnouncementSummary toSummary(AnnouncementEntity announcement) {
        DepartmentEntity department = announcement.getDepartment();
        UserEntity author = announcement.getAuthor();
        return new AnnouncementSummary(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getScope().name(),
                announcement.getStatus().name(),
                department == null ? null : department.getId(),
                department == null ? null : department.getCode(),
                department == null ? null : department.getName(),
                author.getId(),
                author.getFullName(),
                author.getEmailOrCode(),
                announcement.getPublishedAt(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt());
    }

    private record AnnouncementInput(String title, String content) {
    }

    public static final class AnnouncementSummary {
        private final Long id;
        private final String title;
        private final String content;
        private final String scopeCode;
        private final String statusCode;
        private final Long departmentId;
        private final String departmentCode;
        private final String departmentName;
        private final Long authorId;
        private final String authorName;
        private final String authorEmail;
        private final LocalDateTime publishedAt;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;

        public AnnouncementSummary(
                Long id, String title, String content, String scopeCode, String statusCode,
                Long departmentId, String departmentCode, String departmentName, Long authorId,
                String authorName, String authorEmail, LocalDateTime publishedAt,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.scopeCode = scopeCode;
            this.statusCode = statusCode;
            this.departmentId = departmentId;
            this.departmentCode = departmentCode;
            this.departmentName = departmentName;
            this.authorId = authorId;
            this.authorName = authorName;
            this.authorEmail = authorEmail;
            this.publishedAt = publishedAt;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getScopeCode() { return scopeCode; }
        public String getStatusCode() { return statusCode; }
        public Long getDepartmentId() { return departmentId; }
        public String getDepartmentCode() { return departmentCode; }
        public String getDepartmentName() { return departmentName; }
        public Long getAuthorId() { return authorId; }
        public String getAuthorName() { return authorName; }
        public String getAuthorEmail() { return authorEmail; }
        public LocalDateTime getPublishedAt() { return publishedAt; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
    }

    public static final class AnnouncementManagementPage {
        private final List<AnnouncementSummary> announcements;
        private final int page;
        private final int size;
        private final int totalItems;
        private final int totalPages;
        private final String search;
        private final String sort;
        private final String direction;
        private final String status;
        private final Long departmentId;

        public AnnouncementManagementPage(
                List<AnnouncementSummary> announcements, int page, int size, int totalItems,
                int totalPages, String search, String sort, String direction,
                String status, Long departmentId) {
            this.announcements = List.copyOf(announcements);
            this.page = page;
            this.size = size;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
            this.search = search;
            this.sort = sort;
            this.direction = direction;
            this.status = status == null ? "" : status;
            this.departmentId = departmentId;
        }

        public List<AnnouncementSummary> getAnnouncements() { return announcements; }
        public int getPage() { return page; }
        public int getSize() { return size; }
        public int getTotalItems() { return totalItems; }
        public int getTotalPages() { return totalPages; }
        public String getSearch() { return search; }
        public String getSort() { return sort; }
        public String getDirection() { return direction; }
        public String getStatus() { return status; }
        public Long getDepartmentId() { return departmentId; }
        public boolean isHasPrevious() { return page > 0; }
        public boolean isHasNext() { return page + 1 < totalPages; }
    }

    public static class AnnouncementNotFoundException extends RuntimeException {
        public AnnouncementNotFoundException(Long id) {
            super(id == null ? "Announcement id is required." : "Announcement not found: " + id);
        }
    }

    public static class AnnouncementValidationException extends RuntimeException {
        public AnnouncementValidationException(String message) {
            super(message);
        }
    }

    public static class AnnouncementAccessException extends RuntimeException {
        public AnnouncementAccessException(String message) {
            super(message);
        }
    }
}
