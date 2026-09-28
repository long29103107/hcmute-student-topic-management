package com.hcmute.topicmanagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
public class HcmuteStudentTopicManagementApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(HcmuteStudentTopicManagementApplication.class);

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(HcmuteStudentTopicManagementApplication.class);
        application.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {
            ConfigurableEnvironment environment = event.getEnvironment();
            LOGGER.info("spring.datasource.url={}", environment.getProperty("spring.datasource.url"));
            LOGGER.info("spring.datasource.username={}", environment.getProperty("spring.datasource.username"));
            LOGGER.info("spring.datasource.password={}", environment.getProperty("spring.datasource.password"));
        });
        application.run(args);
    }

}
