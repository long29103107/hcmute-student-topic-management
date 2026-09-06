# Department profile — CNTT

> Snapshot of the local seed dataset for department `CNTT` (`Công nghệ thông tin`).
> Generated on 2026-09-06 from `DatabaseSeedService`.
>
> This report does not include password hashes or credentials. The project was
> not connected to a running MySQL instance when this file was generated, so
> rows created manually after seeding (registrations, evaluations, reports and
> results) cannot be asserted here. The sections below describe the reproducible
> local seed baseline.

## 1. Department

| Field | Value |
|---|---|
| Code | `CNTT` |
| Name | Công nghệ thông tin |
| Status | Active |
| Seed source | `src/main/java/com/hcmute/topicmanagement/service/DatabaseSeedService.java` |

## 2. Summary

| Item | Count |
|---|---:|
| Faculty Head | 1 |
| Lecturers | 4 |
| Students | 13 |
| Total department accounts | 18 |
| Seeded topics | 2 |
| Topic-supervisor assignments | 3 |
| Active student groups | 2 |
| Students in seeded groups | 6 |
| Seeded topic registrations | 0 |
| Seeded evaluations | 0 |
| Seeded reports | 0 |
| Seeded registration results | 0 |

## 3. Faculty Head

| Full name | Login identifier | Email |
|---|---|---|
| PGS. TS. Nguyễn Văn Khang | `nguyen.van.khang` | `nguyen.van.khang@lecturer.hcmute.edu.vn` |

Role: `FACULTY_HEAD`.

The Faculty Head inherits lecturer capabilities and receives the faculty
workflow permissions for registration-period management, topic-supervisor
management, topic review/publication and topic-registration review.

## 4. Lecturers

| Full name | Login identifier | Email |
|---|---|---|
| Nguyễn Thanh Bình | `nguyen.thanh.binh` | `nguyen.thanh.binh@lecturer.hcmute.edu.vn` |
| Võ Hoàng Nam | `vo.hoang.nam` | `vo.hoang.nam@lecturer.hcmute.edu.vn` |
| Đặng Minh Trí | `dang.minh.tri` | `dang.minh.tri@lecturer.hcmute.edu.vn` |
| Bùi Thanh Hà | `bui.thanh.ha` | `bui.thanh.ha@lecturer.hcmute.edu.vn` |

Role: `LECTURER`.

Seeded lecturer capabilities are:

- Propose and edit own topics.
- View published topics.
- View relationship-authorized reports.
- Submit assigned evaluations.
- View permitted published results.

## 5. Students

| Full name | Student code / login | Email |
|---|---|---|
| Nguyễn Minh Anh | `24110000` | `24110000@student.hcmute.edu.vn` |
| Trần Hoàng Nam | `24110001` | `24110001@student.hcmute.edu.vn` |
| Lê Gia Hân | `24110002` | `24110002@student.hcmute.edu.vn` |
| Phạm Đức Anh | `24110003` | `24110003@student.hcmute.edu.vn` |
| Võ Thanh Tùng | `24110004` | `24110004@student.hcmute.edu.vn` |
| Đặng Ngọc Mai | `24110005` | `24110005@student.hcmute.edu.vn` |
| Bùi Quang Huy | `24110006` | `24110006@student.hcmute.edu.vn` |
| Nguyễn Khánh Linh | `24110007` | `24110007@student.hcmute.edu.vn` |
| Hồ Minh Khoa | `24110008` | `24110008@student.hcmute.edu.vn` |
| Phan Thùy Dương | `24110009` | `24110009@student.hcmute.edu.vn` |
| Huỳnh Quốc Bảo | `24110010` | `24110010@student.hcmute.edu.vn` |
| Trương Nhật Minh | `24110011` | `24110011@student.hcmute.edu.vn` |
| Lý Hải Yến | `24110012` | `24110012@student.hcmute.edu.vn` |

Role: `STUDENT`.

Seeded student capabilities are:

- View published topics.
- Create and manage student groups.
- Submit a topic registration as the current group leader.
- Submit and view/download relationship-authorized reports.
- View published results for the student's groups.

## 6. Registration period

The CNTT topics and groups are attached to the same seeded period:

| Field | Value |
|---|---|
| Name | Đợt đăng ký đề tài học kỳ 1 năm học 2026-2027 |
| Type | `COURSE` |
| Status | `OPEN` |
| Lecturer registration window | Seed time minus 14 days through seed time plus 14 days |
| Student registration window | Seed time minus 7 days through seed time plus 30 days |
| Reviewer score deadline | Not configured in seed |
| Council report date | Not configured in seed |
| Created by | `admin` / System Administrator |

## 7. Topics

### 7.1 Nền tảng quản lý đề tài và tiến độ khóa luận

| Field | Value |
|---|---|
| Department | `CNTT` · Công nghệ thông tin |
| Proposed by | Nguyễn Thanh Bình (`nguyen.thanh.binh@lecturer.hcmute.edu.vn`) |
| Status | `PENDING_APPROVAL` |
| Registration period | Đợt đăng ký đề tài học kỳ 1 năm học 2026-2027 |
| Supervisors | PGS. TS. Nguyễn Văn Khang; Võ Hoàng Nam |

Description:

> Xây dựng nền tảng theo dõi vòng đời đề tài, tiến độ thực hiện và các mốc
> nghiệm thu cho sinh viên.

### 7.2 Phân tích dữ liệu học tập bằng dashboard tương tác

| Field | Value |
|---|---|
| Department | `CNTT` · Công nghệ thông tin |
| Proposed by | Bùi Thanh Hà (`bui.thanh.ha@lecturer.hcmute.edu.vn`) |
| Status | `DRAFT` |
| Registration period | Đợt đăng ký đề tài học kỳ 1 năm học 2026-2027 |
| Supervisors | Đặng Minh Trí |

Description:

> Thiết kế dashboard giúp cố vấn nhận diện xu hướng học tập và hỗ trợ sinh viên
> theo dữ liệu thực tế.

## 8. Student groups

### Nhóm Phoenix

| Role | Student |
|---|---|
| Status | `ACTIVE` |
| Leader | Nguyễn Minh Anh (`24110000`) |
| Member | Trần Hoàng Nam (`24110001`) |
| Member | Lê Gia Hân (`24110002`) |

### Nhóm Orion

| Role | Student |
|---|---|
| Status | `ACTIVE` |
| Leader | Phạm Đức Anh (`24110003`) |
| Member | Võ Thanh Tùng (`24110004`) |
| Member | Đặng Ngọc Mai (`24110005`) |

## 9. Workflow rows not included in the seed baseline

`DatabaseSeedService.resetAndSeed()` resets and creates departments, users,
roles, permissions, registration periods, groups and topics. It does not create
the following department-scoped activity rows:

- Topic registrations.
- Evaluator assignments / evaluations.
- Uploaded reports.
- Registration results.

Therefore, their seeded baseline is zero. If the local application has been used
after the seed pipeline ran, query the live database before treating these
counts as current operational data.

## 10. Relevant application routes

| Route | Purpose |
|---|---|
| `/faculty/departments` | Faculty Head view of department members |
| `/faculty/topics/review` | Review CNTT topic proposals |
| `/faculty/topics/publish` | Publish approved topics |
| `/faculty/topics/supervisors` | Manage topic supervisors |
| `/faculty/registrations/review` | Review CNTT student registrations |
| `/faculty/registrations/evaluators` | Assign same-department evaluators |
| `/faculty/results` | Publish completed results |
| `/dashboard` | Role-specific dashboard; Faculty Head dashboard is department-scoped |

## 11. Scope note

The report is a department-focused seed snapshot, not an export of credentials
or a live database dump. The source of truth for the reproducible baseline is
`DatabaseSeedService`; current activity data should be exported from MySQL once
the local database service is running.
