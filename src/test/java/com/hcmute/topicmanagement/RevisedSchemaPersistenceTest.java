package com.hcmute.topicmanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.hcmute.topicmanagement.model.DepartmentEntity;
import com.hcmute.topicmanagement.model.EvaluationEntity;
import com.hcmute.topicmanagement.model.RegistrationPeriodEntity;
import com.hcmute.topicmanagement.model.RegistrationResultEntity;
import com.hcmute.topicmanagement.model.ReviewBoardEntity;
import com.hcmute.topicmanagement.model.ReviewBoardMemberEntity;
import com.hcmute.topicmanagement.model.StudentGroupEntity;
import com.hcmute.topicmanagement.model.TopicEntity;
import com.hcmute.topicmanagement.model.TopicRegistrationEntity;
import com.hcmute.topicmanagement.model.UserEntity;
import com.hcmute.topicmanagement.repository.DepartmentRepository;
import com.hcmute.topicmanagement.repository.EvaluationRepository;
import com.hcmute.topicmanagement.repository.RegistrationPeriodRepository;
import com.hcmute.topicmanagement.repository.RegistrationResultRepository;
import com.hcmute.topicmanagement.repository.ReviewBoardMemberRepository;
import com.hcmute.topicmanagement.repository.ReviewBoardRepository;
import com.hcmute.topicmanagement.repository.StudentGroupRepository;
import com.hcmute.topicmanagement.repository.TopicRegistrationRepository;
import com.hcmute.topicmanagement.repository.TopicRepository;
import com.hcmute.topicmanagement.repository.UserRepository;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:revisedschemapersistence;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Transactional
class RevisedSchemaPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RegistrationPeriodRepository registrationPeriodRepository;

    @Autowired
    private StudentGroupRepository studentGroupRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private TopicRegistrationRepository topicRegistrationRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private ReviewBoardRepository reviewBoardRepository;

    @Autowired
    private ReviewBoardMemberRepository reviewBoardMemberRepository;

    @Autowired
    private RegistrationResultRepository registrationResultRepository;

    @Test
    void persistsPeriodScopedGroupsMultipleEvaluationsAndSharedKeyResult() {
        String suffix = UUID.randomUUID().toString();
        UserEntity leader = userRepository.saveAndFlush(user("leader-" + suffix));
        UserEntity lecturer = userRepository.saveAndFlush(user("lecturer-" + suffix));
        UserEntity reviewer = userRepository.saveAndFlush(user("reviewer-" + suffix));

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        RegistrationPeriodEntity period = registrationPeriodRepository.saveAndFlush(
                new RegistrationPeriodEntity("Period " + suffix,
                        com.hcmute.topicmanagement.model.enums.PeriodType.COURSE,
                        start, end, start, end));
        DepartmentEntity department = departmentRepository.saveAndFlush(
                new DepartmentEntity("D-" + suffix.substring(0, 8), "Department " + suffix));
        StudentGroupEntity group = studentGroupRepository.saveAndFlush(
                new StudentGroupEntity("Group " + suffix, period, leader, leader));
        TopicEntity topic = topicRepository.saveAndFlush(
                new TopicEntity(period, department, lecturer, "Topic " + suffix, "Description"));
        TopicRegistrationEntity registration = topicRegistrationRepository.saveAndFlush(
                new TopicRegistrationEntity(group, topic, period, leader));

        EvaluationEntity first = new EvaluationEntity(registration, lecturer);
        first.setScore(new BigDecimal("8.00"));
        EvaluationEntity second = new EvaluationEntity(registration, reviewer);
        second.setScore(new BigDecimal("9.00"));
        evaluationRepository.saveAllAndFlush(java.util.List.of(first, second));

        ReviewBoardEntity board = reviewBoardRepository.saveAndFlush(new ReviewBoardEntity(registration, lecturer));
        reviewBoardMemberRepository.saveAndFlush(new ReviewBoardMemberEntity(board, lecturer));
        first.setBoard(board);
        evaluationRepository.saveAndFlush(first);

        RegistrationResultEntity result = new RegistrationResultEntity(registration);
        result.setAverageScore(new BigDecimal("8.50"));
        registrationResultRepository.saveAndFlush(result);

        assertEquals(period.getId(), group.getRegistrationPeriod().getId());
        assertEquals(2, evaluationRepository.findByTopicRegistration_IdOrderByCreatedAtAsc(registration.getId()).size());
        assertEquals(board.getId(), evaluationRepository
                .findByTopicRegistration_IdAndLecturer_Id(registration.getId(), lecturer.getId())
                .orElseThrow().getBoard().getId());
        assertNotNull(registrationResultRepository.findById(registration.getId()).orElseThrow().getId());
    }

    private static UserEntity user(String loginIdentifier) {
        UserEntity user = new UserEntity(loginIdentifier, "Test User", "test-password-hash");
        user.setEmailOrCode(loginIdentifier + "@example.test");
        return user;
    }
}
