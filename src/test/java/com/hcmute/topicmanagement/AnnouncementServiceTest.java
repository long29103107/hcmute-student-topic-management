package com.hcmute.topicmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.AnnouncementScope;
import com.hcmute.topicmanagement.repository.AnnouncementRepository;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.repository.UserRoleRepository;
import com.hcmute.topicmanagement.service.AnnouncementService;
import com.hcmute.topicmanagement.service.AnnouncementService.AnnouncementAccessException;
import com.hcmute.topicmanagement.service.AnnouncementService.AnnouncementSummary;
import com.hcmute.topicmanagement.service.AnnouncementService.AnnouncementValidationException;

@SpringBootTest
@Transactional
class AnnouncementServiceTest {

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    @WithMockUser(authorities = "ANNOUNCEMENT_MANAGE")
    void createsUpdatesPublishesHidesAndRepublishesAnnouncement() {
        UserEntity admin = saveUser("ADMIN", null);

        AnnouncementSummary draft = announcementService.create(
                admin.getEmailOrCode(), "  Registration notice  ", "  Content for students.  ",
                AnnouncementScope.SCHOOL, null);

        assertThat(draft.getStatusCode()).isEqualTo("DRAFT");
        assertThat(draft.getTitle()).isEqualTo("Registration notice");
        assertThat(draft.getContent()).isEqualTo("Content for students.");
        assertThat(draft.getPublishedAt()).isNull();

        AnnouncementSummary updated = announcementService.update(
                draft.getId(), admin.getEmailOrCode(), "Updated notice", "Updated content",
                AnnouncementScope.SCHOOL, null);
        assertThat(updated.getStatusCode()).isEqualTo("DRAFT");
        assertThat(updated.getTitle()).isEqualTo("Updated notice");

        assertThatThrownBy(() -> announcementService.hide(admin.getEmailOrCode(), draft.getId()))
                .isInstanceOf(AnnouncementValidationException.class);

        AnnouncementSummary published = announcementService.publish(admin.getEmailOrCode(), draft.getId());
        assertThat(published.getStatusCode()).isEqualTo("PUBLISHED");
        LocalDateTime publishedAt = published.getPublishedAt();
        assertThat(publishedAt).isNotNull();

        assertThatThrownBy(() -> announcementService.publish(admin.getEmailOrCode(), draft.getId()))
                .isInstanceOf(AnnouncementValidationException.class);

        AnnouncementSummary hidden = announcementService.hide(admin.getEmailOrCode(), draft.getId());
        assertThat(hidden.getStatusCode()).isEqualTo("HIDDEN");
        assertThat(hidden.getPublishedAt()).isEqualTo(publishedAt);

        AnnouncementSummary republished = announcementService.publish(admin.getEmailOrCode(), draft.getId());
        assertThat(republished.getStatusCode()).isEqualTo("PUBLISHED");
        assertThat(republished.getPublishedAt()).isNotNull();
        assertThat(announcementRepository.findById(draft.getId()).orElseThrow().getStatus().name())
                .isEqualTo("PUBLISHED");
    }

    @Test
    @WithMockUser(authorities = "ANNOUNCEMENT_MANAGE")
    void facultyHeadCanOnlyManageDepartmentAnnouncementsInOwnDepartment() {
        DepartmentEntity it = saveDepartment("IT");
        DepartmentEntity business = saveDepartment("BUS");
        UserEntity facultyHead = saveUser("FACULTY_HEAD", it);

        AnnouncementSummary departmentAnnouncement = announcementService.create(
                facultyHead.getEmailOrCode(), "Department notice", "For IT students.",
                AnnouncementScope.DEPARTMENT, it.getId());
        assertThat(departmentAnnouncement.getDepartmentId()).isEqualTo(it.getId());

        assertThatThrownBy(() -> announcementService.create(
                facultyHead.getEmailOrCode(), "School notice", "Not allowed.",
                AnnouncementScope.SCHOOL, null))
                .isInstanceOf(AnnouncementAccessException.class);
        assertThatThrownBy(() -> announcementService.create(
                facultyHead.getEmailOrCode(), "Other department notice", "Not allowed.",
                AnnouncementScope.DEPARTMENT, business.getId()))
                .isInstanceOf(AnnouncementAccessException.class);

        assertThat(announcementService.listForManagement(facultyHead.getEmailOrCode()))
                .extracting(AnnouncementSummary::getId)
                .containsExactly(departmentAnnouncement.getId());
    }

    @Test
    @WithMockUser(authorities = "ANNOUNCEMENT_MANAGE")
    void managementPageSearchesSortsAndPaginatesWithinManagerScope() {
        UserEntity admin = saveUser("ADMIN", null);
        for (int index = 0; index < 12; index++) {
            announcementService.create(
                    admin.getEmailOrCode(), String.format("Alpha notice %02d", index),
                    "Searchable announcement content.", AnnouncementScope.SCHOOL, null);
        }
        announcementService.create(
                admin.getEmailOrCode(), "Other notice", "Different announcement content.",
                AnnouncementScope.SCHOOL, null);

        AnnouncementService.AnnouncementManagementPage firstPage =
                announcementService.listForManagementPage(
                        admin.getEmailOrCode(), "alpha", 0, 5, "title", "asc");

        assertThat(firstPage.getTotalItems()).isEqualTo(12);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(firstPage.getAnnouncements())
                .extracting(AnnouncementSummary::getTitle)
                .containsExactly(
                        "Alpha notice 00", "Alpha notice 01", "Alpha notice 02",
                        "Alpha notice 03", "Alpha notice 04");

        AnnouncementService.AnnouncementManagementPage lastPage =
                announcementService.listForManagementPage(
                        admin.getEmailOrCode(), "alpha", 2, 5, "title", "asc");
        assertThat(lastPage.getAnnouncements())
                .extracting(AnnouncementSummary::getTitle)
                .containsExactly("Alpha notice 10", "Alpha notice 11");
    }

    @Test
    @WithMockUser(authorities = "ANNOUNCEMENT_MANAGE")
    void publishedQueryReturnsSchoolAndMatchingDepartmentAnnouncementsOnly() {
        DepartmentEntity it = saveDepartment("IT");
        DepartmentEntity business = saveDepartment("BUS");
        UserEntity admin = saveUser("ADMIN", null);
        UserEntity itStudent = saveUser("STUDENT", it);
        UserEntity businessStudent = saveUser("STUDENT", business);
        UserEntity departmentlessStudent = saveUser("STUDENT", null);

        AnnouncementSummary school = createAndPublish(
                admin, "School announcement", AnnouncementScope.SCHOOL, null);
        AnnouncementSummary itAnnouncement = createAndPublish(
                admin, "IT announcement", AnnouncementScope.DEPARTMENT, it.getId());
        AnnouncementSummary businessAnnouncement = createAndPublish(
                admin, "Business announcement", AnnouncementScope.DEPARTMENT, business.getId());
        AnnouncementSummary draft = announcementService.create(
                admin.getEmailOrCode(), "Draft announcement", "Not visible yet.",
                AnnouncementScope.SCHOOL, null);

        assertThat(titles(announcementService.listPublished(itStudent.getEmailOrCode())))
                .containsExactlyInAnyOrder("School announcement", "IT announcement");
        assertThat(titles(announcementService.listPublished(businessStudent.getEmailOrCode())))
                .containsExactlyInAnyOrder("School announcement", "Business announcement");
        assertThat(titles(announcementService.listPublished(departmentlessStudent.getEmailOrCode())))
                .containsExactly("School announcement");
        assertThat(titles(announcementService.listPublished(admin.getEmailOrCode())))
                .containsExactlyInAnyOrder("School announcement", "IT announcement", "Business announcement");

        announcementService.hide(admin.getEmailOrCode(), itAnnouncement.getId());
        assertThat(titles(announcementService.listPublished(itStudent.getEmailOrCode())))
                .containsExactly("School announcement");
        assertThat(announcementService.listForManagement(admin.getEmailOrCode()))
                .extracting(AnnouncementSummary::getId)
                .contains(draft.getId(), school.getId(), itAnnouncement.getId(), businessAnnouncement.getId());
    }

    @Test
    @WithMockUser
    void authenticatedUserWithoutManagementAuthorityCannotCreateAnnouncement() {
        assertThatThrownBy(() -> announcementService.create(
                "missing@example.test", "Title", "Content", AnnouncementScope.SCHOOL, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = "ANNOUNCEMENT_MANAGE")
    void validatesRequiredFieldsAndDepartmentScope() {
        UserEntity admin = saveUser("ADMIN", null);

        assertThatThrownBy(() -> announcementService.create(
                admin.getEmailOrCode(), " ", "Content", AnnouncementScope.SCHOOL, null))
                .isInstanceOf(AnnouncementValidationException.class);
        assertThatThrownBy(() -> announcementService.create(
                admin.getEmailOrCode(), "Title", "Content", AnnouncementScope.DEPARTMENT, null))
                .isInstanceOf(AnnouncementValidationException.class);
        assertThatThrownBy(() -> announcementService.create(
                admin.getEmailOrCode(), "Title", "Content", AnnouncementScope.SCHOOL, saveDepartment("IT").getId()))
                .isInstanceOf(AnnouncementValidationException.class);
    }

    private AnnouncementSummary createAndPublish(
            UserEntity author, String title, AnnouncementScope scope, Long departmentId) {
        AnnouncementSummary draft = announcementService.create(
                author.getEmailOrCode(), title, "Announcement content.", scope, departmentId);
        return announcementService.publish(author.getEmailOrCode(), draft.getId());
    }

    private DepartmentEntity saveDepartment(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return departmentRepository.saveAndFlush(new DepartmentEntity(
                prefix + "-" + suffix, "Announcement Department " + suffix));
    }

    private UserEntity saveUser(String roleCode, DepartmentEntity department) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        UserEntity user = new UserEntity(
                roleCode.toLowerCase() + "-" + suffix,
                "Announcement " + roleCode,
                "test-password-hash");
        user.setEmailOrCode(roleCode.toLowerCase() + "-" + suffix + "@example.test");
        user.setDepartment(department);
        user = userRepository.saveAndFlush(user);
        RoleEntity role = roleRepository.findByCode(roleCode)
                .orElseGet(() -> roleRepository.saveAndFlush(new RoleEntity(roleCode, roleCode, roleCode)));
        UserRoleEntity assignment = new UserRoleEntity(user, role);
        user.getUserRoles().add(assignment);
        userRoleRepository.saveAndFlush(assignment);
        return user;
    }

    private static List<String> titles(List<AnnouncementSummary> announcements) {
        return announcements.stream().map(AnnouncementSummary::getTitle).toList();
    }
}
