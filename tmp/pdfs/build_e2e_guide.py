from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate, Frame, PageTemplate, Paragraph, Spacer, Table, TableStyle,
    PageBreak, KeepTogether
)


ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "output" / "pdf" / "e2e-test-department-faculty-head-lecturer-student.pdf"
OUT.parent.mkdir(parents=True, exist_ok=True)

font_candidates = [
    Path("C:/Windows/Fonts/arial.ttf"),
    Path("C:/Windows/Fonts/segoeui.ttf"),
]
font_path = next((p for p in font_candidates if p.exists()), None)
if font_path:
    pdfmetrics.registerFont(TTFont("GuideSans", str(font_path)))
    pdfmetrics.registerFont(TTFont("GuideSans-Bold", str(font_path)))
    FONT = "GuideSans"
    BOLD = "GuideSans-Bold"
else:
    FONT = "Helvetica"
    BOLD = "Helvetica-Bold"

PAGE_W, PAGE_H = A4
BLUE = colors.HexColor("#155EEF")
NAVY = colors.HexColor("#102A43")
INK = colors.HexColor("#243B53")
MUTED = colors.HexColor("#627D98")
LIGHT = colors.HexColor("#F0F4F8")
GREEN = colors.HexColor("#067647")
ORANGE = colors.HexColor("#B54708")

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="TitleVN", parent=styles["Title"], fontName=BOLD, fontSize=24, leading=29, textColor=NAVY, alignment=TA_CENTER, spaceAfter=8))
styles.add(ParagraphStyle(name="SubtitleVN", parent=styles["Normal"], fontName=FONT, fontSize=11, leading=16, textColor=MUTED, alignment=TA_CENTER, spaceAfter=18))
styles.add(ParagraphStyle(name="H1VN", parent=styles["Heading1"], fontName=BOLD, fontSize=17, leading=22, textColor=NAVY, spaceBefore=8, spaceAfter=9))
styles.add(ParagraphStyle(name="H2VN", parent=styles["Heading2"], fontName=BOLD, fontSize=12.5, leading=17, textColor=BLUE, spaceBefore=8, spaceAfter=5))
styles.add(ParagraphStyle(name="BodyVN", parent=styles["BodyText"], fontName=FONT, fontSize=9.5, leading=14, textColor=INK, spaceAfter=5))
styles.add(ParagraphStyle(name="SmallVN", parent=styles["BodyText"], fontName=FONT, fontSize=8.2, leading=11, textColor=MUTED, spaceAfter=3))
styles.add(ParagraphStyle(name="StepVN", parent=styles["BodyText"], fontName=FONT, fontSize=9.2, leading=13.5, textColor=INK, leftIndent=4, spaceAfter=4))
styles.add(ParagraphStyle(name="CheckVN", parent=styles["BodyText"], fontName=FONT, fontSize=9, leading=13, textColor=INK, leftIndent=5, spaceAfter=3))
styles.add(ParagraphStyle(name="CalloutVN", parent=styles["BodyText"], fontName=FONT, fontSize=9, leading=13, textColor=ORANGE, backColor=colors.HexColor("#FFF7ED"), borderColor=colors.HexColor("#FED7AA"), borderWidth=0.7, borderPadding=7, spaceBefore=5, spaceAfter=8))
styles.add(ParagraphStyle(name="TableHeaderVN", parent=styles["BodyText"], fontName=BOLD, fontSize=8.2, leading=11, textColor=colors.white, spaceAfter=0))


def P(text, style="BodyVN"):
    return Paragraph(text, styles[style])


def bullet(text):
    return P("☐ " + text, "CheckVN")


def numbered(n, text):
    return P(f"<b>{n}.</b> {text}", "StepVN")


def table(data, widths, header=True):
    converted = []
    for row_index, row in enumerate(data):
        converted.append([cell if isinstance(cell, Paragraph) else P(str(cell), "TableHeaderVN" if header and row_index == 0 else "SmallVN") for cell in row])
    t = Table(converted, colWidths=widths, repeatRows=1 if header else 0, hAlign="LEFT")
    cmds = [
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#CBD5E1")),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]
    if header:
        cmds += [("BACKGROUND", (0, 0), (-1, 0), NAVY), ("TEXTCOLOR", (0, 0), (-1, 0), colors.white)]
        for c in range(len(data[0])):
            cmds.append(("FONTNAME", (c, 0), (c, 0), BOLD))
    for r in range(1 if header else 0, len(data)):
        if r % 2 == 0:
            cmds.append(("BACKGROUND", (0, r), (-1, r), colors.HexColor("#F8FAFC")))
    t.setStyle(TableStyle(cmds))
    return t


def header_footer(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(colors.HexColor("#D9E2EC"))
    canvas.line(17 * mm, 14 * mm, PAGE_W - 17 * mm, 14 * mm)
    canvas.setFont(FONT, 7.5)
    canvas.setFillColor(MUTED)
    canvas.drawString(17 * mm, 9 * mm, "HCMUTE Student Topic Management - E2E manual test")
    canvas.drawRightString(PAGE_W - 17 * mm, 9 * mm, f"Trang {doc.page}")
    canvas.restoreState()


class GuideDoc(BaseDocTemplate):
    def __init__(self, filename):
        super().__init__(filename, pagesize=A4, leftMargin=17 * mm, rightMargin=17 * mm, topMargin=15 * mm, bottomMargin=19 * mm, title="E2E test guide")
        frame = Frame(self.leftMargin, self.bottomMargin, self.width, self.height, id="normal")
        self.addPageTemplates([PageTemplate(id="guide", frames=frame, onPage=header_footer)])


story = []
story += [Spacer(1, 22 * mm), P("E2E TEST GUIDE", "SmallVN"), P("Tạo Department mới và chạy xuyên suốt flow Faculty Head - Lecturer - Student", "TitleVN"), P("Bản hướng dẫn thao tác thủ công trên local app, bám theo route và permission hiện tại của project.", "SubtitleVN")]
story += [table([
    ["Mục tiêu", "Sau khi hoàn thành, có 1 department mới, 1 trưởng khoa, 1 lecturer, 1 student và một topic đi từ tạo đến publish kết quả."],
    ["Actor", "ADMIN -> FACULTY_HEAD -> LECTURER -> STUDENT -> FACULTY_HEAD -> LECTURER -> FACULTY_HEAD -> STUDENT"],
    ["Base URL", "http://localhost:5000"],
    ["Tài khoản bắt đầu", "admin@hcmute.edu.vn / admin123 (local only)"],
], [31 * mm, 135 * mm]), Spacer(1, 8)]
story += [P("Cách dùng", "H2VN"), P("Mỗi ô ☐ là một điểm cần tick sau khi làm xong. Nếu một bước fail, ghi lại URL, actor đang đăng nhập, payload/giá trị đã nhập và message màu đỏ trên màn hình.", "BodyVN"), P("Lưu ý an toàn: không dùng mật khẩu local hoặc bật public seed trên staging/production. Bước reset seed/DDL có thể xoá dữ liệu.", "CalloutVN"), PageBreak()]

story += [P("0. Chuẩn bị môi trường", "H1VN"), P("Chạy từ thư mục root của repo.", "BodyVN")]
for n, text in enumerate([
    "Đảm bảo MySQL đang chạy và database hcmute_topic_management đã được tạo theo SETUP_GUIDE.md.",
    "Mở PowerShell, chạy ứng dụng bằng <font name='GuideSans-Bold'>mvn spring-boot:run</font> hoặc <font name='GuideSans-Bold'>docker compose up --build</font>.",
    "Mở http://localhost:5000/seed. Nếu database mới, chạy pipeline theo thứ tự trên trang để có schema, role, permission và tài khoản admin local.",
    "Đăng nhập bằng admin@hcmute.edu.vn / admin123. Không gọi POST /api/admin/seed nếu database đang có dữ liệu cần giữ.",
], 1): story.append(numbered(n, text))
story += [P("Expected", "H2VN"), bullet("Trang /dashboard mở được với role ADMIN."), bullet("Có thể truy cập /admin/departments, /admin/lecturers và /admin/students."), PageBreak()]

story += [P("1. ADMIN - tạo department mới", "H1VN"), P("Dùng một mã chưa tồn tại để không đụng dữ liệu seed. Ví dụ: E2EQA và E2E Quality Assurance.", "BodyVN")]
for n, text in enumerate([
    "Vào /admin/departments.",
    "Bấm Create department.",
    "Nhập Department code: <b>E2EQA</b>. Chỉ dùng chữ, số, dấu gạch ngang hoặc gạch dưới.",
    "Nhập Department name: <b>E2E Quality Assurance</b>.",
    "Bấm Create department.",
], 1): story.append(numbered(n, text))
story += [P("Expected", "H2VN"), bullet("Có flash message tạo thành công."), bullet("Dòng E2EQA xuất hiện trong bảng, status Active."), bullet("Nếu refresh vẫn thấy dòng và không tạo thêm bản ghi trùng."), P("Negative check", "H2VN"), bullet("Thử tạo lại code E2EQA: hệ thống phải báo code đã tồn tại và không tạo duplicate."), PageBreak()]

story += [P("2. ADMIN - tạo tài khoản trưởng khoa", "H1VN"), P("UI hiện tạo Faculty Head từ directory lecturer. Account mới ban đầu được tạo với role LECTURER; sau đó edit để gán role FACULTY_HEAD.", "CalloutVN")]
for n, text in enumerate([
    "Vào /admin/lecturers và bấm Add lecturer.",
    "Full name: <b>E2E Faculty Head</b>. Email/Login identifier: <b>e2e.head@lecturer.hcmute.edu.vn</b>. Department: chọn <b>E2EQA</b>.",
    "Bấm Create account. Tìm đúng account vừa tạo.",
    "Dùng action Edit trên dòng account. Trong System roles, chọn <b>FACULTY_HEAD</b> (giữ role LECTURER nếu form đang giữ role này), rồi Save.",
    "Dùng action Set password/Password trên account, đặt mật khẩu test tối thiểu 8 ký tự, ví dụ <b>E2eHead@123</b>.",
    "Logout admin, login bằng e2e.head@lecturer.hcmute.edu.vn / E2eHead@123.",
], 1): story.append(numbered(n, text))
story += [P("Expected", "H2VN"), bullet("Dashboard hiển thị Faculty Head và scope E2EQA."), bullet("/faculty/departments hiển thị department của mình; danh sách hiện chưa có hoặc mới có member admin tùy policy."), bullet("/admin/departments không truy cập được nếu permission không cấp cho Faculty Head."), PageBreak()]

story += [P("3. ADMIN - tạo lecturer và student", "H1VN")]
story += [P("3A. Lecturer", "H2VN")]
for n, text in enumerate([
    "Đăng nhập lại bằng admin, vào /admin/lecturers -> Add lecturer.",
    "Full name: <b>E2E Lecturer</b>; Email: <b>e2e.lecturer@lecturer.hcmute.edu.vn</b>; Department: <b>E2EQA</b>.",
    "Create account, sau đó Set password = <b>E2eLecturer@123</b>.",
], 1): story.append(numbered(n, text))
story += [P("3B. Student", "H2VN")]
for n, text in enumerate([
    "Vào /admin/students -> Add student.",
    "Student code (MSSV): <b>26990001</b>; Full name: <b>E2E Student</b>; Department: <b>E2EQA</b>.",
    "Create account. Hệ thống tự sinh login email: <b>26990001@student.hcmute.edu.vn</b>.",
    "Set password = <b>E2eStudent@123</b>.",
], 1): story.append(numbered(n, text))
story += [P("Expected", "H2VN"), bullet("Admin directory có đủ 3 account mới, tất cả Active và cùng department E2EQA."), bullet("Faculty Head login lại và /faculty/departments thấy E2E Lecturer, E2E Student."), bullet("Lecturer login được bằng email; Student login được bằng email sinh từ MSSV."), PageBreak()]

story += [P("4. FACULTY_HEAD - tạo registration period", "H1VN"), P("Topic và student registration bị chặn bởi time window. Để test ngay, đặt các window bao phủ thời điểm hiện tại.", "BodyVN")]
for n, text in enumerate([
    "Login bằng e2e.head@lecturer.hcmute.edu.vn.",
    "Vào /faculty/periods -> Create registration period.",
    "Name: <b>E2E Period</b>; Type: <b>COURSE</b>; Status: <b>OPEN</b>.",
    "Lecturer window: start hôm qua, end ngày mai. Student window: start hôm qua, end ngày mai. Các mốc phải đúng thứ tự.",
    "Save/Create period.",
], 1): story.append(numbered(n, text))
story += [P("Expected", "H2VN"), bullet("Period xuất hiện với status OPEN."), bullet("Lecturer và Student registration windows hiển thị đang mở."), PageBreak()]

story += [P("5. LECTURER - tạo và submit topic", "H1VN")]
for n, text in enumerate([
    "Logout, login bằng e2e.lecturer@lecturer.hcmute.edu.vn / E2eLecturer@123.",
    "Vào /lecturer/topics -> Create topic/proposal.",
    "Chọn period E2E Period và department E2EQA. Điền title <b>E2E Topic - Student Portal</b> và các field bắt buộc còn lại.",
    "Save để topic ở trạng thái DRAFT.",
    "Mở lại topic, kiểm tra thông tin, bấm Submit for review. Trạng thái phải chuyển PENDING_APPROVAL.",
], 1): story.append(numbered(n, text))
story += [P("Expected", "H2VN"), bullet("Topic hiển thị trong danh sách của lecturer với trạng thái PENDING_APPROVAL."), bullet("Lecturer không tự approve topic của chính mình."), PageBreak()]

story += [P("6. FACULTY_HEAD - review, assign supervisor và publish topic", "H1VN")]
for n, text in enumerate([
    "Login lại bằng Faculty Head. Vào /faculty/topics/review, tìm E2E Topic - Student Portal.",
    "Mở review, chọn APPROVE. Kiểm tra topic chuyển APPROVED.",
    "Vào /faculty/topics/supervisors, mở supervisor assignment của topic, chọn E2E Lecturer rồi Save.",
    "Vào /faculty/topics/publish, tìm topic APPROVED và bấm Publish.",
], 1): story.append(numbered(n, text))
story += [P("Expected", "H2VN"), bullet("Topic sau cùng có trạng thái PUBLISHED."), bullet("Supervisor là E2E Lecturer và cùng department E2EQA."), bullet("Student có thể thấy topic khi period OPEN và student window đang mở."), P("Negative checks", "H2VN"), bullet("Không assign student làm supervisor."), bullet("Không assign lecturer ở department khác."), PageBreak()]

story += [P("7. STUDENT - tạo group và đăng ký topic", "H1VN")]
for n, text in enumerate([
    "Logout, login bằng 26990001@student.hcmute.edu.vn / E2eStudent@123.",
    "Vào /student/groups, tạo group mới trong E2E Period. Đặt tên <b>E2E Group</b> và giữ student hiện tại làm leader.",
    "Vào /student/topic-registration hoặc màn hình Register topic.",
    "Chọn E2E Topic - Student Portal, chọn E2E Group, submit registration.",
], 1): story.append(numbered(n, text))
story += [P("Expected", "H2VN"), bullet("Group có đúng leader là E2E Student."), bullet("Registration xuất hiện trạng thái PENDING."), bullet("Nếu thử submit khi hết window hoặc topic chưa PUBLISHED, server phải từ chối."), PageBreak()]

story += [P("8. FACULTY_HEAD - approve registration và tạo board/evaluator", "H1VN")]
for n, text in enumerate([
    "Login Faculty Head, vào /faculty/registration-review.",
    "Tìm E2E Group / E2E Topic, kiểm tra department và leader, rồi Approve.",
    "Vào /faculty/evaluator-assignments, chọn registration đã approved, assign E2E Lecturer làm evaluator. Không chọn supervisor của topic nếu UI/service chặn trường hợp này.",
    "Nếu project dùng review board flow: vào /faculty/boards, Create review board cho registration; thêm các member hợp lệ theo yêu cầu 3-5 người, đúng chair và secretary. Với smoke test 1 lecturer, dùng evaluator flow nếu board yêu cầu tối thiểu 3 member.",
], 1): story.append(numbered(n, text))
story += [P("Expected", "H2VN"), bullet("Registration chuyển APPROVED."), bullet("Evaluator assignment tồn tại và thuộc đúng department."), bullet("Supervisor không được làm evaluator của chính topic đó."), PageBreak()]

story += [P("9. LECTURER - chấm điểm", "H1VN")]
for n, text in enumerate([
    "Login bằng E2E Lecturer.",
    "Vào /lecturer/scoring.",
    "Mở E2E registration, nhập score hợp lệ trong khoảng 0-10, tối đa 2 chữ số thập phân; ví dụ <b>8.50</b>. Nhập comment nếu cần.",
    "Submit score. Refresh trang để kiểm tra score vẫn được lưu và evaluation ở trạng thái SUBMITTED/PUBLISHED theo flow.",
], 1): story.append(numbered(n, text))
story += [P("Expected", "H2VN"), bullet("Lecturer chỉ thấy registration được assign cho mình."), bullet("Điểm 11, -1 hoặc quá 2 decimals bị từ chối."), PageBreak()]

story += [P("10. FACULTY_HEAD - publish result và STUDENT verify", "H1VN")]
for n, text in enumerate([
    "Login Faculty Head, vào /faculty/results.",
    "Mở E2E registration, kiểm tra đủ evaluation score, sau đó Publish result.",
    "Đăng xuất, login bằng E2E Student, vào /student/results.",
    "Mở kết quả của E2E Group và kiểm tra average score, status PUBLISHED, topic và group đúng.",
], 1): story.append(numbered(n, text))
story += [P("Expected", "H2VN"), bullet("Faculty Head thấy result PUBLISHED và audit publisher/time."), bullet("Student thấy đúng result của group mình."), bullet("Student khác không thấy result của E2E Group."), P("Smoke test completed", "H2VN"), bullet("Department tạo được và active."), bullet("Faculty Head scope đúng department."), bullet("Lecturer tạo/submit topic, được assign supervisor/evaluator."), bullet("Student tạo group, register topic, xem result sau publish."), PageBreak()]

story += [P("11. Bảng dữ liệu test để copy", "H1VN"), table([
    ["Item", "Value"],
    ["Department", "E2EQA / E2E Quality Assurance"],
    ["Faculty Head", "e2e.head@lecturer.hcmute.edu.vn / E2eHead@123"],
    ["Lecturer", "e2e.lecturer@lecturer.hcmute.edu.vn / E2eLecturer@123"],
    ["Student", "26990001@student.hcmute.edu.vn / E2eStudent@123"],
    ["Period", "E2E Period"],
    ["Topic", "E2E Topic - Student Portal"],
    ["Group", "E2E Group"],
], [36 * mm, 130 * mm]), Spacer(1, 8), P("12. Lỗi thường gặp", "H1VN"), table([
    ["Hiện tượng", "Cách kiểm tra"],
    ["403", "Đúng actor chưa? Kiểm tra role/permission và logout-login lại sau khi đổi role."],
    ["Không thấy department trong select", "Department phải Active; refresh trang sau khi tạo."],
    ["Không login được account mới", "Account mới không có password mặc định. Dùng action Set password."],
    ["Topic không submit được", "Period phải OPEN và lecturer window phải bao phủ thời điểm hiện tại."],
    ["Student không thấy topic", "Topic phải PUBLISHED, period OPEN và student window đang mở."],
    ["Không approve được registration", "Registration phải PENDING, đúng department và group leader hợp lệ."],
    ["Không publish result được", "Mọi evaluator bắt buộc phải có score hợp lệ trước khi publish."],
], [45 * mm, 121 * mm]), Spacer(1, 8), P("Nguồn đối chiếu trong repo", "H2VN"), P("README.md, SETUP_GUIDE.md, docs/ui-route-map.md, docs/workflows.md và các controller/template dưới src/main.", "SmallVN")]

GuideDoc(str(OUT)).build(story)
print(OUT)
