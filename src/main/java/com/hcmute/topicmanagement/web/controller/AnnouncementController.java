package com.hcmute.topicmanagement.web.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.AnnouncementScope;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.service.AnnouncementService;
import com.hcmute.topicmanagement.service.AnnouncementService.AnnouncementAccessException;
import com.hcmute.topicmanagement.service.AnnouncementService.AnnouncementNotFoundException;
import com.hcmute.topicmanagement.service.AnnouncementService.AnnouncementSummary;
import com.hcmute.topicmanagement.service.AnnouncementService.AnnouncementValidationException;

@Controller
@RequestMapping("/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public AnnouncementController(
            AnnouncementService announcementService,
            DepartmentRepository departmentRepository,
            UserRepository userRepository) {
        this.announcementService = announcementService;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String list(Authentication authentication, Model model) {
        model.addAttribute("pageTitle", "Announcements");
        model.addAttribute("announcements", announcementService.listPublished(authentication.getName()));
        return "announcements/list";
    }

    @GetMapping("/manage")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public String manage(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "updated") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Model model) {
        populateManagementModel(authentication, model, page, size, search, sort, direction);
        return "announcements/manage";
    }

    @PostMapping("/manage")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public String create(
            Authentication authentication,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam AnnouncementScope scope,
            @RequestParam(required = false) Long departmentId,
            RedirectAttributes redirectAttributes) {
        try {
            AnnouncementSummary created = announcementService.create(
                    authentication.getName(), title, content, scope, departmentId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Announcement " + created.getTitle() + " saved as draft.");
        } catch (AnnouncementAccessException exception) {
            return "redirect:/forbidden";
        } catch (AnnouncementValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/announcements/manage";
    }

    @PostMapping("/manage/update")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public String update(
            Authentication authentication,
            @RequestParam Long announcementId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam AnnouncementScope scope,
            @RequestParam(required = false) Long departmentId,
            RedirectAttributes redirectAttributes) {
        try {
            AnnouncementSummary updated = announcementService.update(
                    announcementId, authentication.getName(), title, content, scope, departmentId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Announcement " + updated.getTitle() + " updated.");
        } catch (AnnouncementAccessException exception) {
            return "redirect:/forbidden";
        } catch (AnnouncementNotFoundException | AnnouncementValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/announcements/manage";
    }

    @PostMapping("/manage/publish")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public String publish(
            Authentication authentication,
            @RequestParam Long announcementId,
            RedirectAttributes redirectAttributes) {
        try {
            AnnouncementSummary published = announcementService.publish(
                    authentication.getName(), announcementId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Announcement " + published.getTitle() + " published.");
        } catch (AnnouncementAccessException exception) {
            return "redirect:/forbidden";
        } catch (AnnouncementNotFoundException | AnnouncementValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/announcements/manage";
    }

    @PostMapping("/manage/hide")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public String hide(
            Authentication authentication,
            @RequestParam Long announcementId,
            RedirectAttributes redirectAttributes) {
        try {
            AnnouncementSummary hidden = announcementService.hide(
                    authentication.getName(), announcementId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Announcement " + hidden.getTitle() + " hidden.");
        } catch (AnnouncementAccessException exception) {
            return "redirect:/forbidden";
        } catch (AnnouncementNotFoundException | AnnouncementValidationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/announcements/manage";
    }

    private void populateManagementModel(
            Authentication authentication, Model model, int page, int size,
            String search, String sort, String direction) {
        AnnouncementService.AnnouncementManagementPage announcementPage =
                announcementService.listForManagementPage(
                        authentication.getName(), search, page, size, sort, direction);
        model.addAttribute("pageTitle", "Manage announcements");
        model.addAttribute("announcementPage", announcementPage);
        model.addAttribute("announcements", announcementPage.getAnnouncements());
        model.addAttribute("announcementSearch", announcementPage.getSearch());
        model.addAttribute("announcementSort", announcementPage.getSort());
        model.addAttribute("announcementDirection", announcementPage.getDirection());
        model.addAttribute("departments", departmentsFor(authentication.getName()));
        boolean canManageSchoolWide = isAdmin(authentication.getName());
        model.addAttribute("canManageSchoolWide", canManageSchoolWide);
        model.addAttribute(
                "scopes", canManageSchoolWide
                        ? Arrays.asList(AnnouncementScope.values())
                        : List.of(AnnouncementScope.DEPARTMENT));
    }

    private List<DepartmentEntity> departmentsFor(String email) {
        UserEntity manager = userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email)
                .orElse(null);
        if (manager == null) {
            return List.of();
        }
        if (isAdmin(manager)) {
            return departmentRepository.findByActiveTrueOrderByNameAsc();
        }
        return manager.getDepartment() == null ? List.of() : List.of(manager.getDepartment());
    }

    private boolean isAdmin(String email) {
        return isAdmin(userRepository.findByEmailIgnoreCaseWithRolesAndDepartment(email).orElse(null));
    }

    private boolean isAdmin(UserEntity user) {
        return user != null && user.getUserRoles().stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRole)
                .anyMatch(role -> role != null && role.isActive() && "ADMIN".equalsIgnoreCase(role.getCode()));
    }
}
