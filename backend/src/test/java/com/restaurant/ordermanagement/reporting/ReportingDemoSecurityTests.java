package com.restaurant.ordermanagement.reporting;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "TV5_DEMO_PASSWORD=TEST_ONLY_PASSWORD",
        "spring.datasource.url=jdbc:h2:mem:tv5-demo-security;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.sql.init.mode=never"
})
@AutoConfigureMockMvc
@ActiveProfiles({"test", "tv5-demo"})
class ReportingDemoSecurityTests {
    @Autowired MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings={"MANAGER","WAITER","CHEF","CASHIER"})
    void profileScopedDemoUsersAuthenticateThroughTheActualPasswordEncoder(String role) throws Exception {
        mvc.perform(formLogin().user("demo_" + role.toLowerCase(java.util.Locale.ROOT)).password("TEST_ONLY_PASSWORD"))
                .andExpect(status().isFound()).andExpect(authenticated().withRoles(role))
                .andExpect(redirectedUrl("/manager/reports"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(formLogin().user("demo_manager").password("TEST_WRONG_PASSWORD"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void anonymousUsersAreSentToTheDemoLoginAndPublicAssetsRemainAvailable() throws Exception {
        mvc.perform(get("/manager/reports")).andExpect(status().isFound());
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/fonts/inter-regular.ttf")).andExpect(status().isOk());
        mvc.perform(get("/images/tv5/search.svg")).andExpect(status().isOk());
    }
}
