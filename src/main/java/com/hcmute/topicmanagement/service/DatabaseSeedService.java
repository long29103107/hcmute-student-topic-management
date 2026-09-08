package com.hcmute.topicmanagement.service;

import java.sql.SQLException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.EvaluationEntity;
import com.hcmute.topicmanagement.model.RegistrationResultEntity;
import com.hcmute.topicmanagement.model.ReviewBoardEntity;
import com.hcmute.topicmanagement.model.ReviewBoardMemberEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.AnnouncementEntity;
import com.hcmute.topicmanagement.model.PermissionEntity;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.RoleEntity;
import com.hcmute.topicmanagement.model.RolePermissionEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.model.UserRoleEntity;
import com.hcmute.topicmanagement.model.enums.GroupStatus;
import com.hcmute.topicmanagement.model.enums.EvaluationStatus;
import com.hcmute.topicmanagement.model.enums.AnnouncementScope;
import com.hcmute.topicmanagement.model.enums.AnnouncementStatus;
import com.hcmute.topicmanagement.model.enums.PeriodType;
import com.hcmute.topicmanagement.model.enums.RegistrationPeriodStatus;
import com.hcmute.topicmanagement.model.enums.RegistrationResultStatus;
import com.hcmute.topicmanagement.model.enums.ReviewBoardMemberRole;
import com.hcmute.topicmanagement.model.enums.ReviewBoardStatus;
import com.hcmute.topicmanagement.model.enums.TopicRegistrationStatus;
import com.hcmute.topicmanagement.model.enums.TopicStatus;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.AnnouncementRepository;
import com.hcmute.topicmanagement.repository.PermissionRepository;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.RolePermissionRepository;
import com.hcmute.topicmanagement.repository.EvaluationRepository;
import com.hcmute.topicmanagement.repository.RegistrationResultRepository;
import com.hcmute.topicmanagement.repository.ReviewBoardMemberRepository;
import com.hcmute.topicmanagement.repository.ReviewBoardRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.RoleRepository;
import com.hcmute.topicmanagement.repository.StudentGroupRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;
import com.hcmute.topicmanagement.repository.UserRoleRepository;

@Service
public class DatabaseSeedService {

    private static final String LOCAL_PASSWORD_HASH =
            "$2a$10$VI1jWffo.Jg/04uyrX73TufViz1kOmzLTa9trum0bK61bf9gwh5cq";
    private static final String ADMIN_PASSWORD_HASH =
            "$2a$10$Tib/thYqs.dQRhB17iTfIO7qY0KKHBywglurPCoADhi9VRnjep79i";
    private static final String STUDENT_EMAIL_DOMAIN = "@student.hcmute.edu.vn";
    private static final String LECTURER_EMAIL_DOMAIN = "@lecturer.hcmute.edu.vn";
    private static final String SEEDED_PERIOD_NAME = "Đợt đăng ký đề tài học kỳ 1 năm học 2026-2027";

    private static final List<String> TABLES = List.of(
            "announcements",
            "registration_results",
            "evaluations",
            "review_board_members",
            "review_boards",
            "reports",
            "topic_registrations",
            "group_members",
            "topic_supervisors",
            "student_groups",
            "topics",
            "registration_periods",
            "departments",
            "user_roles",
            "role_permissions",
            "users",
            "permissions",
            "roles");

    private static final List<RoleSeed> ROLES = List.of(
            new RoleSeed("ADMIN", "Administrator", "Full access to all role capabilities and system administration."),
            new RoleSeed("FACULTY_HEAD", "Faculty Head",
                    "Lecturer capabilities plus faculty workflow and registration management."),
            new RoleSeed("LECTURER", "Lecturer", "Propose topics, supervise students, and submit evaluations."),
            new RoleSeed("STUDENT", "Student", "Create groups, register topics, submit reports, and view group results."));

    private static final List<PermissionSeed> PERMISSIONS = List.of(
            new PermissionSeed("DASHBOARD_VIEW", "View dashboard", "Dashboard", "View the administration dashboard."),
            new PermissionSeed("USER_READ", "View accounts", "Users", "View the account list and account details."),
            new PermissionSeed("USER_CREATE", "Create account", "Users", "Create a new user account."),
            new PermissionSeed("USER_UPDATE", "Update account", "Users", "Update account information."),
            new PermissionSeed("USER_LOCK", "Lock/unlock account", "Users", "Lock or unlock an account."),
            new PermissionSeed("USER_DELETE", "Delete account", "Users", "Delete an account when it has no dependent records."),
            new PermissionSeed("USER_ROLE_ASSIGN", "Assign role to user", "Users", "Assign or remove a user role."),
            new PermissionSeed("ROLE_READ", "View roles", "Roles", "View the role list and current permissions."),
            new PermissionSeed("ROLE_UPDATE", "Manage role permissions", "Roles",
                    "Open the role permission assignment screen."),
            new PermissionSeed("PERMISSION_ASSIGN", "Assign permissions to role", "Roles",
                    "Enable or disable permissions for a role."),
            new PermissionSeed("DEPARTMENT_MANAGE", "Manage departments", "Departments",
                    "Create and update faculty departments."),
            new PermissionSeed("PERIOD_MANAGE", "Manage registration periods", "Registration periods",
                    "Create and manage topic registration periods."),
            new PermissionSeed("TOPIC_PROPOSE", "Propose topics", "Topics", "Create and update topic proposals."),
            new PermissionSeed("SUPERVISOR_MANAGE", "Manage topic supervisors", "Topics",
                    "Assign one or two lecturer-capability supervisors to a topic."),
            new PermissionSeed("TOPIC_REVIEW", "Review topics", "Topics",
                    "Review, approve, reject, and publish topic proposals for Admin/Faculty Head workflows."),
            new PermissionSeed("TOPIC_VIEW", "View published topics", "Topics",
                    "View topics published for registration."),
            new PermissionSeed("GROUP_MANAGE", "Manage student groups", "Student groups",
                    "Create and manage student group membership."),
            new PermissionSeed("GROUP_READ", "View student groups", "Student groups",
                    "View student groups within the user's faculty scope."),
            new PermissionSeed("GROUP_UPDATE", "Update student groups", "Student groups",
                    "Update student group name, leader, and lifecycle status within the user's faculty scope."),
            new PermissionSeed("REGISTRATION_SUBMIT", "Submit topic registrations", "Registrations",
                    "Submit a topic registration for a student group."),
            new PermissionSeed("REPORT_SUBMIT", "Submit reports", "Reports",
                    "Submit reports for an approved topic registration."),
            new PermissionSeed("REPORT_VIEW", "View/download reports", "Reports",
                    "View and download reports allowed by resource relationship."),
            new PermissionSeed("EVALUATION_SUBMIT", "Submit evaluations", "Evaluations",
                    "Submit evaluation scores and comments."),
            new PermissionSeed("REGISTRATION_REVIEW", "Review topic registrations", "Registrations",
                    "Approve or reject student topic registrations."),
            new PermissionSeed("RESULT_VIEW", "View results", "Results",
                    "View published results for permitted users."),
            new PermissionSeed("ANNOUNCEMENT_MANAGE", "Manage announcements", "Announcements",
                    "Create, update, publish, and hide announcements within the user's management scope."),
            new PermissionSeed("REVIEW_BOARD_VIEW", "View review boards", "Review boards",
                    "View review boards visible in the user's faculty scope."),
            new PermissionSeed("REVIEW_BOARD_MANAGE", "Manage review boards", "Review boards",
                    "Create boards, assign members, and move board lifecycle status."));

    private static final List<String> LECTURER_PERMISSIONS = List.of(
            "TOPIC_PROPOSE", "TOPIC_VIEW", "REPORT_VIEW", "EVALUATION_SUBMIT", "RESULT_VIEW", "REVIEW_BOARD_VIEW");

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
            "ADMIN", allPermissionCodes(),
            "FACULTY_HEAD", withLecturerPermissions(
                    "PERIOD_MANAGE", "SUPERVISOR_MANAGE", "TOPIC_REVIEW", "REGISTRATION_REVIEW",
                    "ANNOUNCEMENT_MANAGE", "REVIEW_BOARD_MANAGE", "GROUP_READ", "GROUP_UPDATE"),
            "LECTURER", LECTURER_PERMISSIONS,
            "STUDENT", List.of(
                    "TOPIC_VIEW", "GROUP_MANAGE", "REGISTRATION_SUBMIT", "REPORT_SUBMIT", "REPORT_VIEW",
                    "RESULT_VIEW"));

    private static final List<DepartmentSeed> DEPARTMENTS = List.of(
            new DepartmentSeed("CNTT", "Công nghệ thông tin"),
            new DepartmentSeed("KHMT", "Khoa học máy tính"),
            new DepartmentSeed("CNPM", "Công nghệ phần mềm"),
            new DepartmentSeed("HTTT", "Hệ thống thông tin"),
            new DepartmentSeed("KTMT", "Kỹ thuật máy tính"),
            new DepartmentSeed("MMT", "Mạng máy tính và truyền thông"),
            new DepartmentSeed("ATTT", "An toàn thông tin"),
            new DepartmentSeed("KHDL", "Khoa học dữ liệu"),
            new DepartmentSeed("AI", "Trí tuệ nhân tạo"),
            new DepartmentSeed("IOT", "Internet vạn vật"),
            new DepartmentSeed("WEB", "Công nghệ Web"),
            new DepartmentSeed("MOBILE", "Công nghệ di động"),
            new DepartmentSeed("HTN", "Hệ thống nhúng"),
            new DepartmentSeed("CDS", "Chuyển đổi số"),
            new DepartmentSeed("QLCNTT", "Quản lý công nghệ thông tin"));

    private static final List<UserSeed> FACULTY_HEADS = List.of(
            facultyHead("nguyen.van.khang", "PGS. TS. Nguyễn Văn Khang", "CNTT"),
            facultyHead("tran.thi.hong.gam", "TS. Trần Thị Hồng Gấm", "KHMT"),
            facultyHead("le.quang.huy", "TS. Lê Quang Huy", "CNPM"),
            facultyHead("pham.minh.tuan", "PGS. TS. Phạm Minh Tuấn", "HTTT"));

    private static final List<UserSeed> LECTURERS = List.of(
            lecturer("nguyen.thanh.binh", "Nguyễn Thanh Bình", "CNTT"),
            lecturer("vo.hoang.nam", "Võ Hoàng Nam", "CNTT"),
            lecturer("dang.minh.tri", "Đặng Minh Trí", "CNTT"),
            lecturer("bui.thanh.ha", "Bùi Thanh Hà", "CNTT"),
            lecturer("nguyen.quoc.viet", "Nguyễn Quốc Việt", "KHMT"),
            lecturer("doan.thi.ngoc", "Đoàn Thị Ngọc", "KHMT"),
            lecturer("truong.gia.huy", "Trương Gia Huy", "KHMT"),
            lecturer("ly.minh.kiet", "Lý Minh Kiệt", "KHMT"),
            lecturer("phan.tuan.anh", "Phan Tuấn Anh", "CNPM"),
            lecturer("huynh.thi.my.linh", "Huỳnh Thị Mỹ Linh", "CNPM"),
            lecturer("ngo.duy.khanh", "Ngô Duy Khánh", "CNPM"),
            lecturer("mai.quoc.thang", "Mai Quốc Thắng", "CNPM"),
            lecturer("hoang.duc.long", "Hoàng Đức Long", "HTTT"),
            lecturer("nguyen.thi.thu", "Nguyễn Thị Thu", "HTTT"),
            lecturer("ta.minh.quan", "Tạ Minh Quân", "HTTT"),
            lecturer("cao.ngoc.han", "Cao Ngọc Hân", "HTTT"));

    private static final List<UserSeed> STUDENTS = List.of(
            student("24110000", "Nguyễn Minh Anh", "CNTT"),
            student("24110001", "Trần Hoàng Nam", "CNTT"),
            student("24110002", "Lê Gia Hân", "CNTT"),
            student("24110003", "Phạm Đức Anh", "CNTT"),
            student("24110004", "Võ Thanh Tùng", "CNTT"),
            student("24110005", "Đặng Ngọc Mai", "CNTT"),
            student("24110006", "Bùi Quang Huy", "CNTT"),
            student("24110007", "Nguyễn Khánh Linh", "CNTT"),
            student("24110008", "Hồ Minh Khoa", "CNTT"),
            student("24110009", "Phan Thùy Dương", "CNTT"),
            student("24110010", "Huỳnh Quốc Bảo", "CNTT"),
            student("24110011", "Trương Nhật Minh", "CNTT"),
            student("24110012", "Lý Hải Yến", "CNTT"),
            student("24110013", "Nguyễn Thành Đạt", "KHMT"),
            student("24110014", "Trần Ngọc Hân", "KHMT"),
            student("24110015", "Lê Minh Khôi", "KHMT"),
            student("24110016", "Phạm Thảo Vy", "KHMT"),
            student("24110017", "Võ Gia Bảo", "KHMT"),
            student("24110018", "Đặng Tuấn Kiệt", "KHMT"),
            student("24110019", "Bùi Phương Nhi", "KHMT"),
            student("24110020", "Hồ Hoàng Long", "KHMT"),
            student("24110021", "Phan Minh Châu", "KHMT"),
            student("24110022", "Huỳnh Anh Quân", "KHMT"),
            student("24110023", "Trương Khả Hân", "KHMT"),
            student("24110024", "Lý Đức Tài", "KHMT"),
            student("24110025", "Nguyễn Bảo Ngọc", "KHMT"),
            student("24110026", "Trần Minh Quân", "CNPM"),
            student("24110027", "Lê Hoài An", "CNPM"),
            student("24110028", "Phạm Quốc Hưng", "CNPM"),
            student("24110029", "Võ Mỹ Duyên", "CNPM"),
            student("24110030", "Đặng Anh Tú", "CNPM"),
            student("24110031", "Bùi Ngọc Anh", "CNPM"),
            student("24110032", "Hồ Gia Minh", "CNPM"),
            student("24110033", "Phan Nhật Nam", "CNPM"),
            student("24110034", "Huỳnh Minh Thư", "CNPM"),
            student("24110035", "Trương Đức Minh", "CNPM"),
            student("24110036", "Lý Thanh Trúc", "CNPM"),
            student("24110037", "Nguyễn Hoàng Phúc", "CNPM"),
            student("24110038", "Trần Gia Bảo", "HTTT"),
            student("24110039", "Lê Ngọc Diệp", "HTTT"),
            student("24110040", "Phạm Minh Nhật", "HTTT"),
            student("24110041", "Võ Thanh Vân", "HTTT"),
            student("24110042", "Đặng Quốc Khải", "HTTT"),
            student("24110043", "Bùi Khánh Vy", "HTTT"),
            student("24110044", "Hồ Anh Duy", "HTTT"),
            student("24110045", "Phan Thảo Nguyên", "HTTT"),
            student("24110046", "Huỳnh Quốc Trung", "HTTT"),
            student("24110047", "Trương Minh Tâm", "HTTT"),
            student("24110048", "Lý Ngọc Huyền", "HTTT"),
            student("24110049", "Nguyễn Đức Toàn", "HTTT"));

    private static final List<TopicSeed> TOPICS = List.of(
            new TopicSeed(
                    "Nền tảng quản lý đề tài và tiến độ khóa luận",
                    "Xây dựng nền tảng theo dõi vòng đời đề tài, tiến độ thực hiện và các mốc nghiệm thu cho sinh viên.",
                    "CNTT",
                    "nguyen.thanh.binh",
                    TopicStatus.PENDING_APPROVAL,
                    List.of("nguyen.van.khang", "vo.hoang.nam")),
            new TopicSeed(
                    "Phân tích dữ liệu học tập bằng dashboard tương tác",
                    "Thiết kế dashboard giúp cố vấn nhận diện xu hướng học tập và hỗ trợ sinh viên theo dữ liệu thực tế.",
                    "CNTT",
                    "bui.thanh.ha",
                    TopicStatus.DRAFT,
                    List.of("dang.minh.tri")),
            new TopicSeed(
                    "Phát hiện bất thường trong kết quả học tập",
                    "Nghiên cứu các phương pháp phát hiện sớm kết quả bất thường trong dữ liệu điểm và lịch sử học tập.",
                    "KHMT",
                    "nguyen.quoc.viet",
                    TopicStatus.APPROVED,
                    List.of("tran.thi.hong.gam", "doan.thi.ngoc")),
            new TopicSeed(
                    "Mô hình gợi ý lộ trình học tập cá nhân hóa",
                    "Xây dựng mô hình gợi ý học phần dựa trên năng lực, mục tiêu và lịch sử đăng ký của sinh viên.",
                    "KHMT",
                    "doan.thi.ngoc",
                    TopicStatus.PENDING_APPROVAL,
                    List.of("truong.gia.huy")),
            new TopicSeed(
                    "Kiến trúc microservices cho cổng dịch vụ sinh viên",
                    "Đề xuất và triển khai thử nghiệm kiến trúc microservices cho các dịch vụ học vụ có khả năng mở rộng.",
                    "CNPM",
                    "phan.tuan.anh",
                    TopicStatus.PUBLISHED,
                    List.of("le.quang.huy", "huynh.thi.my.linh")),
            new TopicSeed(
                    "Ứng dụng quản lý quy trình thực tập doanh nghiệp",
                    "Số hóa quy trình đăng ký, phê duyệt và theo dõi thực tập giữa sinh viên, giảng viên và doanh nghiệp.",
                    "CNPM",
                    "huynh.thi.my.linh",
                    TopicStatus.APPROVED,
                    List.of("ngo.duy.khanh", "mai.quoc.thang")),
            new TopicSeed(
                    "Hệ thống cảnh báo sớm nguy cơ trễ tiến độ đề tài",
                    "Phân tích các mốc công việc và tín hiệu tiến độ để cảnh báo sớm những đề tài có nguy cơ chậm kế hoạch.",
                    "HTTT",
                    "hoang.duc.long",
                    TopicStatus.PENDING_APPROVAL,
                    List.of("pham.minh.tuan")),
            new TopicSeed(
                    "Số hóa quy trình tiếp nhận yêu cầu hỗ trợ học vụ",
                    "Thiết kế hệ thống tiếp nhận, phân loại và theo dõi yêu cầu hỗ trợ học vụ theo từng đơn vị phụ trách.",
                    "HTTT",
                    "nguyen.thi.thu",
                    TopicStatus.DRAFT,
                    List.of("nguyen.thi.thu", "ta.minh.quan")));

    private static final List<AnnouncementSeed> ANNOUNCEMENTS = List.of(
            new AnnouncementSeed(
                    "Thông báo mở đợt đăng ký đề tài học kỳ 1 năm học 2026-2027",
                    "Sinh viên kiểm tra nhóm, chọn đề tài và hoàn tất đăng ký trong thời gian mở của đợt đăng ký.",
                    AnnouncementScope.SCHOOL,
                    null,
                    "admin",
                    AnnouncementStatus.PUBLISHED),
            new AnnouncementSeed(
                    "CNTT: Lịch hướng dẫn đăng ký đề tài",
                    "Khoa CNTT tổ chức buổi hướng dẫn đăng ký đề tài và giải đáp thắc mắc cho sinh viên trong khoa.",
                    AnnouncementScope.DEPARTMENT,
                    "CNTT",
                    "nguyen.van.khang@lecturer.hcmute.edu.vn",
                    AnnouncementStatus.PUBLISHED),
            new AnnouncementSeed(
                    "CNTT: Dự thảo lịch tư vấn đề tài",
                    "Bản nháp nội bộ để Trưởng khoa rà soát trước khi phát hành cho sinh viên.",
                    AnnouncementScope.DEPARTMENT,
                    "CNTT",
                    "nguyen.van.khang@lecturer.hcmute.edu.vn",
                    AnnouncementStatus.DRAFT));

    private static final List<StudentGroupSeed> STUDENT_GROUPS = List.of(
            new StudentGroupSeed("Nhóm Phoenix", "24110000", List.of("24110001", "24110002")),
            new StudentGroupSeed("Nhóm Orion", "24110003", List.of("24110004", "24110005")),
            new StudentGroupSeed("Nhóm Nova", "24110013", List.of("24110014", "24110015")),
            new StudentGroupSeed("Nhóm Atlas", "24110026", List.of("24110027", "24110028")));

    private static final List<UserSeed> USERS = buildUsers();

    private static List<UserSeed> buildUsers() {
        List<UserSeed> users = new ArrayList<>();
        users.add(new UserSeed("admin", "System Administrator", "admin@hcmute.edu.vn", "ADMIN", null));
        users.addAll(FACULTY_HEADS);
        users.addAll(LECTURERS);
        users.addAll(STUDENTS);
        return List.copyOf(users);
    }

    private static UserSeed facultyHead(String localPart, String fullName, String departmentCode) {
        return staff(localPart, fullName, "FACULTY_HEAD", departmentCode);
    }

    private static UserSeed lecturer(String localPart, String fullName, String departmentCode) {
        return staff(localPart, fullName, "LECTURER", departmentCode);
    }

    private static UserSeed staff(String localPart, String fullName, String roleCode, String departmentCode) {
        String email = localPart + LECTURER_EMAIL_DOMAIN;
        return new UserSeed(email, fullName, email, roleCode, departmentCode);
    }

    private static UserSeed student(String studentCode, String fullName, String departmentCode) {
        return new UserSeed(
                studentCode,
                fullName,
                studentCode + STUDENT_EMAIL_DOMAIN,
                "STUDENT",
                departmentCode);
    }

    private static List<String> withLecturerPermissions(String... additionalPermissions) {
        List<String> permissions = new ArrayList<>(LECTURER_PERMISSIONS);
        permissions.addAll(List.of(additionalPermissions));
        return List.copyOf(permissions);
    }

    private static List<String> allPermissionCodes() {
        return PERMISSIONS.stream().map(PermissionSeed::code).toList();
    }

    private final DataSource dataSource;
    private final AnnouncementRepository announcementRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final TopicRegistrationRepository topicRegistrationRepository;
    private final EvaluationRepository evaluationRepository;
    private final RegistrationResultRepository registrationResultRepository;
    private final ReviewBoardRepository reviewBoardRepository;
    private final ReviewBoardMemberRepository reviewBoardMemberRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    @PersistenceContext
    private EntityManager entityManager;

    public DatabaseSeedService(
            DataSource dataSource,
            AnnouncementRepository announcementRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RegistrationPeriodRepository registrationPeriodRepository,
            RolePermissionRepository rolePermissionRepository,
            DepartmentRepository departmentRepository,
            TopicRegistrationRepository topicRegistrationRepository,
            EvaluationRepository evaluationRepository,
            RegistrationResultRepository registrationResultRepository,
            ReviewBoardRepository reviewBoardRepository,
            ReviewBoardMemberRepository reviewBoardMemberRepository,
            StudentGroupRepository studentGroupRepository,
            TopicRepository topicRepository,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository) {
        this.dataSource = dataSource;
        this.announcementRepository = announcementRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.registrationPeriodRepository = registrationPeriodRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.topicRegistrationRepository = topicRegistrationRepository;
        this.evaluationRepository = evaluationRepository;
        this.registrationResultRepository = registrationResultRepository;
        this.reviewBoardRepository = reviewBoardRepository;
        this.reviewBoardMemberRepository = reviewBoardMemberRepository;
        this.departmentRepository = departmentRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Transactional
    public SeedResult resetAndSeed() {
        truncateAllTables();

        Map<String, RoleEntity> roles = seedRoles();
        Map<String, PermissionEntity> permissions = seedPermissions();
        seedRolePermissions(roles, permissions);
        seedDepartments();
        seedUsers(roles);
        seedRegistrationPeriods();
        List<StudentGroupEntity> studentGroups = seedStudentGroups();
        List<TopicEntity> topics = seedTopics();
        List<AnnouncementEntity> announcements = seedAnnouncements();
        List<TopicRegistrationEntity> registrations = seedTopicRegistrations();
        List<ReviewBoardEntity> boards = seedReviewBoards();
        entityManager.clear();

        return new SeedResult(
                TABLES.size(),
                ROLES.size(),
                PERMISSIONS.size(),
                DEPARTMENTS.size(),
                USERS.size(),
                1,
                studentGroups.size(),
                topics.size(),
                topics.stream().mapToInt(topic -> topic.getSupervisors().size()).sum(),
                registrations.size(),
                boards.size(),
                (int) reviewBoardMemberRepository.count(),
                countUsersWithRole("FACULTY_HEAD"),
                countUsersWithRole("LECTURER"),
                countUsersWithRole("STUDENT"),
                announcements.size(),
                (int) registrationResultRepository.count(),
                LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedPermissionsStep() {
        List<PermissionEntity> permissions = PERMISSIONS.stream()
                .map(this::upsertPermission)
                .toList();
        permissionRepository.saveAllAndFlush(permissions);
        return new SeedStepResult("permissions", permissions.size(), LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedRolesStep() {
        List<RoleEntity> roles = ROLES.stream()
                .map(this::upsertRole)
                .toList();
        roleRepository.saveAllAndFlush(roles);
        return new SeedStepResult("roles", roles.size(), LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedRolePermissionsStep() {
        Map<String, RoleEntity> roles = roleRepository.findAll().stream()
                .collect(Collectors.toMap(RoleEntity::getCode, Function.identity()));
        Map<String, PermissionEntity> permissions = permissionRepository.findAll().stream()
                .collect(Collectors.toMap(PermissionEntity::getCode, Function.identity()));

        if (!roles.keySet().containsAll(ROLE_PERMISSIONS.keySet())
                || !permissions.keySet().containsAll(ROLE_PERMISSIONS.values().stream()
                        .flatMap(List::stream)
                        .toList())) {
            throw new IllegalStateException("Seed roles and permissions before role-permissions.");
        }

        rolePermissionRepository.deleteAllInBatch();
        List<RolePermissionEntity> rolePermissions = ROLE_PERMISSIONS.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(permissionCode -> new RolePermissionEntity(
                                roles.get(entry.getKey()), permissions.get(permissionCode))))
                .toList();
        rolePermissionRepository.saveAllAndFlush(rolePermissions);
        return new SeedStepResult("role-permissions", rolePermissions.size(), LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedUsersStep() {
        Map<String, RoleEntity> roles = roleRepository.findAll().stream()
                .collect(Collectors.toMap(RoleEntity::getCode, Function.identity()));
        if (!roles.keySet().containsAll(USERS.stream().map(UserSeed::roleCode).toList())) {
            throw new IllegalStateException("Seed roles before users.");
        }
        Map<String, DepartmentEntity> departments = departmentsByCode();

        List<UserEntity> users = USERS.stream()
                .map(user -> upsertUser(
                        user,
                        roles.get(user.roleCode()),
                        departmentFor(user, departments)))
                .toList();
        userRepository.saveAllAndFlush(users);
        return new SeedStepResult("users", users.size(), LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedDepartmentsStep() {
        List<DepartmentEntity> departments = seedDepartments();
        return new SeedStepResult("departments", departments.size(), LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedRegistrationPeriodsStep() {
        List<RegistrationPeriodEntity> periods = seedRegistrationPeriods();
        return new SeedStepResult("registration-periods", periods.size(), LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedStudentGroupsStep() {
        List<StudentGroupEntity> studentGroups = seedStudentGroups();
        return new SeedStepResult("student-groups", studentGroups.size(), LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedTopicsStep() {
        List<TopicEntity> topics = seedTopics();
        return new SeedStepResult("topics", topics.size(), LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedTopicRegistrationsStep() {
        List<TopicRegistrationEntity> registrations = seedTopicRegistrations();
        return new SeedStepResult("topic-registrations", registrations.size(), LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedReviewBoardsStep() {
        if (topicRegistrationRepository.count() == 0) {
            seedTopicRegistrations();
        }
        List<ReviewBoardEntity> boards = seedReviewBoards();
        return new SeedStepResult("review-boards", boards.size(), LocalDateTime.now());
    }

    @Transactional
    public SeedStepResult seedAnnouncementsStep() {
        List<AnnouncementEntity> announcements = seedAnnouncements();
        return new SeedStepResult("announcements", announcements.size(), LocalDateTime.now());
    }

    private void truncateAllTables() {
        boolean h2 = databaseProductName().contains("h2");
        entityManager.clear();
        entityManager.createNativeQuery(h2
                ? "SET REFERENTIAL_INTEGRITY FALSE"
                : "SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        try {
            for (String table : TABLES) {
                entityManager.createNativeQuery("TRUNCATE TABLE " + table).executeUpdate();
            }
        } finally {
            entityManager.createNativeQuery(h2
                    ? "SET REFERENTIAL_INTEGRITY TRUE"
                    : "SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        }
    }

    private String databaseProductName() {
        try (var connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot detect the configured database before seeding.", exception);
        }
    }

    private Map<String, RoleEntity> seedRoles() {
        List<RoleEntity> savedRoles = roleRepository.saveAllAndFlush(ROLES.stream()
                .map(role -> new RoleEntity(role.code(), role.name(), role.description()))
                .toList());
        return savedRoles.stream().collect(Collectors.toMap(RoleEntity::getCode, Function.identity()));
    }

    private RoleEntity upsertRole(RoleSeed role) {
        RoleEntity entity = roleRepository.findByCode(role.code())
                .orElseGet(() -> new RoleEntity(role.code(), role.name(), role.description()));
        entity.setCode(role.code());
        entity.setName(role.name());
        entity.setDescription(role.description());
        entity.setActive(true);
        return entity;
    }

    private Map<String, PermissionEntity> seedPermissions() {
        List<PermissionEntity> savedPermissions = permissionRepository.saveAllAndFlush(PERMISSIONS.stream()
                .map(permission -> {
                    PermissionEntity entity = new PermissionEntity(
                            permission.code(), permission.name(), permission.permissionGroup());
                    entity.setDescription(permission.description());
                    return entity;
                })
                .toList());
        return savedPermissions.stream().collect(Collectors.toMap(PermissionEntity::getCode, Function.identity()));
    }

    private PermissionEntity upsertPermission(PermissionSeed permission) {
        PermissionEntity entity = permissionRepository.findByCode(permission.code())
                .orElseGet(() -> new PermissionEntity(
                        permission.code(), permission.name(), permission.permissionGroup()));
        entity.setCode(permission.code());
        entity.setName(permission.name());
        entity.setPermissionGroup(permission.permissionGroup());
        entity.setDescription(permission.description());
        entity.setActive(true);
        return entity;
    }

    private void seedRolePermissions(
            Map<String, RoleEntity> roles,
            Map<String, PermissionEntity> permissions) {
        List<RolePermissionEntity> rolePermissions = ROLE_PERMISSIONS.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(permissionCode -> new RolePermissionEntity(
                                roles.get(entry.getKey()), permissions.get(permissionCode))))
                .toList();
        rolePermissionRepository.saveAllAndFlush(rolePermissions);
    }

    private List<DepartmentEntity> seedDepartments() {
        List<DepartmentEntity> departments = DEPARTMENTS.stream()
                .map(this::upsertDepartment)
                .toList();
        departmentRepository.saveAllAndFlush(departments);
        return departments;
    }

    private List<RegistrationPeriodEntity> seedRegistrationPeriods() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        RegistrationPeriodEntity period = registrationPeriodRepository.findByNameIgnoreCase(SEEDED_PERIOD_NAME)
                .orElseGet(() -> new RegistrationPeriodEntity(
                        SEEDED_PERIOD_NAME,
                        PeriodType.COURSE,
                        now.minusDays(14),
                        now.plusDays(14),
                        now.minusDays(7),
                        now.plusDays(30)));
        period.setName(SEEDED_PERIOD_NAME);
        period.setType(PeriodType.COURSE);
        period.setLecturerRegistrationStart(now.minusDays(14));
        period.setLecturerRegistrationEnd(now.plusDays(14));
        period.setStudentRegistrationStart(now.minusDays(7));
        period.setStudentRegistrationEnd(now.plusDays(30));
        period.setReviewerScoreDeadline(null);
        period.setCouncilReportDate(null);
        period.setStatus(RegistrationPeriodStatus.OPEN);
        period.setCreatedBy(userRepository.findByLoginIdentifier("admin").orElse(null));
        return List.of(registrationPeriodRepository.saveAndFlush(period));
    }

    private List<TopicEntity> seedTopics() {
        RegistrationPeriodEntity period = registrationPeriodRepository.findByNameIgnoreCase(SEEDED_PERIOD_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Seed registration periods before topics."));
        Map<String, DepartmentEntity> departments = departmentsByCode();
        List<TopicEntity> topics = TOPICS.stream()
                .map(seed -> {
                    DepartmentEntity department = departments.get(seed.departmentCode().toUpperCase(Locale.ROOT));
                    if (department == null) {
                        throw new IllegalStateException("Seed department before topic: " + seed.departmentCode());
                    }
                    UserEntity proposer = requireSeedUser(seed.proposerLogin());
                    TopicEntity topic = topicRepository.findFirstByTitleIgnoreCase(seed.title())
                            .orElseGet(() -> new TopicEntity(
                                    period, department, proposer, seed.title(), seed.description()));
                    topic.setRegistrationPeriod(period);
                    topic.setDepartment(department);
                    topic.setProposedBy(proposer);
                    topic.setTitle(seed.title());
                    topic.setDescription(seed.description());
                    topic.setStatus(seed.status());
                    topic.getSupervisors().clear();
                    seed.supervisorLogins().stream()
                            .map(this::requireSeedUser)
                            .forEach(supervisor -> topic.getSupervisors().add(supervisor));
                    return topic;
                })
                .toList();
        return topicRepository.saveAllAndFlush(topics);
    }

    private List<TopicRegistrationEntity> seedTopicRegistrations() {
        RegistrationPeriodEntity period = registrationPeriodRepository.findByNameIgnoreCase(SEEDED_PERIOD_NAME)
                .orElseThrow(() -> new IllegalStateException("Seed registration periods before registrations."));
        return List.of(
                upsertSeedRegistration(period, "Nhóm Phoenix", "Nền tảng quản lý đề tài và tiến độ khóa luận"),
                upsertSeedRegistration(period, "Nhóm Nova", "Phát hiện bất thường trong kết quả học tập"),
                upsertSeedRegistration(period, "Nhóm Atlas", "Ứng dụng quản lý quy trình thực tập doanh nghiệp"));
    }

    private TopicRegistrationEntity upsertSeedRegistration(
            RegistrationPeriodEntity period, String groupName, String topicTitle) {
        StudentGroupEntity group = studentGroupRepository
                .findByRegistrationPeriod_IdAndNameIgnoreCase(period.getId(), groupName)
                .orElseThrow(() -> new IllegalStateException("Seed student group before registration: " + groupName));
        TopicEntity topic = topicRepository.findFirstByTitleIgnoreCase(topicTitle)
                .orElseThrow(() -> new IllegalStateException("Seed topic before registration: " + topicTitle));
        TopicRegistrationEntity registration = topicRegistrationRepository
                .findByStudentGroup_IdAndRegistrationPeriod_IdOrderBySubmittedAtDesc(group.getId(), period.getId())
                .stream().filter(existing -> existing.getTopic().getId().equals(topic.getId())).findFirst()
                .orElseGet(() -> new TopicRegistrationEntity(group, topic, period, group.getLeader()));
        registration.setStudentGroup(group);
        registration.setTopic(topic);
        registration.setRegistrationPeriod(period);
        registration.setSubmittedBy(group.getLeader());
        registration.setStatus(TopicRegistrationStatus.APPROVED);
        registration.setRejectionReason(null);
        return topicRegistrationRepository.saveAndFlush(registration);
    }

    private List<ReviewBoardEntity> seedReviewBoards() {
        List<ReviewBoardSeed> seeds = List.of(
                new ReviewBoardSeed("Nhóm Phoenix", "Nền tảng quản lý đề tài và tiến độ khóa luận",
                        "nguyen.van.khang", LocalDateTime.now().minusDays(2), ReviewBoardStatus.PUBLISHED,
                        "Seeded board result: all assigned evaluators submitted their scores.",
                        List.of(new BoardMemberSeed("nguyen.thanh.binh", ReviewBoardMemberRole.CHAIR,
                                        new BigDecimal("8.50"), "Clear scope and implementation."),
                                new BoardMemberSeed("dang.minh.tri", ReviewBoardMemberRole.SECRETARY,
                                        new BigDecimal("9.00"), "Strong progress and documentation."),
                                new BoardMemberSeed("bui.thanh.ha", ReviewBoardMemberRole.MEMBER,
                                        new BigDecimal("8.00"), "Good result with minor polish remaining."))),
                new ReviewBoardSeed("Nhóm Atlas", "Ứng dụng quản lý quy trình thực tập doanh nghiệp",
                        "le.quang.huy", LocalDateTime.now().plusDays(6), ReviewBoardStatus.ACTIVE, null,
                        List.of(new BoardMemberSeed("phan.tuan.anh", ReviewBoardMemberRole.CHAIR, null, null),
                                new BoardMemberSeed("huynh.thi.my.linh", ReviewBoardMemberRole.SECRETARY, null, null),
                                new BoardMemberSeed("le.quang.huy", ReviewBoardMemberRole.MEMBER, null, null))));
        RegistrationPeriodEntity period = registrationPeriodRepository.findByNameIgnoreCase(SEEDED_PERIOD_NAME)
                .orElseThrow(() -> new IllegalStateException("Seed registration periods before boards."));
        List<ReviewBoardEntity> boards = new ArrayList<>();
        for (ReviewBoardSeed seed : seeds) {
            StudentGroupEntity group = studentGroupRepository
                    .findByRegistrationPeriod_IdAndNameIgnoreCase(period.getId(), seed.groupName())
                    .orElseThrow(() -> new IllegalStateException("Seed group before board: " + seed.groupName()));
            TopicEntity topic = topicRepository.findFirstByTitleIgnoreCase(seed.topicTitle())
                    .orElseThrow(() -> new IllegalStateException("Seed topic before board: " + seed.topicTitle()));
            TopicRegistrationEntity registration = topicRegistrationRepository
                    .findByStudentGroup_IdAndRegistrationPeriod_IdOrderBySubmittedAtDesc(group.getId(), period.getId())
                    .stream().filter(existing -> existing.getTopic().getId().equals(topic.getId())).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Seed registration before board: " + seed.groupName()));
            UserEntity creator = requireSeedUser(seed.creatorLogin());
            ReviewBoardEntity board = reviewBoardRepository.findByTopicRegistration_Id(registration.getId())
                    .orElseGet(() -> new ReviewBoardEntity(registration, creator));
            board.setTopicRegistration(registration);
            board.setCreatedBy(creator);
            board.setScheduledAt(seed.scheduledAt());
            board.setStatus(seed.status());
            board = reviewBoardRepository.saveAndFlush(board);
            ReviewBoardEntity savedBoard = board;
            Map<String, UserEntity> members = seed.members().stream()
                    .collect(Collectors.toMap(BoardMemberSeed::login, member -> requireSeedUser(member.login())));
            for (BoardMemberSeed memberSeed : seed.members()) {
                UserEntity lecturer = members.get(memberSeed.login());
                ReviewBoardMemberEntity member = reviewBoardMemberRepository
                        .findByBoard_IdAndLecturer_Id(board.getId(), lecturer.getId())
                        .orElseGet(() -> new ReviewBoardMemberEntity(savedBoard, lecturer));
                member.setMemberRole(memberSeed.role());
                member.setActive(true);
                member.setEndedAt(null);
                reviewBoardMemberRepository.saveAndFlush(member);
                EvaluationEntity evaluation = evaluationRepository
                        .findByTopicRegistration_IdAndLecturer_IdAndBoard_Id(
                                registration.getId(), lecturer.getId(), board.getId())
                        .orElseGet(() -> new EvaluationEntity(registration, lecturer));
                evaluation.setBoard(board);
                evaluation.setBoardMember(member);
                evaluation.setScore(memberSeed.score());
                evaluation.setComment(memberSeed.comment());
                evaluation.setStatus(memberSeed.score() == null
                        ? EvaluationStatus.DRAFT : EvaluationStatus.PUBLISHED);
                evaluation.setSubmittedAt(memberSeed.score() == null
                        ? null : LocalDateTime.now().minusDays(1));
                evaluationRepository.saveAndFlush(evaluation);
            }
            if (seed.status() == ReviewBoardStatus.PUBLISHED) {
                BigDecimal average = seed.members().stream()
                        .map(BoardMemberSeed::score)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(seed.members().size()), 2, RoundingMode.HALF_UP);
                RegistrationResultEntity result = registrationResultRepository
                        .findByTopicRegistration_Id(registration.getId())
                        .orElseGet(() -> new RegistrationResultEntity(registration));
                LocalDateTime publishedAt = LocalDateTime.now().minusDays(1);
                result.setAverageScore(average);
                result.setFinalComment(seed.finalComment());
                result.setStatus(RegistrationResultStatus.PUBLISHED);
                result.setFinalizedBy(creator);
                result.setFinalizedAt(publishedAt);
                result.setPublishedBy(creator);
                result.setPublishedAt(publishedAt);
                registrationResultRepository.saveAndFlush(result);
            }
            boards.add(board);
        }
        return boards;
    }
    private List<AnnouncementEntity> seedAnnouncements() {
        Map<String, DepartmentEntity> departments = departmentsByCode();
        List<AnnouncementEntity> announcements = ANNOUNCEMENTS.stream()
                .map(seed -> {
                    DepartmentEntity department = seed.departmentCode() == null
                            ? null
                            : departments.get(seed.departmentCode().toUpperCase(Locale.ROOT));
                    if (seed.departmentCode() != null && department == null) {
                        throw new IllegalStateException(
                                "Seed department before announcement: " + seed.departmentCode());
                    }
                    UserEntity author = userRepository.findByLoginIdentifier(seed.authorLogin())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Seed users before announcement: " + seed.authorLogin()));
                    AnnouncementEntity announcement = seed.scope() == AnnouncementScope.SCHOOL
                            ? announcementRepository
                                    .findByTitleIgnoreCaseAndScopeAndDepartmentIsNull(seed.title(), seed.scope())
                                    .orElseGet(() -> new AnnouncementEntity(
                                            seed.title(), seed.content(), seed.scope(), null, author))
                            : announcementRepository
                                    .findByTitleIgnoreCaseAndScopeAndDepartment_Id(
                                            seed.title(), seed.scope(), department.getId())
                                    .orElseGet(() -> new AnnouncementEntity(
                                            seed.title(), seed.content(), seed.scope(), department, author));
                    announcement.setTitle(seed.title());
                    announcement.setContent(seed.content());
                    announcement.setScope(seed.scope());
                    announcement.setDepartment(department);
                    announcement.setAuthor(author);
                    announcement.setStatus(seed.status());
                    if (seed.status() == AnnouncementStatus.PUBLISHED
                            || seed.status() == AnnouncementStatus.HIDDEN) {
                        if (announcement.getPublishedAt() == null) {
                            announcement.setPublishedAt(LocalDateTime.now().minusDays(1));
                        }
                    } else {
                        announcement.setPublishedAt(null);
                    }
                    return announcement;
                })
                .toList();
        return announcementRepository.saveAllAndFlush(announcements);
    }

    private List<StudentGroupEntity> seedStudentGroups() {
        RegistrationPeriodEntity period = registrationPeriodRepository.findByNameIgnoreCase(SEEDED_PERIOD_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Seed registration periods before student groups."));

        List<StudentGroupEntity> groups = STUDENT_GROUPS.stream()
                .map(seed -> {
                    UserEntity leader = requireSeedStudent(seed.leaderLogin());
                    StudentGroupEntity group = studentGroupRepository
                            .findByRegistrationPeriod_IdAndNameIgnoreCase(period.getId(), seed.name())
                            .orElseGet(() -> new StudentGroupEntity(seed.name(), period, leader, leader));
                    group.setName(seed.name());
                    group.setRegistrationPeriod(period);
                    group.setCreatedBy(leader);
                    group.setLeader(leader);
                    group.setStatus(GroupStatus.ACTIVE);
                    group.getMembers().clear();
                    group.getMembers().add(leader);
                    seed.memberLogins().stream()
                            .map(this::requireSeedStudent)
                            .forEach(group.getMembers()::add);
                    if (group.getMembers().size() > 3) {
                        throw new IllegalStateException("Student group seed exceeds the maximum size: " + seed.name());
                    }
                    return group;
                })
                .toList();
        return studentGroupRepository.saveAllAndFlush(groups);
    }

    private UserEntity requireSeedStudent(String loginIdentifier) {
        return userRepository.findByLoginIdentifier(loginIdentifier)
                .orElseThrow(() -> new IllegalStateException(
                        "Seed users before student groups: " + loginIdentifier));
    }

    private UserEntity requireSeedUser(String loginIdentifier) {
        String lookupIdentifier = loginIdentifier.contains("@")
                ? loginIdentifier
                : loginIdentifier + LECTURER_EMAIL_DOMAIN;
        return userRepository.findByLoginIdentifier(lookupIdentifier)
                .orElseThrow(() -> new IllegalStateException(
                        "Seed users before topic: " + lookupIdentifier));
    }

    private Map<String, DepartmentEntity> departmentsByCode() {
        return departmentRepository.findAll().stream()
                .collect(Collectors.toMap(
                        department -> department.getCode().toUpperCase(Locale.ROOT),
                        Function.identity()));
    }

    private DepartmentEntity departmentFor(
            UserSeed user,
            Map<String, DepartmentEntity> departments) {
        if (user.departmentCode() == null) {
            return null;
        }
        DepartmentEntity department = departments.get(user.departmentCode().toUpperCase(Locale.ROOT));
        if (department == null) {
            throw new IllegalStateException("Seed department before users: " + user.departmentCode());
        }
        return department;
    }

    private DepartmentEntity upsertDepartment(DepartmentSeed department) {
        DepartmentEntity entity = departmentRepository.findByCodeIgnoreCase(department.code())
                .orElseGet(() -> new DepartmentEntity(department.code(), department.name()));
        entity.setCode(department.code());
        entity.setName(department.name());
        entity.setActive(true);
        return entity;
    }

    private Map<String, UserEntity> seedUsers(Map<String, RoleEntity> roles) {
        Map<String, DepartmentEntity> departments = departmentsByCode();
        List<UserEntity> savedUsers = userRepository.saveAllAndFlush(USERS.stream()
                .map(user -> {
                    UserEntity entity = new UserEntity(
                            user.loginIdentifier(), user.fullName(), passwordHashFor(user));
                    entity.setEmailOrCode(user.emailOrCode());
                    entity.setDepartment(departmentFor(user, departments));
                    return entity;
                })
                .toList());

        List<UserRoleEntity> userRoles = savedUsers.stream()
                .map(user -> new UserRoleEntity(user, roles.get(USERS.stream()
                        .filter(seed -> seed.loginIdentifier().equals(user.getLoginIdentifier()))
                        .findFirst()
                        .orElseThrow()
                        .roleCode())))
                .toList();
        userRoleRepository.saveAllAndFlush(userRoles);
        return savedUsers.stream().collect(Collectors.toMap(UserEntity::getLoginIdentifier, Function.identity()));
    }

    private UserEntity upsertUser(UserSeed user, RoleEntity role, DepartmentEntity department) {
        UserEntity entity = userRepository.findByLoginIdentifier(user.loginIdentifier())
                .orElseGet(() -> new UserEntity(
                        user.loginIdentifier(), user.fullName(), passwordHashFor(user)));
        entity.setLoginIdentifier(user.loginIdentifier());
        entity.setFullName(user.fullName());
        entity.setEmailOrCode(user.emailOrCode());
        entity.setDepartment(department);
        entity.setPasswordHash(passwordHashFor(user));
        entity.setActive(true);

        userRepository.saveAndFlush(entity);
        UserRoleEntity userRole = userRoleRepository.findByUser_Id(entity.getId()).stream()
                .filter(existing -> existing.getRole().getId().equals(role.getId()))
                .findFirst()
                .orElseGet(() -> new UserRoleEntity(entity, role));
        userRole.setActive(true);
        userRoleRepository.saveAndFlush(userRole);
        return entity;
    }

    private String passwordHashFor(UserSeed user) {
        return "ADMIN".equals(user.roleCode()) ? ADMIN_PASSWORD_HASH : LOCAL_PASSWORD_HASH;
    }

    private static int countUsersWithRole(String roleCode) {
        return (int) USERS.stream()
                .filter(user -> roleCode.equals(user.roleCode()))
                .count();
    }

    public record SeedResult(
            int tablesReset,
            int roles,
            int permissions,
            int departments,
            int users,
            int registrationPeriods,
            int studentGroups,
            int topics,
            int topicSupervisors,
            int registrations,
            int reviewBoards,
            int reviewBoardMembers,
            int facultyHeads,
            int lecturers,
            int students,
            int announcements,
            int registrationResults,
            LocalDateTime completedAt) {
    }

    private record RoleSeed(String code, String name, String description) {
    }

    private record PermissionSeed(String code, String name, String permissionGroup, String description) {
    }

    private record DepartmentSeed(String code, String name) {
    }

    private record UserSeed(
            String loginIdentifier,
            String fullName,
            String emailOrCode,
            String roleCode,
            String departmentCode) {
    }

    private record TopicSeed(
            String title,
            String description,
            String departmentCode,
            String proposerLogin,
            TopicStatus status,
            List<String> supervisorLogins) {
    }

    private record StudentGroupSeed(String name, String leaderLogin, List<String> memberLogins) {
    }

    private record ReviewBoardSeed(
            String groupName,
            String topicTitle,
            String creatorLogin,
            LocalDateTime scheduledAt,
            ReviewBoardStatus status,
            String finalComment,
            List<BoardMemberSeed> members) {
    }

    private record BoardMemberSeed(
            String login,
            ReviewBoardMemberRole role,
            BigDecimal score,
            String comment) {
    }

    private record AnnouncementSeed(
            String title,
            String content,
            AnnouncementScope scope,
            String departmentCode,
            String authorLogin,
            AnnouncementStatus status) {
    }
}
