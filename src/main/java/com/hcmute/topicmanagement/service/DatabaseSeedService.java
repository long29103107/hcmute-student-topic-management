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
            new DepartmentSeed("QLCNTT", "Quản lý công nghệ thông tin"),
            new DepartmentSeed("KT", "Kế toán"));

    private static final List<String> REGISTRATION_REVIEW_TOPIC_TITLES = List.of(
            "Phân tích chênh lệch chi phí theo trung tâm trách nhiệm",
            "Đánh giá hiệu quả kiểm soát công nợ phải thu tại doanh nghiệp thương mại",
            "Các yếu tố ảnh hưởng đến chất lượng dự báo ngân sách",
            "Ứng dụng phân tích dữ liệu trong phát hiện sai lệch chứng từ",
            "Hoàn thiện quy trình đối chiếu doanh thu và dòng tiền",
            "Tác động của chuyển đổi số đến công tác kế toán quản trị",
            "Mô hình theo dõi tuổi nợ và khả năng thu hồi công nợ",
            "Phân tích cơ cấu chi phí phục vụ quyết định giá bán",
            "Kiểm soát rủi ro trong chu trình mua hàng và thanh toán",
            "Đánh giá hiệu quả sử dụng vốn lưu động theo quý",
            "Các nhân tố ảnh hưởng đến việc áp dụng IFRS tại doanh nghiệp",
            "Thiết kế báo cáo quản trị chi phí cho doanh nghiệp dịch vụ",
            "Ứng dụng Power BI trong phân tích báo cáo tài chính",
            "Phân tích biến động lợi nhuận theo sản phẩm",
            "Giải pháp nâng cao chất lượng dữ liệu kế toán đầu vào",
            "Quản trị dòng tiền trong giai đoạn doanh nghiệp tăng trưởng",
            "Đánh giá mức độ tuân thủ quy trình phê duyệt chi phí",
            "Mô hình cảnh báo giao dịch bất thường trong sổ nhật ký",
            "Phân tích tác động của hóa đơn điện tử đến công nợ",
            "Xây dựng bộ chỉ tiêu đánh giá hiệu quả phòng kế toán");

    private static final List<String> REGISTRATION_REVIEW_TOPIC_DESCRIPTIONS = List.of(
            "Phân tích nguyên nhân chênh lệch giữa chi phí thực tế và định mức để hỗ trợ trưởng bộ phận ra quyết định.",
            "Xây dựng bộ tiêu chí theo dõi tuổi nợ, hạn thanh toán và tỷ lệ thu hồi cho doanh nghiệp thương mại.",
            "Nghiên cứu các yếu tố ảnh hưởng đến độ chính xác của dự báo ngân sách trong các kỳ kế toán.",
            "Đề xuất quy trình phát hiện và xử lý sai lệch chứng từ dựa trên dữ liệu giao dịch kế toán.",
            "Thiết kế quy trình đối chiếu doanh thu với dòng tiền thực thu nhằm giảm sai lệch cuối kỳ.",
            "Đánh giá tác động của phần mềm kế toán và tự động hóa đến chất lượng thông tin quản trị.",
            "Xây dựng mô hình phân loại tuổi nợ và cảnh báo các khoản công nợ có nguy cơ khó thu hồi.",
            "Phân tích cơ cấu chi phí theo sản phẩm để hỗ trợ xác định giá bán và biên lợi nhuận mục tiêu.",
            "Nhận diện các điểm kiểm soát quan trọng trong quy trình mua hàng, nghiệm thu và thanh toán.",
            "Đánh giá xu hướng vốn lưu động theo quý và đề xuất chỉ tiêu theo dõi hiệu quả sử dụng vốn.",
            "Khảo sát mức độ sẵn sàng và các rào cản khi doanh nghiệp áp dụng chuẩn mực IFRS.",
            "Thiết kế báo cáo quản trị chi phí theo phòng ban, hợp đồng và mức độ hoàn thành ngân sách.",
            "Xây dựng dashboard Power BI hỗ trợ phân tích doanh thu, chi phí và các chỉ số tài chính cơ bản.",
            "Phân tích biến động doanh thu, giá vốn và chi phí để xác định nguyên nhân thay đổi lợi nhuận.",
            "Đề xuất quy trình kiểm tra, chuẩn hóa và bổ sung dữ liệu kế toán đầu vào trước khi ghi sổ.",
            "Nghiên cứu giải pháp lập kế hoạch và kiểm soát dòng tiền cho doanh nghiệp đang mở rộng quy mô.",
            "Đánh giá mức độ tuân thủ các bước đề nghị, phê duyệt và thanh toán chi phí nội bộ.",
            "Xây dựng quy tắc cảnh báo giao dịch bất thường trong nhật ký chung và sổ chi tiết kế toán.",
            "Phân tích thay đổi trong việc quản lý công nợ sau khi doanh nghiệp triển khai hóa đơn điện tử.",
            "Xây dựng bộ chỉ tiêu theo dõi khối lượng xử lý, thời gian đóng sổ và chất lượng công việc phòng kế toán.");

    private static final List<String> ACCOUNTING_TOPIC_TITLES = List.of(
            "Ứng dụng phân tích dòng tiền cho doanh nghiệp vừa và nhỏ",
            "Xây dựng dashboard theo dõi công nợ phải thu theo khách hàng",
            "Mô hình cảnh báo sớm rủi ro thanh khoản trong doanh nghiệp",
            "Số hóa quy trình kiểm soát chứng từ kế toán nội bộ",
            "Phân tích hiệu quả sử dụng vốn lưu động bằng dữ liệu kế toán",
            "Hệ thống hỗ trợ lập ngân sách hoạt động cho doanh nghiệp dịch vụ",
            "Đánh giá tác động của hóa đơn điện tử đến quy trình ghi nhận doanh thu",
            "Ứng dụng RPA trong đối soát giao dịch ngân hàng và sổ cái",
            "Phân tích biến động chi phí sản xuất theo trung tâm trách nhiệm",
            "Mô hình dự báo doanh thu theo mùa vụ cho chuỗi bán lẻ",
            "Thiết kế quy trình quản trị công nợ phải trả trên nền tảng số",
            "Đánh giá chất lượng thông tin kế toán phục vụ quyết định quản trị",
            "Ứng dụng dữ liệu lớn trong phát hiện giao dịch bất thường",
            "Xây dựng bộ chỉ số đo lường hiệu quả hoạt động phòng kế toán",
            "Phân tích khả năng sinh lời theo nhóm sản phẩm và kênh phân phối",
            "Giải pháp kiểm soát chi phí marketing dựa trên ngân sách linh hoạt",
            "Nghiên cứu các yếu tố ảnh hưởng đến chất lượng báo cáo tài chính",
            "Hệ thống nhắc hạn kê khai và đối chiếu nghĩa vụ thuế doanh nghiệp",
            "Ứng dụng phân tích dữ liệu trong hỗ trợ lập báo cáo quản trị tháng",
            "Mô hình đánh giá rủi ro gian lận trong chu trình mua hàng - thanh toán");

    private static final List<String> HOANG_TOPIC_TITLES = List.of(
            "Phân tích hiệu quả kiểm soát chi phí tại doanh nghiệp thương mại",
            "Xây dựng mô hình dự báo dòng tiền ngắn hạn cho doanh nghiệp vừa và nhỏ",
            "Ứng dụng dashboard quản trị trong theo dõi công nợ khách hàng",
            "Đánh giá rủi ro sai sót trong quy trình lập và lưu trữ chứng từ",
            "Phân tích các yếu tố ảnh hưởng đến biên lợi nhuận sản phẩm",
            "Số hóa quy trình phê duyệt đề nghị thanh toán nội bộ",
            "Mô hình cảnh báo sớm khoản phải thu quá hạn",
            "Ứng dụng dữ liệu kế toán trong lập kế hoạch ngân sách phòng ban",
            "Đánh giá tác động của tự động hóa đến thời gian đóng sổ cuối kỳ",
            "Xây dựng báo cáo phân tích chi phí theo trung tâm trách nhiệm",
            "Nghiên cứu chất lượng dữ liệu đầu vào cho báo cáo tài chính",
            "Phân tích hiệu quả sử dụng vốn lưu động tại doanh nghiệp dịch vụ",
            "Thiết kế bộ chỉ tiêu theo dõi nghĩa vụ thuế và hạn kê khai",
            "Mô hình phát hiện giao dịch bất thường trong dữ liệu kế toán",
            "Giải pháp nâng cao chất lượng báo cáo quản trị tháng");

    private static final List<String> HOANG_TOPIC_DESCRIPTIONS = List.of(
            "Phân tích chi phí theo khoản mục và bộ phận để xác định các điểm có thể tối ưu trong hoạt động kinh doanh.",
            "Xây dựng mô hình dự báo thu chi theo tuần, hỗ trợ doanh nghiệp chủ động kế hoạch thanh toán ngắn hạn.",
            "Thiết kế dashboard theo dõi tuổi nợ, hạn thu tiền và tỷ lệ thu hồi theo từng nhóm khách hàng.",
            "Nhận diện các rủi ro thường gặp trong khâu lập, kiểm tra và lưu trữ chứng từ kế toán nội bộ.",
            "Phân tích doanh thu, giá vốn và chi phí để xác định các yếu tố làm thay đổi biên lợi nhuận sản phẩm.",
            "Đề xuất luồng số hóa từ lúc lập đề nghị thanh toán đến khi phê duyệt và đối chiếu chứng từ.",
            "Xây dựng bộ tiêu chí phân loại và cảnh báo các khoản phải thu có nguy cơ chuyển thành nợ quá hạn.",
            "Khai thác dữ liệu kế toán để lập ngân sách, theo dõi thực hiện và phân tích chênh lệch theo phòng ban.",
            "Đánh giá thời gian xử lý trước và sau tự động hóa, tập trung vào các bước đối chiếu và khóa sổ.",
            "Thiết kế báo cáo chi phí theo trung tâm trách nhiệm để hỗ trợ trưởng bộ phận kiểm soát ngân sách.",
            "Khảo sát tính đầy đủ, chính xác và nhất quán của dữ liệu đầu vào trước khi lập báo cáo tài chính.",
            "Phân tích vòng quay hàng tồn kho, công nợ và tiền mặt để đánh giá hiệu quả sử dụng vốn lưu động.",
            "Xây dựng lịch theo dõi số thuế phải nộp, thời hạn kê khai và tình trạng hoàn tất hồ sơ định kỳ.",
            "Áp dụng các quy tắc phân tích dữ liệu để nhận diện giao dịch có dấu hiệu bất thường trong sổ kế toán.",
            "Tổng hợp các chỉ tiêu doanh thu, chi phí và công nợ thành báo cáo quản trị tháng dễ theo dõi.");

    private static final List<UserSeed> FACULTY_HEADS = List.of(
            facultyHead("nguyen.van.khang", "PGS. TS. Nguyễn Văn Khang", "CNTT"),
            facultyHead("tran.thi.hong.gam", "TS. Trần Thị Hồng Gấm", "KHMT"),
            facultyHead("le.quang.huy", "TS. Lê Quang Huy", "CNPM"),
            facultyHead("pham.minh.tuan", "PGS. TS. Phạm Minh Tuấn", "HTTT"),
            facultyHead("hoang.thai.xuan.khoa", "Hoàng Thái Xuân Khoa", "KT"));

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
            lecturer("cao.ngoc.han", "Cao Ngọc Hân", "HTTT"),
            lecturer("nguyen.hoang.long", "Nguyễn Hoàng Long", "KT"),
            lecturer("nguyen.anh.quan", "Nguyễn Anh Quân", "KT"),
            lecturer("thai.gia.khang", "Thái Gia Khang", "KT"),
            lecturer("nguyen.anh.minh", "Nguyễn Anh Minh", "KT"));

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
            student("24110049", "Nguyễn Đức Toàn", "HTTT"),
            student("24910000", "Dương Gia Huy", "KT"),
            student("24910001", "Vương Tâm", "KT"),
            student("24910002", "Châu Thành Lợi", "KT"));

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
            new StudentGroupSeed("Nhóm Phoenix", "24110000", List.of("24110001", "24110002"), GroupStatus.ACTIVE),
            new StudentGroupSeed("Nhóm Orion", "24110003", List.of("24110004", "24110005"), GroupStatus.ACTIVE),
            new StudentGroupSeed("Nhóm Nova", "24110013", List.of("24110014", "24110015"), GroupStatus.ACTIVE),
            new StudentGroupSeed("Nhóm Atlas", "24110026", List.of("24110027", "24110028"), GroupStatus.ACTIVE),
            new StudentGroupSeed("Nhóm Nebula", "24110006", List.of("24110007", "24110008"), GroupStatus.ACTIVE),
            new StudentGroupSeed("Nhóm Cosmos", "24110009", List.of("24110010", "24110011"), GroupStatus.ACTIVE),
            new StudentGroupSeed("Nhóm Pixel", "24110012", List.of("24110016", "24110017"), GroupStatus.ACTIVE),
            new StudentGroupSeed("Nhóm Vertex", "24110018", List.of("24110019", "24110020"), GroupStatus.ACTIVE),
            new StudentGroupSeed("Nhóm Quantum", "24110021", List.of("24110022", "24110023"), GroupStatus.ACTIVE),
            new StudentGroupSeed("Nhóm Matrix", "24110024", List.of("24110025", "24110029"), GroupStatus.ACTIVE),
            new StudentGroupSeed("Nhóm CodeLab", "24110030", List.of("24110031", "24110032"), GroupStatus.COMPLETED),
            new StudentGroupSeed("Nhóm DevHub", "24110033", List.of("24110034", "24110035"), GroupStatus.COMPLETED),
            new StudentGroupSeed("Nhóm Cloud", "24110036", List.of("24110037", "24110038"), GroupStatus.INACTIVE),
            new StudentGroupSeed("Nhóm Data", "24110039", List.of("24110040", "24110041"), GroupStatus.INACTIVE),
            new StudentGroupSeed("Nhóm SmartLab", "24110042", List.of("24110043", "24110044"), GroupStatus.ACTIVE),
            new StudentGroupSeed("Nhóm WebCore", "24110045", List.of("24110046", "24110047"), GroupStatus.ACTIVE),
            new StudentGroupSeed("Nhóm InfoSec", "24110048", List.of("24110049", "24910000"), GroupStatus.COMPLETED),
            new StudentGroupSeed("Nhóm SysNet", "24910001", List.of("24910002"), GroupStatus.INACTIVE),
            new StudentGroupSeed("Nhóm AI Lab", "24910000", List.of("24910002"), GroupStatus.ACTIVE),
            new StudentGroupSeed("Nhóm IoT Lab", "24110021", List.of("24110023"), GroupStatus.COMPLETED));

    private static final List<PeriodStudentGroupSeed> PERIOD_STUDENT_GROUPS = List.of(
            new PeriodStudentGroupSeed(1, "Nhóm Sổ Cái", "24910000", List.of("24910001", "24910002"), GroupStatus.ACTIVE),
            new PeriodStudentGroupSeed(1, "Nhóm FinSight", "24110000", List.of("24110001", "24110002"), GroupStatus.ACTIVE),
            new PeriodStudentGroupSeed(1, "Nhóm CloudLedger", "24110013", List.of("24110014", "24110015"), GroupStatus.COMPLETED),
            new PeriodStudentGroupSeed(1, "Nhóm AuditFlow", "24110026", List.of("24110027", "24110028"), GroupStatus.COMPLETED),
            new PeriodStudentGroupSeed(2, "Nhóm Kế Toán Số", "24910001", List.of("24910002"), GroupStatus.ACTIVE),
            new PeriodStudentGroupSeed(2, "Nhóm Campus Insight", "24110003", List.of("24110004", "24110005"), GroupStatus.COMPLETED),
            new PeriodStudentGroupSeed(2, "Nhóm Data Compass", "24110016", List.of("24110017", "24110018"), GroupStatus.INACTIVE),
            new PeriodStudentGroupSeed(2, "Nhóm DevFinance", "24110029", List.of("24110030", "24110031"), GroupStatus.ACTIVE),
            new PeriodStudentGroupSeed(3, "Nhóm TaxMate", "24910002", List.of("24910000", "24910001"), GroupStatus.COMPLETED),
            new PeriodStudentGroupSeed(3, "Nhóm Smart Campus", "24110006", List.of("24110007", "24110008"), GroupStatus.ACTIVE),
            new PeriodStudentGroupSeed(3, "Nhóm AlgoVision", "24110019", List.of("24110020", "24110021"), GroupStatus.ACTIVE),
            new PeriodStudentGroupSeed(3, "Nhóm Process Hub", "24110032", List.of("24110033", "24110034"), GroupStatus.INACTIVE),
            new PeriodStudentGroupSeed(4, "Nhóm Finance Pulse", "24910000", List.of("24910002"), GroupStatus.ACTIVE),
            new PeriodStudentGroupSeed(4, "Nhóm EduTech Link", "24110009", List.of("24110010", "24110011"), GroupStatus.COMPLETED),
            new PeriodStudentGroupSeed(4, "Nhóm Machine Mind", "24110022", List.of("24110023", "24110024"), GroupStatus.COMPLETED),
            new PeriodStudentGroupSeed(4, "Nhóm Service Desk", "24110035", List.of("24110036", "24110037"), GroupStatus.ACTIVE),
            new PeriodStudentGroupSeed(5, "Nhóm Budget Lens", "24910001", List.of("24910000"), GroupStatus.INACTIVE),
            new PeriodStudentGroupSeed(5, "Nhóm Green Campus", "24110012", List.of("24110004", "24110005"), GroupStatus.ACTIVE),
            new PeriodStudentGroupSeed(5, "Nhóm Vision Stack", "24110025", List.of("24110016", "24110017"), GroupStatus.COMPLETED),
            new PeriodStudentGroupSeed(5, "Nhóm OmniFlow", "24110038", List.of("24110039", "24110040"), GroupStatus.ACTIVE));

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
        List<RegistrationPeriodEntity> registrationPeriods = seedRegistrationPeriods();
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
                registrationPeriods.size(),
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
        UserEntity admin = userRepository.findByLoginIdentifier("admin").orElse(null);
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
        period.setCreatedBy(admin);

        List<RegistrationPeriodEntity> periods = new ArrayList<>();
        periods.add(period);
        PeriodType[] types = PeriodType.values();
        for (int index = 1; index <= 20; index++) {
            String number = String.format(Locale.ROOT, "%02d", index);
            String name = "Đợt đăng ký mẫu " + number + " - năm học 2025-2026";
            LocalDateTime lecturerStart = now.minusDays(45L + index * 30L);
            LocalDateTime lecturerEnd = lecturerStart.plusDays(14);
            LocalDateTime studentStart = lecturerEnd.plusDays(2);
            LocalDateTime studentEnd = studentStart.plusDays(30);
            RegistrationPeriodEntity sample = registrationPeriodRepository.findByNameIgnoreCase(name).orElse(null);
            if (sample == null) {
                sample = new RegistrationPeriodEntity(
                        name,
                        types[(index - 1) % types.length],
                        lecturerStart,
                        lecturerEnd,
                        studentStart,
                        studentEnd);
            }
            sample.setName(name);
            sample.setType(types[(index - 1) % types.length]);
            sample.setLecturerRegistrationStart(lecturerStart);
            sample.setLecturerRegistrationEnd(lecturerEnd);
            sample.setStudentRegistrationStart(studentStart);
            sample.setStudentRegistrationEnd(studentEnd);
            sample.setReviewerScoreDeadline(studentEnd.plusDays(14));
            sample.setCouncilReportDate(studentEnd.plusDays(21));
            sample.setStatus(switch (index % 4) {
                case 1 -> RegistrationPeriodStatus.DRAFT;
                case 2 -> RegistrationPeriodStatus.OPEN;
                case 3 -> RegistrationPeriodStatus.CLOSED;
                default -> RegistrationPeriodStatus.ARCHIVED;
            });
            sample.setCreatedBy(admin);
            periods.add(sample);
        }
        return registrationPeriodRepository.saveAllAndFlush(periods);
    }

    private List<TopicEntity> seedTopics() {
        RegistrationPeriodEntity period = registrationPeriodRepository.findByNameIgnoreCase(SEEDED_PERIOD_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Seed registration periods before topics."));
        Map<String, DepartmentEntity> departments = departmentsByCode();
        List<TopicEntity> topics = allTopicSeeds().stream()
                .map(seed -> {
                    DepartmentEntity department = departments.get(seed.departmentCode().toUpperCase(Locale.ROOT));
                    if (department == null) {
                        throw new IllegalStateException("Seed department before topic: " + seed.departmentCode());
                    }
                    UserEntity proposer = requireSeedUser(seed.proposerLogin());
                    TopicEntity topic = topicRepository.findFirstByTitleIgnoreCase(seed.title()).orElse(null);
                    if (topic == null && seed.title().startsWith("Đề tài mẫu Hoàng Thái Xuân Khoa ")) {
                        String legacyTitle = seed.title().replace(
                                "Đề tài mẫu Hoàng Thái Xuân Khoa ", "Đề tài mẫu Bùi Thanh Hà ");
                        topic = topicRepository.findFirstByTitleIgnoreCase(legacyTitle).orElse(null);
                    }
                    if (topic == null) {
                        topic = new TopicEntity(period, department, proposer, seed.title(), seed.description());
                    }
                    topic.setRegistrationPeriod(period);
                    topic.setDepartment(department);
                    topic.setProposedBy(proposer);
                    topic.setTitle(seed.title());
                    topic.setDescription(seed.description());
                    topic.setStatus(seed.status());
                    topic.getSupervisors().clear();
                    for (String supervisorLogin : seed.supervisorLogins()) {
                        topic.getSupervisors().add(requireSeedUser(supervisorLogin));
                    }
                    return topic;
                })
                .toList();
        return topicRepository.saveAllAndFlush(topics);
    }

    /**
     * Adds deterministic topic fixtures so every status has enough rows for
     * searching, sorting, filtering, and pagination in the topic directory.
     */
    private static List<TopicSeed> allTopicSeeds() {
        List<TopicSeed> seeds = new ArrayList<>(TOPICS);
        for (int index = 1; index <= 10; index++) {
            String number = String.format(Locale.ROOT, "%02d", index);
            seeds.add(new TopicSeed(
                    "Đề tài mẫu Khang DRAFT " + number,
                    "Dữ liệu mẫu cho danh sách đề tài cá nhân và kiểm thử phân trang của giảng viên.",
                    "CNTT",
                    "nguyen.van.khang",
                    TopicStatus.DRAFT,
                    List.of("vo.hoang.nam")));
        }
        List<String> departmentCodes = List.of("CNTT", "KHMT", "CNPM", "HTTT");
        List<String> proposerLogins = List.of(
                "nguyen.thanh.binh", "nguyen.quoc.viet", "phan.tuan.anh", "hoang.duc.long");
        List<String> supervisorLogins = List.of(
                "nguyen.van.khang", "tran.thi.hong.gam", "le.quang.huy", "pham.minh.tuan");

        for (int index = 1; index <= 20; index++) {
            int ownerIndex = (index - 1) % departmentCodes.size();
            String number = String.format(Locale.ROOT, "%02d", index);
            TopicStatus status = switch (index % 4) {
                case 0 -> TopicStatus.PENDING_APPROVAL;
                case 1 -> TopicStatus.DRAFT;
                case 2 -> TopicStatus.REJECTED;
                default -> TopicStatus.APPROVED;
            };
            seeds.add(new TopicSeed(
                    "Đề tài mẫu Hoàng Thái Xuân Khoa " + number,
                    "Dữ liệu mẫu cho topic proposal của giảng viên và kiểm thử phân trang.",
                    departmentCodes.get(ownerIndex),
                    "hoang.thai.xuan.khoa",
                    status,
                    List.of(supervisorLogins.get(ownerIndex))));
        }

        List<String> ktLecturerLogins = List.of(
                "nguyen.hoang.long", "nguyen.anh.quan", "thai.gia.khang", "nguyen.anh.minh");
        for (int index = 0; index < HOANG_TOPIC_TITLES.size(); index++) {
            TopicStatus status = switch (index % 4) {
                case 0 -> TopicStatus.DRAFT;
                case 1 -> TopicStatus.PENDING_APPROVAL;
                case 2 -> TopicStatus.REJECTED;
                default -> TopicStatus.APPROVED;
            };
            seeds.add(new TopicSeed(
                    HOANG_TOPIC_TITLES.get(index),
                    HOANG_TOPIC_DESCRIPTIONS.get(index),
                    "KT",
                    "hoang.thai.xuan.khoa",
                    status,
                    List.of(ktLecturerLogins.get(index % ktLecturerLogins.size()))));
        }
        for (int index = 1; index <= 20; index++) {
            int ownerIndex = (index - 1) % departmentCodes.size();
            seeds.add(new TopicSeed(
                    REGISTRATION_REVIEW_TOPIC_TITLES.get(index - 1),
                    REGISTRATION_REVIEW_TOPIC_DESCRIPTIONS.get(index - 1),
                    "KT",
                    ktLecturerLogins.get(ownerIndex),
                    TopicStatus.PENDING_APPROVAL,
                    List.of(ktLecturerLogins.get(ownerIndex))));
        }

        for (int index = 1; index <= 20; index++) {
            int ownerIndex = (index - 1) % ktLecturerLogins.size();
            String number = String.format(Locale.ROOT, "%02d", index);
            seeds.add(new TopicSeed(
                    "Đề tài evaluator KT " + number,
                    "Dữ liệu mẫu đã duyệt để kiểm thử danh sách phân công evaluator của khoa KT.",
                    "KT",
                    ktLecturerLogins.get(ownerIndex),
                    TopicStatus.PUBLISHED,
                    List.of(ktLecturerLogins.get((ownerIndex + 1) % ktLecturerLogins.size()))));
        }

        List<String> accountingTopicDescriptions = List.of(
                "Xây dựng bộ chỉ tiêu và giao diện phân tích dòng tiền theo tháng, giúp nhà quản trị theo dõi dòng tiền vào ra và nhận diện thời điểm thiếu hụt vốn.",
                "Thiết kế dashboard tổng hợp tuổi nợ, hạn thanh toán và lịch sử thu tiền để hỗ trợ kế toán công nợ ưu tiên các khoản cần xử lý.",
                "Nghiên cứu các chỉ số thanh khoản, khả năng trả nợ và dòng tiền để xây dựng mô hình cảnh báo sớm cho doanh nghiệp.",
                "Đề xuất quy trình tiếp nhận, phê duyệt, lưu trữ và tra cứu chứng từ nhằm giảm thao tác thủ công và tăng khả năng kiểm soát nội bộ.",
                "Phân tích dữ liệu hàng tồn kho, công nợ và tiền mặt để đánh giá hiệu quả sử dụng vốn lưu động trong các kỳ kế toán.",
                "Xây dựng mẫu ngân sách theo doanh thu, chi phí và công suất hoạt động, kèm cơ chế so sánh ngân sách với số liệu thực tế.",
                "Đánh giá thay đổi trong khâu lập hóa đơn, ghi nhận doanh thu và đối chiếu công nợ sau khi doanh nghiệp chuyển sang hóa đơn điện tử.",
                "Thiết kế quy trình tự động đối soát sao kê ngân hàng với sổ cái, tập trung vào nhận diện giao dịch lệch và giảm thời gian kiểm tra.",
                "Phân tích dữ liệu chi phí theo phân xưởng và trung tâm trách nhiệm để hỗ trợ xác định nguyên nhân chênh lệch so với định mức.",
                "Xây dựng mô hình dự báo doanh thu theo tháng dựa trên lịch sử bán hàng, mùa vụ và các chương trình khuyến mãi của chuỗi bán lẻ.",
                "Số hóa luồng đề nghị mua hàng, nhận hóa đơn, kiểm tra công nợ và phê duyệt thanh toán giữa các bộ phận trong doanh nghiệp.",
                "Khảo sát mức độ đầy đủ, kịp thời và dễ hiểu của thông tin kế toán trong quá trình lập kế hoạch và ra quyết định quản trị.",
                "Áp dụng các quy tắc và phương pháp phân tích dữ liệu để phát hiện giao dịch có dấu hiệu bất thường trong nhật ký kế toán.",
                "Xây dựng bộ chỉ số theo dõi khối lượng xử lý, thời gian đóng sổ và tỷ lệ sai lệch của phòng kế toán theo từng tháng.",
                "Phân tích doanh thu, giá vốn và chi phí theo sản phẩm và kênh phân phối để xác định nhóm mang lại biên lợi nhuận tốt nhất.",
                "Đề xuất cách phân bổ ngân sách marketing linh hoạt theo mục tiêu chiến dịch và đánh giá chênh lệch giữa chi phí kế hoạch và thực tế.",
                "Nghiên cứu các yếu tố về quy trình, nhân sự và hệ thống ảnh hưởng đến chất lượng báo cáo tài chính tại doanh nghiệp.",
                "Thiết kế lịch nhắc và bảng theo dõi nghĩa vụ thuế, kết hợp đối chiếu số liệu kê khai với sổ chi tiết trước khi nộp hồ sơ.",
                "Tổng hợp dữ liệu doanh thu, chi phí và công nợ để hỗ trợ lập báo cáo quản trị tháng với các chỉ tiêu có thể drill-down.",
                "Phân tích điểm kiểm soát trong chu trình mua hàng - thanh toán và xây dựng mô hình chấm điểm rủi ro gian lận cho từng giao dịch.");
        for (int index = 0; index < ACCOUNTING_TOPIC_TITLES.size(); index++) {
            seeds.add(new TopicSeed(
                    ACCOUNTING_TOPIC_TITLES.get(index),
                    accountingTopicDescriptions.get(index),
                    "KT",
                    ktLecturerLogins.get(index % ktLecturerLogins.size()),
                    TopicStatus.APPROVED,
                    List.of("hoang.thai.xuan.khoa")));
        }

        for (TopicStatus status : TopicStatus.values()) {
            int existingCount = (int) TOPICS.stream()
                    .filter(topic -> topic.status() == status)
                    .count();
            int targetCount = status == TopicStatus.PUBLISHED ? 40 : 10;
            int topicsToAdd = Math.max(0, targetCount - existingCount);
            for (int index = 0; index < topicsToAdd; index++) {
                int ownerIndex = index % departmentCodes.size();
                String statusLabel = status.name().replace('_', ' ');
                String number = String.format(Locale.ROOT, "%02d", index + 1);
                seeds.add(new TopicSeed(
                        "Đề tài mẫu " + statusLabel + " " + number,
                        "Dữ liệu mẫu để kiểm thử danh sách đề tài, bộ lọc, sắp xếp và phân trang.",
                        departmentCodes.get(ownerIndex),
                        proposerLogins.get(ownerIndex),
                        status,
                        List.of(supervisorLogins.get(ownerIndex))));
            }
        }
        return seeds;
    }

    private List<TopicRegistrationEntity> seedTopicRegistrations() {
        RegistrationPeriodEntity period = registrationPeriodRepository.findByNameIgnoreCase(SEEDED_PERIOD_NAME)
                .orElseThrow(() -> new IllegalStateException("Seed registration periods before registrations."));
        List<TopicRegistrationEntity> registrations = new ArrayList<>(List.of(
                upsertSeedRegistration(period, "Nhóm Phoenix", "Nền tảng quản lý đề tài và tiến độ khóa luận"),
                upsertSeedRegistration(period, "Nhóm Nova", "Phát hiện bất thường trong kết quả học tập"),
                upsertSeedRegistration(period, "Nhóm Atlas", "Ứng dụng quản lý quy trình thực tập doanh nghiệp")));
        List<String> groupNames = List.of("Nhóm Phoenix", "Nhóm Orion", "Nhóm Nova", "Nhóm Atlas");
        for (int index = 1; index <= 15; index++) {
            String number = String.format(Locale.ROOT, "%02d", index);
            registrations.add(upsertSeedRegistration(
                    period,
                    groupNames.get((index - 1) % groupNames.size()),
                    "Đề tài mẫu PUBLISHED " + number,
                    TopicRegistrationStatus.PENDING));
        }
        for (int index = 16; index <= 23; index++) {
            String number = String.format(Locale.ROOT, "%02d", index);
            registrations.add(upsertSeedRegistration(
                    period,
                    groupNames.get((index - 1) % groupNames.size()),
                    "Đề tài mẫu PUBLISHED " + number,
                    TopicRegistrationStatus.APPROVED));
        }
        for (int index = 1; index <= 20; index++) {
            String number = String.format(Locale.ROOT, "%02d", index);
            registrations.add(upsertSeedRegistration(
                    period,
                    groupNames.get((index - 1) % groupNames.size()),
                    "Đề tài evaluator KT " + number,
                    TopicRegistrationStatus.APPROVED));
        }
        List<String> ktGroupNames = List.of("Nhóm InfoSec", "Nhóm SysNet", "Nhóm AI Lab");
        for (int index = 0; index < REGISTRATION_REVIEW_TOPIC_TITLES.size(); index++) {
            registrations.add(upsertSeedRegistration(
                    period,
                    ktGroupNames.get(index % ktGroupNames.size()),
                    REGISTRATION_REVIEW_TOPIC_TITLES.get(index),
                    TopicRegistrationStatus.PENDING));
        }
        for (int index = 0; index < ACCOUNTING_TOPIC_TITLES.size(); index++) {
            registrations.add(upsertSeedRegistration(
                    period,
                    ktGroupNames.get(index % ktGroupNames.size()),
                    ACCOUNTING_TOPIC_TITLES.get(index),
                    TopicRegistrationStatus.APPROVED));
        }
        return registrations;
    }

    private TopicRegistrationEntity upsertSeedRegistration(
            RegistrationPeriodEntity period, String groupName, String topicTitle) {
        return upsertSeedRegistration(period, groupName, topicTitle, TopicRegistrationStatus.APPROVED);
    }

    private TopicRegistrationEntity upsertSeedRegistration(
            RegistrationPeriodEntity period, String groupName, String topicTitle,
            TopicRegistrationStatus status) {
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
        registration.setStatus(status);
        registration.setRejectionReason(null);
        return topicRegistrationRepository.saveAndFlush(registration);
    }

    private List<ReviewBoardEntity> seedReviewBoards() {
        List<ReviewBoardSeed> seeds = new ArrayList<>(List.of(
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
                                new BoardMemberSeed("le.quang.huy", ReviewBoardMemberRole.MEMBER, null, null)))));
        List<String> ktGroupNames = List.of("Nhóm InfoSec", "Nhóm SysNet", "Nhóm AI Lab");
        List<String> ktLecturerLogins = List.of(
                "nguyen.hoang.long", "nguyen.anh.quan", "thai.gia.khang", "nguyen.anh.minh");
        for (int index = 0; index < ACCOUNTING_TOPIC_TITLES.size(); index++) {
            ReviewBoardStatus status = switch (index % 5) {
                case 0 -> ReviewBoardStatus.DRAFT;
                case 1 -> ReviewBoardStatus.ASSIGNED;
                case 2 -> ReviewBoardStatus.SCHEDULED;
                case 3 -> ReviewBoardStatus.ACTIVE;
                default -> ReviewBoardStatus.COMPLETED;
            };
            String chair = ktLecturerLogins.get(index % ktLecturerLogins.size());
            String secretary = ktLecturerLogins.get((index + 1) % ktLecturerLogins.size());
            String member = ktLecturerLogins.get((index + 2) % ktLecturerLogins.size());
            seeds.add(new ReviewBoardSeed(
                    ktGroupNames.get(index % ktGroupNames.size()),
                    ACCOUNTING_TOPIC_TITLES.get(index),
                    "hoang.thai.xuan.khoa",
                    LocalDateTime.now().plusDays(index + 2).withHour(9).withMinute(30),
                    status,
                    null,
                    List.of(
                            new BoardMemberSeed(chair, ReviewBoardMemberRole.CHAIR, null, null),
                            new BoardMemberSeed(secretary, ReviewBoardMemberRole.SECRETARY, null, null),
                            new BoardMemberSeed(member, ReviewBoardMemberRole.MEMBER, null, null))));
        }
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
        seedStandaloneEvaluations(period);
        seedRegistrationResults(period);
        seedHoangScoringAssignments(period);
        return boards;
    }

    private void seedStandaloneEvaluations(RegistrationPeriodEntity period) {
        List<String> groupNames = List.of("Nhóm Phoenix", "Nhóm Orion", "Nhóm Nova", "Nhóm Atlas");
        List<String> evaluatorLogins = List.of(
                "phan.tuan.anh", "huynh.thi.my.linh", "le.quang.huy", "nguyen.thanh.binh");
        for (int index = 16; index <= 23; index++) {
            String number = String.format(Locale.ROOT, "%02d", index);
            StudentGroupEntity group = studentGroupRepository
                    .findByRegistrationPeriod_IdAndNameIgnoreCase(period.getId(),
                            groupNames.get((index - 1) % groupNames.size()))
                    .orElseThrow(() -> new IllegalStateException("Seed group before evaluation."));
            TopicEntity topic = topicRepository.findFirstByTitleIgnoreCase("Đề tài mẫu PUBLISHED " + number)
                    .orElseThrow(() -> new IllegalStateException("Seed topic before evaluation: " + number));
            TopicRegistrationEntity registration = topicRegistrationRepository
                    .findByStudentGroup_IdAndRegistrationPeriod_IdOrderBySubmittedAtDesc(group.getId(), period.getId())
                    .stream().filter(existing -> existing.getTopic().getId().equals(topic.getId())).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Seed registration before evaluation: " + number));
            UserEntity evaluator = requireSeedUser(evaluatorLogins.get((index - 16) % evaluatorLogins.size()));
            EvaluationEntity evaluation = evaluationRepository
                    .findByTopicRegistration_IdAndLecturer_Id(registration.getId(), evaluator.getId())
                    .orElseGet(() -> new EvaluationEntity(registration, evaluator));
            evaluation.setBoard(null);
            evaluation.setBoardMember(null);
            evaluation.setScore(null);
            evaluation.setComment(null);
            evaluation.setStatus(EvaluationStatus.DRAFT);
            evaluation.setSubmittedAt(null);
            evaluationRepository.saveAndFlush(evaluation);
        }
    }

    private void seedHoangScoringAssignments(RegistrationPeriodEntity period) {
        List<String> groupNames = List.of("Nhóm Phoenix", "Nhóm Orion", "Nhóm Nova", "Nhóm Atlas");
        UserEntity evaluator = requireSeedUser("hoang.thai.xuan.khoa");
        for (int index = 1; index <= 15; index++) {
            String number = String.format(Locale.ROOT, "%02d", index);
            String topicTitle = "Đề tài evaluator KT " + number;
            StudentGroupEntity group = studentGroupRepository
                    .findByRegistrationPeriod_IdAndNameIgnoreCase(
                            period.getId(), groupNames.get((index - 1) % groupNames.size()))
                    .orElseThrow(() -> new IllegalStateException("Seed group before Hoang scoring assignment."));
            TopicEntity topic = topicRepository.findFirstByTitleIgnoreCase(topicTitle)
                    .orElseThrow(() -> new IllegalStateException(
                            "Seed topic before Hoang scoring assignment: " + topicTitle));
            TopicRegistrationEntity registration = topicRegistrationRepository
                    .findByStudentGroup_IdAndRegistrationPeriod_IdOrderBySubmittedAtDesc(
                            group.getId(), period.getId())
                    .stream()
                    .filter(existing -> existing.getTopic().getId().equals(topic.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Seed registration before Hoang scoring assignment: " + topicTitle));
            EvaluationEntity evaluation = evaluationRepository
                    .findByTopicRegistration_IdAndLecturer_Id(registration.getId(), evaluator.getId())
                    .orElseGet(() -> new EvaluationEntity(registration, evaluator));
            evaluation.setBoard(null);
            evaluation.setBoardMember(null);
            evaluation.setScore(null);
            evaluation.setComment(null);
            evaluation.setStatus(EvaluationStatus.DRAFT);
            evaluation.setSubmittedAt(null);
            evaluationRepository.saveAndFlush(evaluation);
        }
    }

    private List<RegistrationResultEntity> seedRegistrationResults(RegistrationPeriodEntity period) {
        List<String> ktGroupNames = List.of("Nhóm InfoSec", "Nhóm SysNet", "Nhóm AI Lab");
        UserEntity publisher = requireSeedUser("hoang.thai.xuan.khoa");
        List<RegistrationResultEntity> results = new ArrayList<>();
        for (int index = 0; index < 15; index++) {
            String groupName = ktGroupNames.get(index % ktGroupNames.size());
            String topicTitle = ACCOUNTING_TOPIC_TITLES.get(index);
            StudentGroupEntity group = studentGroupRepository
                    .findByRegistrationPeriod_IdAndNameIgnoreCase(
                            period.getId(), groupName)
                    .orElseThrow(() -> new IllegalStateException("Seed group before result: " + groupName));
            TopicEntity topic = topicRepository.findFirstByTitleIgnoreCase(topicTitle)
                    .orElseThrow(() -> new IllegalStateException("Seed topic before result: " + topicTitle));
            TopicRegistrationEntity registration = topicRegistrationRepository
                    .findByStudentGroup_IdAndRegistrationPeriod_IdOrderBySubmittedAtDesc(
                            group.getId(), period.getId())
                    .stream()
                    .filter(existing -> existing.getTopic().getId().equals(topic.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Seed registration before result: " + topicTitle));
            ReviewBoardEntity board = reviewBoardRepository.findByTopicRegistration_Id(registration.getId())
                    .orElseThrow(() -> new IllegalStateException("Seed board before result: " + topicTitle));
            List<ReviewBoardMemberEntity> members = reviewBoardMemberRepository
                    .findByBoard_IdAndActiveTrueOrderByMemberRoleAscAssignedAtAsc(board.getId());
            BigDecimal firstScore = BigDecimal.valueOf(7.25 + (index % 4) * 0.25);
            List<BigDecimal> scores = List.of(
                    firstScore,
                    firstScore.add(BigDecimal.valueOf(0.50)),
                    firstScore.add(BigDecimal.valueOf(0.25)));
            for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
                ReviewBoardMemberEntity member = members.get(memberIndex);
                EvaluationEntity evaluation = evaluationRepository
                        .findByTopicRegistration_IdAndLecturer_IdAndBoard_Id(
                                registration.getId(), member.getLecturer().getId(), board.getId())
                        .orElseGet(() -> new EvaluationEntity(registration, member.getLecturer()));
                evaluation.setBoard(board);
                evaluation.setBoardMember(member);
                evaluation.setScore(scores.get(memberIndex % scores.size()));
                evaluation.setComment("Đánh giá tiến độ và chất lượng triển khai của nhóm.");
                evaluation.setStatus(EvaluationStatus.PUBLISHED);
                evaluation.setSubmittedAt(LocalDateTime.now().minusDays(index + 1));
                evaluationRepository.saveAndFlush(evaluation);
            }

            RegistrationResultStatus status = switch (index % 5) {
                case 0, 2 -> RegistrationResultStatus.DRAFT;
                case 1, 3 -> RegistrationResultStatus.FINALIZED;
                default -> RegistrationResultStatus.PUBLISHED;
            };
            BigDecimal average = scores.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
            RegistrationResultEntity result = registrationResultRepository
                    .findByTopicRegistration_Id(registration.getId())
                    .orElseGet(() -> new RegistrationResultEntity(registration));
            result.setAverageScore(average);
            result.setFinalComment(status == RegistrationResultStatus.DRAFT
                    ? "Hồ sơ đang được hoàn thiện trước khi công bố kết quả."
                    : "Kết quả được tổng hợp từ các phiếu đánh giá của hội đồng.");
            result.setStatus(status);
            result.setFinalizedBy(status == RegistrationResultStatus.DRAFT ? null : publisher);
            result.setFinalizedAt(status == RegistrationResultStatus.DRAFT
                    ? null : LocalDateTime.now().minusDays(index + 1));
            result.setPublishedBy(status == RegistrationResultStatus.PUBLISHED ? publisher : null);
            result.setPublishedAt(status == RegistrationResultStatus.PUBLISHED
                    ? LocalDateTime.now().minusDays(index + 1) : null);
            results.add(registrationResultRepository.saveAndFlush(result));
        }
        return results;
    }

    private List<AnnouncementEntity> seedAnnouncements() {
        Map<String, DepartmentEntity> departments = departmentsByCode();
        List<AnnouncementSeed> seeds = new ArrayList<>(ANNOUNCEMENTS);
        addAnnouncementFixtures(seeds);
        List<AnnouncementEntity> announcements = seeds.stream()
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

    private static void addAnnouncementFixtures(List<AnnouncementSeed> seeds) {
        addAnnouncementFixtures(
                seeds,
                AnnouncementScope.SCHOOL,
                null,
                "admin",
                12,
                4,
                List.of(
                        "Hướng dẫn cập nhật thông tin tài khoản trước đợt đăng ký",
                        "Lịch nghỉ lễ và kế hoạch học tập tháng 10 năm 2026",
                        "Mở cổng đăng ký học phần bổ trợ học kỳ 1",
                        "Hội thảo kỹ năng xây dựng đề cương nghiên cứu",
                        "Quy định sử dụng email sinh viên trong trao đổi học vụ",
                        "Thông báo bảo trì cổng Topic Manager cuối tuần",
                        "Hướng dẫn nộp minh chứng hoạt động ngoại khóa",
                        "Khảo sát mức độ hài lòng về dịch vụ hỗ trợ sinh viên",
                        "Lịch tiếp nhận hồ sơ xác nhận sinh viên tháng 10",
                        "Nhắc hạn hoàn thành học phí học kỳ 1 năm học 2026-2027",
                        "Thông báo cấp giấy xác nhận sinh viên trực tuyến",
                        "Phát động cuộc thi ý tưởng đổi mới sáng tạo HCMUTE",
                        "Lịch tập huấn sử dụng hệ thống quản lý đề tài",
                        "Cập nhật quy định bảo vệ dữ liệu cá nhân trong học vụ",
                        "Thông báo hỗ trợ sinh viên có hoàn cảnh khó khăn",
                        "Kế hoạch kiểm tra an toàn phòng học và phòng thí nghiệm",
                        "Chương trình học bổng khuyến khích học tập học kỳ 1",
                        "Hướng dẫn tra cứu kết quả học tập trên Student Portal",
                        "Thông báo tổng vệ sinh khuôn viên trước năm học mới",
                        "Lịch trực tư vấn học vụ tại Trung tâm hỗ trợ sinh viên"),
                List.of(
                        "Sinh viên vui lòng kiểm tra họ tên, email, mã số và thông tin khoa trước khi gửi đăng ký đề tài. Các trường hợp sai thông tin cần báo Phòng Đào tạo để được hỗ trợ.",
                        "Nhà trường thông tin lịch nghỉ lễ, lịch học bù và các mốc học vụ trong tháng 10 để sinh viên chủ động sắp xếp kế hoạch học tập.",
                        "Cổng đăng ký học phần bổ trợ sẽ mở từ 08:00 ngày 01/10 đến 17:00 ngày 05/10. Sinh viên đăng nhập bằng tài khoản HCMUTE và kiểm tra lại kết quả sau khi đăng ký.",
                        "Buổi hội thảo giới thiệu cách xác định vấn đề, tìm tài liệu và xây dựng đề cương nghiên cứu sẽ diễn ra tại hội trường A vào 14:00 ngày 08/10.",
                        "Từ học kỳ này, các trao đổi liên quan đến học vụ và đề tài cần được thực hiện bằng email HCMUTE để bảo đảm khả năng xác thực và lưu vết xử lý.",
                        "Hệ thống Topic Manager sẽ tạm dừng từ 22:00 đến 23:30 ngày 12/10 để nâng cấp cơ sở dữ liệu. Vui lòng hoàn tất các thao tác trước thời gian bảo trì.",
                        "Sinh viên nộp minh chứng hoạt động ngoại khóa theo biểu mẫu mới trên Student Portal trước ngày 15/10. Hồ sơ thiếu thông tin sẽ được trả lại để bổ sung.",
                        "Nhà trường mời sinh viên tham gia khảo sát ngắn về chất lượng dịch vụ hỗ trợ. Ý kiến phản hồi sẽ được dùng để cải thiện quy trình tiếp nhận và giải quyết yêu cầu.",
                        "Trung tâm hỗ trợ sinh viên tiếp nhận hồ sơ xác nhận trực tiếp vào các buổi sáng trong tuần và trực tuyến qua Student Portal trong toàn bộ thời gian làm việc.",
                        "Sinh viên kiểm tra công nợ và hoàn tất học phí trước hạn quy định để không ảnh hưởng đến quyền đăng ký học phần, đăng ký đề tài và nhận kết quả học tập.",
                        "Giấy xác nhận sinh viên có thể được đăng ký trực tuyến, theo dõi trạng thái xử lý và tải bản điện tử sau khi được phê duyệt.",
                        "Cuộc thi khuyến khích các giải pháp công nghệ phục vụ học tập và đời sống campus. Hồ sơ ý tưởng được tiếp nhận đến hết ngày 20/10.",
                        "Buổi tập huấn dành cho giảng viên và cán bộ khoa sẽ hướng dẫn quy trình tạo topic, duyệt đăng ký, phân công evaluator và công bố kết quả.",
                        "Các đơn vị thực hiện thu thập và xử lý thông tin sinh viên đúng mục đích, giới hạn quyền truy cập và không chia sẻ dữ liệu cho bên ngoài khi chưa được phép.",
                        "Phòng Công tác sinh viên tiếp nhận hồ sơ đề nghị hỗ trợ từ ngày 05/10. Sinh viên chuẩn bị giấy tờ theo danh mục và nộp tại quầy Một cửa.",
                        "Các khoa phối hợp kiểm tra thiết bị, lối thoát hiểm và điều kiện an toàn tại phòng học, phòng máy trước khi bắt đầu đợt học mới.",
                        "Thông tin điều kiện, tiêu chí và thời hạn nộp hồ sơ học bổng được công bố trên Student Portal. Sinh viên cần hoàn tất minh chứng trước khi gửi.",
                        "Sinh viên có thể xem điểm, tiến độ học tập và các thông báo liên quan bằng cách đăng nhập Student Portal; nếu phát hiện sai lệch, vui lòng gửi yêu cầu hỗ trợ.",
                        "Các đơn vị và lớp học phối hợp tổng vệ sinh, sắp xếp bàn ghế và kiểm tra thiết bị trước ngày tựu trường theo lịch của nhà trường.",
                        "Trung tâm hỗ trợ sinh viên bố trí cán bộ tư vấn theo ca trong tuần đầu tháng 10 để giải đáp các vấn đề về đăng ký học phần và thủ tục học vụ."));

        addAnnouncementFixtures(
                seeds,
                AnnouncementScope.DEPARTMENT,
                "KT",
                "hoang.thai.xuan.khoa@lecturer.hcmute.edu.vn",
                8,
                4,
                List.of(
                        "KT: Lịch họp khoa tháng 10 năm 2026",
                        "KT: Phân công cố vấn học tập cho các lớp khóa 2024",
                        "KT: Hướng dẫn rà soát đề tài tốt nghiệp đợt 1",
                        "KT: Lịch seminar chuyên môn về phân tích báo cáo tài chính",
                        "KT: Nhắc cập nhật tiến độ đề tài trước cuộc họp khoa",
                        "KT: Tiếp nhận đề xuất đề tài nghiên cứu sinh viên",
                        "KT: Lịch phản biện và góp ý đề cương khóa luận",
                        "KT: Quy trình xin xác nhận số liệu phục vụ khóa luận",
                        "KT: Phân công trực tư vấn đăng ký đề tài",
                        "KT: Hướng dẫn chấm và nhập điểm đánh giá đề tài",
                        "KT: Họp xét tiến độ sinh viên thực hiện khóa luận",
                        "KT: Điều chỉnh lịch seminar do trùng lịch phòng",
                        "KT: Danh sách sinh viên cần bổ sung đề cương",
                        "KT: Lịch nghiệm thu khóa luận học kỳ 1",
                        "KT: Tổng hợp deadline hồ sơ đề tài tháng 10"),
                List.of(
                        "Khoa Kế toán tổ chức họp khoa lúc 09:00 ngày 03/10 tại phòng B.302. Nội dung gồm kế hoạch đề tài, lịch seminar và phân công công việc tháng 10.",
                        "Các giảng viên cố vấn kiểm tra danh sách lớp được phân công, cập nhật thông tin liên hệ và phản hồi các trường hợp cần hỗ trợ về khoa trước ngày 05/10.",
                        "Giảng viên hướng dẫn rà soát tên đề tài, mục tiêu, phương pháp và sản phẩm dự kiến của sinh viên trước khi xác nhận đề cương đợt 1.",
                        "Seminar chuyên môn diễn ra lúc 14:00 ngày 07/10 tại phòng B.204, tập trung vào cách đọc và phân tích báo cáo tài chính doanh nghiệp.",
                        "Các giảng viên cập nhật tiến độ hướng dẫn trên hệ thống trước 16:00 ngày 09/10 để khoa tổng hợp cho cuộc họp giao ban.",
                        "Khoa tiếp nhận đề xuất đề tài nghiên cứu sinh viên có liên quan đến kế toán quản trị, kiểm toán và chuyển đổi số trong nghiệp vụ tài chính.",
                        "Lịch phản biện đề cương được sắp xếp trong tuần thứ hai của tháng 10. Giảng viên phản biện xem hồ sơ trên Topic Manager trước buổi họp.",
                        "Giảng viên và sinh viên sử dụng biểu mẫu xác nhận số liệu của khoa khi cần khai thác báo cáo nội bộ phục vụ khóa luận.",
                        "Lịch trực tư vấn đăng ký đề tài được phân công theo buổi; giảng viên phụ trách cập nhật trạng thái hỗ trợ sau mỗi ca trực.",
                        "Khoa gửi hướng dẫn nhập điểm đánh giá trên hệ thống, bao gồm thang điểm, nhận xét bắt buộc và thời hạn hoàn tất.",
                        "Cuộc họp xét tiến độ dành cho sinh viên đang thực hiện khóa luận diễn ra ngày 18/10. Giảng viên hướng dẫn chuẩn bị nhận xét ngắn cho từng sinh viên.",
                        "Seminar ngày 21/10 chuyển sang phòng B.205 do phòng B.204 được sử dụng cho hoạt động khảo sát thiết bị.",
                        "Danh sách sinh viên cần bổ sung đề cương đã được cập nhật. Giảng viên hướng dẫn nhắc sinh viên hoàn thiện trước hạn khoa quy định.",
                        "Lịch nghiệm thu khóa luận học kỳ 1 dự kiến tổ chức từ ngày 26/10 đến ngày 30/10. Hội đồng và phòng bảo vệ sẽ được thông báo trong lịch chi tiết.",
                        "Các mốc nộp đề cương, biên bản phản biện và phiếu đánh giá tháng 10 được tổng hợp để giảng viên chủ động hoàn tất hồ sơ."));
    }

    private static void addAnnouncementFixtures(
            List<AnnouncementSeed> seeds,
            AnnouncementScope scope,
            String departmentCode,
            String authorLogin,
            int publishedCount,
            int draftCount,
            List<String> titles,
            List<String> contents) {
        if (titles.size() != contents.size()) {
            throw new IllegalArgumentException("Announcement fixture titles and contents must have the same size.");
        }
        for (int index = 0; index < titles.size(); index++) {
            AnnouncementStatus status = index < publishedCount
                    ? AnnouncementStatus.PUBLISHED
                    : index < publishedCount + draftCount ? AnnouncementStatus.DRAFT : AnnouncementStatus.HIDDEN;
            seeds.add(new AnnouncementSeed(
                    titles.get(index), contents.get(index), scope, departmentCode, authorLogin, status));
        }
    }

    private List<StudentGroupEntity> seedStudentGroups() {
        RegistrationPeriodEntity period = registrationPeriodRepository.findByNameIgnoreCase(SEEDED_PERIOD_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Seed registration periods before student groups."));

        List<StudentGroupEntity> groups = new ArrayList<>();
        STUDENT_GROUPS.stream()
                .map(seed -> upsertStudentGroup(period, seed))
                .forEach(groups::add);
        PERIOD_STUDENT_GROUPS.forEach(seed -> {
            RegistrationPeriodEntity samplePeriod = registrationPeriodRepository
                    .findByNameIgnoreCase(sampleRegistrationPeriodName(seed.periodIndex()))
                    .orElseThrow(() -> new IllegalStateException(
                            "Seed registration periods before student groups: " + seed.periodIndex()));
            groups.add(upsertStudentGroup(samplePeriod, new StudentGroupSeed(
                    seed.name(), seed.leaderLogin(), seed.memberLogins(), seed.status())));
        });
        return studentGroupRepository.saveAllAndFlush(groups);
    }

    private StudentGroupEntity upsertStudentGroup(RegistrationPeriodEntity period, StudentGroupSeed seed) {
        UserEntity leader = requireSeedStudent(seed.leaderLogin());
        StudentGroupEntity group = studentGroupRepository
                .findByRegistrationPeriod_IdAndNameIgnoreCase(period.getId(), seed.name())
                .orElseGet(() -> new StudentGroupEntity(seed.name(), period, leader, leader));
        group.setName(seed.name());
        group.setRegistrationPeriod(period);
        group.setCreatedBy(leader);
        group.setLeader(leader);
        group.setStatus(seed.status());
        group.getMembers().clear();
        group.getMembers().add(leader);
        seed.memberLogins().stream()
                .map(this::requireSeedStudent)
                .forEach(group.getMembers()::add);
        if (group.getMembers().size() > 3) {
            throw new IllegalStateException("Student group seed exceeds the maximum size: " + seed.name());
        }
        return group;
    }

    private static String sampleRegistrationPeriodName(int index) {
        return String.format(Locale.ROOT, "Đợt đăng ký mẫu %02d - năm học 2025-2026", index);
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

    private record StudentGroupSeed(
            String name, String leaderLogin, List<String> memberLogins, GroupStatus status) {
    }

    private record PeriodStudentGroupSeed(
            int periodIndex, String name, String leaderLogin, List<String> memberLogins, GroupStatus status) {
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
