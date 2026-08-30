package com.hcmute.topicmanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.hcmute.topicmanagement.security.DatabaseUserDetailsService;
import com.hcmute.topicmanagement.security.GoogleOAuth2UserService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationProvider authenticationProvider(
            DatabaseUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider,
            GoogleOAuth2UserService googleOAuth2UserService,
            @Value("${google.oauth.enabled:false}") boolean googleOAuthEnabled,
            @Value("${security.remember-me.enabled:false}") boolean rememberMeEnabled,
            @Value("${security.remember-me.key:}") String rememberMeKey,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookies,
            @Value("${security.remember-me.token-validity-seconds:1209600}") int rememberMeValiditySeconds,
            @Value("${seed.public-enabled:false}") boolean publicSeedEnabled)
            throws Exception {
        http
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/", "/dashboard").authenticated();
                    auth.requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll();
                    auth.requestMatchers("/login", "/forgot-password", "/access-denied").permitAll();
                    auth.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll();
                    if (publicSeedEnabled) {
                        auth.requestMatchers("/seed", "/api/seed/**").permitAll();
                    } else {
                        auth.requestMatchers("/seed", "/api/seed/**").hasRole("ADMIN");
                    }
                    auth.requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN");
                    auth.requestMatchers("/faculty/**").hasRole("FACULTY_HEAD");
                    auth.requestMatchers("/api/faculty/scores/**").hasRole("LECTURER");
                    auth.requestMatchers("/api/faculty/**").hasRole("FACULTY_HEAD");
                    auth.requestMatchers("/lecturer/**", "/api/lecturer/**").hasRole("LECTURER");
                    auth.requestMatchers("/student/**", "/api/student/**").hasRole("STUDENT");
                    auth.requestMatchers("/announcements/manage", "/api/announcements/manage/**")
                            .hasAnyRole("ADMIN", "FACULTY_HEAD");
                    auth.anyRequest().authenticated();
                })
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "hcmute-remember-me")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .exceptionHandling(exception -> exception.accessDeniedPage("/access-denied"))
                .headers(headers -> {
                    headers.frameOptions(frame -> frame.deny());
                    headers.contentTypeOptions(Customizer.withDefaults());
                    headers.referrerPolicy(referrer -> referrer
                            .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN));
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; "
                                    + "base-uri 'self'; "
                                    + "form-action 'self'; "
                                    + "frame-ancestors 'none'; "
                                    + "object-src 'none'; "
                                    + "script-src 'self'; "
                                    + "style-src 'self'; "
                                    + "img-src 'self' data:; "
                                    + "connect-src 'self'"));
                    if (secureCookies) {
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000));
                    }
                });

        if (rememberMeEnabled) {
            Assert.state(StringUtils.hasText(rememberMeKey) && rememberMeKey.length() >= 32,
                    "security.remember-me.key must be at least 32 characters when remember-me is enabled");
            Assert.state(rememberMeValiditySeconds > 0,
                    "security.remember-me.token-validity-seconds must be positive");
            http.rememberMe(rememberMe -> rememberMe
                    .key(rememberMeKey)
                    .tokenValiditySeconds(rememberMeValiditySeconds)
                    .useSecureCookie(secureCookies)
                    .rememberMeParameter("remember-me")
                    .rememberMeCookieName("hcmute-remember-me"));
        }

        if (googleOAuthEnabled) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .defaultSuccessUrl("/dashboard", true)
                    .failureUrl("/login?error")
                    .userInfoEndpoint(userInfo -> userInfo.userService(googleOAuth2UserService))
                    .permitAll());
        }

        return http.build();
    }
}
