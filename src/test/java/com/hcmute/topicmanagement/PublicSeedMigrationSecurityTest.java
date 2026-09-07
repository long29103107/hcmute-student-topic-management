package com.hcmute.topicmanagement;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "seed.public-enabled=true")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PublicSeedMigrationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousUserCanOpenSeedPageAndRunDdlWhenPublicMigrationIsEnabled() throws Exception {
        mockMvc.perform(get("/seed"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/seed/ddl").with(csrf()))
                .andExpect(status().isOk());
    }
}
