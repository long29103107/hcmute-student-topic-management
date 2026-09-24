package com.hcmute.topicmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.hcmute.topicmanagement.model.EvaluationEntity;
import com.hcmute.topicmanagement.model.ReviewBoardEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.repository.EvaluationRepository;
import com.hcmute.topicmanagement.repository.ReviewBoardRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.security.DatabaseUserPrincipal;
import com.hcmute.topicmanagement.service.DatabaseSeedService;


@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:reviewboardtest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class ReviewBoardControllerTest {

    private static final String CNTT_HEAD =
            "nguyen.van.khang@lecturer.hcmute.edu.vn";

    private static final String KHMT_HEAD =
            "tran.thi.hong.gam@lecturer.hcmute.edu.vn";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatabaseSeedService databaseSeedService;

    @Autowired
    private ReviewBoardRepository reviewBoardRepository;

    @Autowired
    private TopicRegistrationRepository registrationRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void resetSeed() {
        databaseSeedService.resetAndSeed();
    }

    @Test
    void facultyHeadSeesOnlyBoardsInTheirDepartmentAndTheBoardPageRenders()
            throws Exception {

        mockMvc.perform(
                        get("/faculty/boards")
                                .with(user(facultyHeadPrincipal(
                                        CNTT_HEAD,
                                        "REVIEW_BOARD_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/boards"))
                .andExpect(content().string(
                        containsString(
                                "Nền tảng quản lý đề tài và tiến độ khóa luận")))
                .andExpect(content().string(
                        containsString("PUBLISHED")))
                .andExpect(content().string(
                        containsString("Nguyễn Thanh Bình")))
                .andExpect(content().string(
                        not(containsString(
                                "Ứng dụng quản lý quy trình thực tập doanh nghiệp"))));

        mockMvc.perform(
                        get("/api/faculty/boards")
                                .with(user(facultyHeadPrincipal(
                                        CNTT_HEAD,
                                        "REVIEW_BOARD_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boards.length()").value(1))
                .andExpect(jsonPath("$.boards[0].departmentCode")
                        .value("CNTT"))
                .andExpect(jsonPath("$.boards[0].members.length()")
                        .value(3));
    }

    @Test
    void createBoardFormRendersInsideACenteredWidthLimitedModal()
            throws Exception {

        mockMvc.perform(
                        get("/faculty/boards")
                                .with(user(facultyHeadPrincipal(
                                        CNTT_HEAD,
                                        "REVIEW_BOARD_VIEW",
                                        "REVIEW_BOARD_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString(
                                "data-modal-target=\"create-board-modal\"")))
                .andExpect(content().string(
                        containsString(
                                "id=\"create-board-modal\"")))
                .andExpect(content().string(
                        containsString(
                                "class=\"relative mx-auto w-full max-w-4xl\"")))
                .andExpect(content().string(
                        containsString(
                                "name=\"lecturerIds\"")))
                .andExpect(content().string(
                        containsString(
                                "type=\"checkbox\"")))
                .andExpect(content().string(
                        containsString("max-h-72")))
                .andExpect(content().string(
                        not(containsString(
                                "multiple required size=\"6\""))));
    }

    @Test
    void reviewBoardPageRendersDepartmentStatusSortingAndPaginationControls()
            throws Exception {

        mockMvc.perform(
                        get("/faculty/boards")
                                .with(user(facultyHeadPrincipal(
                                        CNTT_HEAD,
                                        "REVIEW_BOARD_VIEW",
                                        "REVIEW_BOARD_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(view().name("faculty/boards"))
                .andExpect(content().string(
                        containsString("Department")))
                .andExpect(content().string(
                        containsString("Status")))
                .andExpect(content().string(
                        containsString("Group")))
                .andExpect(content().string(
                        containsString("Topic")))
                .andExpect(content().string(
                        containsString("Scheduled")))
                .andExpect(content().string(
                        containsString("All departments")))
                .andExpect(content().string(
                        containsString("All statuses")))
                .andExpect(content().string(
                        containsString("Search boards")))
                .andExpect(content().string(
                        containsString("Previous")))
                .andExpect(content().string(
                        containsString("Next")));
    }

    @Test
    void reviewBoardActionsUseThreeDotMenuAndConfirmation()
            throws Exception {

        mockMvc.perform(
                        get("/faculty/boards")
                                .with(user(facultyHeadPrincipal(
                                        CNTT_HEAD,
                                        "REVIEW_BOARD_VIEW",
                                        "REVIEW_BOARD_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("Board actions")))
                .andExpect(content().string(
                        containsString("Edit board")))
                .andExpect(content().string(
                        containsString("data-dropdown-toggle")))
                .andExpect(content().string(
                        containsString("data-dropdown-placement=\"bottom-end\"")));
    }

    @Test
    void departmentFilterWorksForApi() throws Exception {

        ReviewBoardEntity board = reviewBoardRepository
                .findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        Long departmentId = board.getTopicRegistration()
                .getTopic()
                .getDepartment()
                .getId();

        mockMvc.perform(
                        get("/api/faculty/boards")
                                .param(
                                        "departmentId",
                                        departmentId.toString())
                                .with(user(facultyHeadPrincipal(
                                        CNTT_HEAD,
                                        "REVIEW_BOARD_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId")
                        .value(departmentId))
                .andExpect(jsonPath("$.totalItems")
                        .isNumber())
                .andExpect(jsonPath("$.boards").isArray());

        mockMvc.perform(
                        get("/faculty/boards")
                                .param(
                                        "departmentId",
                                        departmentId.toString())
                                .with(user(facultyHeadPrincipal(
                                        CNTT_HEAD,
                                        "REVIEW_BOARD_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("Department")));
    }

    @Test
    void statusFilterWorksForApi() throws Exception {

        ReviewBoardEntity board = reviewBoardRepository
                .findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        String boardStatus = board.getStatus().name();

        mockMvc.perform(
                        get("/api/faculty/boards")
                                .param("status", boardStatus)
                                .with(user(facultyHeadPrincipal(
                                        CNTT_HEAD,
                                        "REVIEW_BOARD_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value(boardStatus))
                .andExpect(jsonPath("$.boards").isArray());
    }

    @Test
    void searchFilterWorksForApi() throws Exception {

        mockMvc.perform(
                        get("/api/faculty/boards")
                                .param("search", "Phoenix")
                                .with(user(facultyHeadPrincipal(
                                        CNTT_HEAD,
                                        "REVIEW_BOARD_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.search").value("Phoenix"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.boards[0].groupName")
                        .value("Nhóm Phoenix"));
    }

    @Test
    void sortingAndPaginationParametersAreAppliedToApi()
            throws Exception {

        mockMvc.perform(
                        get("/api/faculty/boards")
                                .param("page", "0")
                                .param("size", "1")
                                .param("sort", "topic")
                                .param("direction", "asc")
                                .with(user(facultyHeadPrincipal(
                                        CNTT_HEAD,
                                        "REVIEW_BOARD_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.sort").value("topic"))
                .andExpect(jsonPath("$.direction").value("asc"))
                .andExpect(jsonPath("$.totalItems").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.boards").isArray());
    }

    @Test
    void filterSortAndPaginationStateIsPreservedOnFacultyPage()
            throws Exception {

        ReviewBoardEntity board = reviewBoardRepository
                .findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        Long departmentId = board.getTopicRegistration()
                .getTopic()
                .getDepartment()
                .getId();

        String boardStatus = board.getStatus().name();

        mockMvc.perform(
                        get("/faculty/boards")
                                .param(
                                        "page",
                                        "0")
                                .param(
                                        "size",
                                        "10")
                                .param(
                                        "departmentId",
                                        departmentId.toString())
                                .param(
                                        "status",
                                        boardStatus)
                                .param(
                                        "sort",
                                        "topic")
                                .param(
                                        "direction",
                                        "desc")
                                .with(user(facultyHeadPrincipal(
                                        CNTT_HEAD,
                                        "REVIEW_BOARD_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString(
                                "name=\"departmentId\"")))
                .andExpect(content().string(
                        containsString(
                                "name=\"status\"")))
                .andExpect(content().string(
                        containsString(
                                "name=\"sort\"")))
                .andExpect(content().string(
                        containsString(
                                "name=\"direction\"")))
                .andExpect(content().string(
                        containsString(
                                "direction")));
    }

    @Test
    void boardCreationRejectsInvalidCompositionAndCreatesEvaluationsForValidMembers()
            throws Exception {

        TopicRegistrationEntity registration =
                registration("Nhóm Nova");

        List<Long> members = registrationRepository
                .findByStatusForReview(
                        com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus.APPROVED)
                .stream()
                .filter(item ->
                        item.getId().equals(
                                registration.getId()))
                .findFirst()
                .map(item -> List.of(
                        lecturerId("nguyen.quoc.viet"),
                        lecturerId("ly.minh.kiet")))
                .orElseThrow();

        mockMvc.perform(
                        post("/api/faculty/boards")
                                .with(user(facultyHeadPrincipal(
                                        KHMT_HEAD,
                                        "REVIEW_BOARD_MANAGE")))
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"registrationId\":"
                                                + registration.getId()
                                                + ",\"status\":\"DRAFT\""
                                                + ",\"lecturerIds\":["
                                                + members.get(0)
                                                + ","
                                                + members.get(1)
                                                + "],\"chairId\":"
                                                + members.get(0)
                                                + ",\"secretaryId\":"
                                                + members.get(1)
                                                + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("REVIEW_BOARD_INVALID"));

        mockMvc.perform(
                        post("/api/faculty/boards")
                                .with(user(facultyHeadPrincipal(
                                        KHMT_HEAD,
                                        "REVIEW_BOARD_MANAGE")))
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"registrationId\":"
                                                + registration.getId()
                                                + ",\"status\":\"DRAFT\""
                                                + ",\"lecturerIds\":["
                                                + lecturerId("nguyen.quoc.viet")
                                                + ","
                                                + lecturerId("ly.minh.kiet")
                                                + ","
                                                + lecturerId("truong.gia.huy")
                                                + "],\"chairId\":"
                                                + lecturerId("nguyen.quoc.viet")
                                                + ",\"secretaryId\":"
                                                + lecturerId("ly.minh.kiet")
                                                + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("DRAFT"))
                .andExpect(jsonPath("$.members.length()")
                        .value(3));

        ReviewBoardEntity board =
                reviewBoardRepository
                        .findByTopicRegistration_Id(
                                registration.getId())
                        .orElseThrow();

        List<EvaluationEntity> evaluations =
                evaluationRepository
                        .findByTopicRegistration_IdOrderByCreatedAtAsc(
                                registration.getId());

        Assertions.assertThat(evaluations)
                .hasSize(3)
                .allSatisfy(evaluation ->
                        Assertions.assertThat(
                                        evaluation.getBoard().getId())
                                .isEqualTo(board.getId()));
    }

    @Test
    void facultyHeadCannotManageAnotherDepartmentBoard()
            throws Exception {

        ReviewBoardEntity cnpmBoard =
                reviewBoardRepository
                        .findByTopicRegistration_Id(
                                registration("Nhóm Atlas").getId())
                        .orElseThrow();

        mockMvc.perform(
                        post(
                                "/api/faculty/boards/{id}/status",
                                cnpmBoard.getId())
                                .with(user(facultyHeadPrincipal(
                                        CNTT_HEAD,
                                        "REVIEW_BOARD_MANAGE")))
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("REVIEW_BOARD_FORBIDDEN"));
    }

    private TopicRegistrationEntity registration(
            String groupName) {

        return registrationRepository
                .findByStatusForReview(
                        com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus.APPROVED)
                .stream()
                .filter(item ->
                        groupName.equals(
                                item.getStudentGroup().getName()))
                .findFirst()
                .orElseThrow();
    }

    private Long lecturerId(String login) {

        return userRepository
                .findByLoginIdentifier(
                        login + "@lecturer.hcmute.edu.vn")
                .orElseThrow()
                .getId();
    }

    private static DatabaseUserPrincipal facultyHeadPrincipal(
            String email,
            String... permissions) {

        List<SimpleGrantedAuthority> authorities =
                new ArrayList<>();

        authorities.add(
                new SimpleGrantedAuthority(
                        "ROLE_FACULTY_HEAD"));

        for (String permission : permissions) {

            authorities.add(
                    new SimpleGrantedAuthority(
                            permission));
        }

        return new DatabaseUserPrincipal(
                email,
                "",
                "Test faculty head",
                "ROLE_FACULTY_HEAD",
                authorities);
    }
}
