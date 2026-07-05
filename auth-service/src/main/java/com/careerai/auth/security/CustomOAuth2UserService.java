package com.careerai.auth.security;

import com.careerai.auth.domain.entity.Role;
import com.careerai.auth.domain.entity.User;
import com.careerai.auth.domain.enums.AuthProvider;
import com.careerai.auth.domain.enums.RoleName;
import com.careerai.auth.repository.RoleRepository;
import com.careerai.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Resolves an OAuth2 login to a local {@link User}, provisioning the account on
 * first sign-in. Supports Google and GitHub attribute shapes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = providerFor(registrationId);

        Map<String, Object> attributes = oauth2User.getAttributes();
        OAuthAttributes parsed = parse(provider, attributes);

        if (parsed.email() == null || parsed.email().isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_available"),
                    "Email not available from " + registrationId + " account");
        }

        User user = upsertUser(provider, parsed);

        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(
                user.getAuthorities().stream()
                        .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
                        .toList(),
                attributes,
                nameAttributeKey);
    }

    private User upsertUser(AuthProvider provider, OAuthAttributes parsed) {
        return userRepository.findByEmail(parsed.email())
                .map(existing -> linkProvider(existing, provider, parsed))
                .orElseGet(() -> createUser(provider, parsed));
    }

    private User linkProvider(User user, AuthProvider provider, OAuthAttributes parsed) {
        // Keep an existing LOCAL account but record the provider link and refresh profile fields.
        if (user.getProvider() == AuthProvider.LOCAL) {
            user.setProvider(provider);
        }
        if (user.getProviderId() == null) {
            user.setProviderId(parsed.providerId());
        }
        if (user.getProfilePictureUrl() == null) {
            user.setProfilePictureUrl(parsed.pictureUrl());
        }
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private User createUser(AuthProvider provider, OAuthAttributes parsed) {
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER is not seeded"));
        User user = User.builder()
                .email(parsed.email())
                .firstName(parsed.firstName())
                .lastName(parsed.lastName())
                .profilePictureUrl(parsed.pictureUrl())
                .provider(provider)
                .providerId(parsed.providerId())
                .emailVerified(true)
                .enabled(true)
                .build();
        user.addRole(userRole);
        return userRepository.save(user);
    }

    private AuthProvider providerFor(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> AuthProvider.GOOGLE;
            case "github" -> AuthProvider.GITHUB;
            default -> throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"),
                    "Unsupported OAuth2 provider: " + registrationId);
        };
    }

    private OAuthAttributes parse(AuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> {
                String fullName = asString(attributes.get("name"));
                String[] names = splitName(fullName,
                        asString(attributes.get("given_name")),
                        asString(attributes.get("family_name")));
                yield new OAuthAttributes(
                        asString(attributes.get("email")),
                        names[0],
                        names[1],
                        asString(attributes.get("picture")),
                        asString(attributes.get("sub")));
            }
            case GITHUB -> {
                String[] names = splitName(asString(attributes.get("name")),
                        asString(attributes.get("login")), "");
                yield new OAuthAttributes(
                        asString(attributes.get("email")),
                        names[0],
                        names[1],
                        asString(attributes.get("avatar_url")),
                        String.valueOf(attributes.get("id")));
            }
            default -> throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"), "Unsupported provider");
        };
    }

    private String[] splitName(String fullName, String givenFallback, String familyFallback) {
        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+", 2);
            String first = parts[0];
            String last = parts.length > 1 ? parts[1] : "";
            return new String[]{first, last.isBlank() ? defaultLast(familyFallback) : last};
        }
        String first = (givenFallback == null || givenFallback.isBlank()) ? "User" : givenFallback;
        return new String[]{first, defaultLast(familyFallback)};
    }

    private String defaultLast(String familyFallback) {
        return (familyFallback == null || familyFallback.isBlank()) ? "-" : familyFallback;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private record OAuthAttributes(String email, String firstName, String lastName,
                                   String pictureUrl, String providerId) {
    }
}
