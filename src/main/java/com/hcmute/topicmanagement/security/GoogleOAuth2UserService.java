package com.hcmute.topicmanagement.security;

import java.util.Collection;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GoogleOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final DatabaseUserDetailsService databaseUserDetailsService;
    private final Set<String> allowedDomains;

    public GoogleOAuth2UserService(
            DatabaseUserDetailsService databaseUserDetailsService,
            @Value("${google.oauth.allowed-domains:}") String configuredDomains) {
        this.databaseUserDetailsService = databaseUserDetailsService;
        this.allowedDomains = Arrays.stream(configuredDomains.split(","))
                .map(String::trim)
                .map(value -> value.startsWith("@") ? value.substring(1) : value)
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = delegate.loadUser(userRequest);
        String email = user.getAttribute("email");
        Boolean emailVerified = user.getAttribute("email_verified");
        if (!StringUtils.hasText(email) || !Boolean.TRUE.equals(emailVerified)) {
            throw oauthFailure("Google account email must be present and verified");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        int atIndex = normalizedEmail.lastIndexOf('@');
        String domain = atIndex < 0 ? "" : normalizedEmail.substring(atIndex + 1);
        if (allowedDomains.isEmpty() || !allowedDomains.contains(domain)) {
            throw oauthFailure("Google account domain is not allowed");
        }

        UserDetails localAccount;
        try {
            localAccount = databaseUserDetailsService.loadUserByLoginIdentifierOrEmail(normalizedEmail);
        } catch (UsernameNotFoundException | DisabledException ex) {
            throw oauthFailure("Google account is not provisioned for this application");
        }

        DatabaseUserPrincipal databasePrincipal = (DatabaseUserPrincipal) localAccount;
        String fullName = firstNonBlank(user.getAttribute("name"), email, user.getName());
        String username = firstNonBlank(normalizedEmail, user.getName());

        return new GoogleUserPrincipal(
                username,
                firstNonBlank(databasePrincipal.getFullName(), fullName),
                databasePrincipal.getPrimaryRoleName(),
                databasePrincipal.getAuthorities(),
                user.getAttributes());
    }

    private static OAuth2AuthenticationException oauthFailure(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"), message);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "Google user";
    }

    public static final class GoogleUserPrincipal implements OAuth2User {

        private final String username;
        private final String fullName;
        private final String primaryRoleName;
        private final Collection<? extends GrantedAuthority> authorities;
        private final Map<String, Object> attributes;

        private GoogleUserPrincipal(
                String username,
                String fullName,
                String primaryRoleName,
                Collection<? extends GrantedAuthority> authorities,
                Map<String, Object> attributes) {
            this.username = username;
            this.fullName = fullName;
            this.primaryRoleName = primaryRoleName;
            this.authorities = List.copyOf(authorities);
            this.attributes = Map.copyOf(new LinkedHashMap<>(attributes));
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public String getName() {
            return username;
        }

        public String getFullName() {
            return fullName;
        }

        public String getPrimaryRoleName() {
            return primaryRoleName;
        }

        public String getInitials() {
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length == 1) {
                return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
            }
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                    .toUpperCase(Locale.ROOT);
        }
    }
}
