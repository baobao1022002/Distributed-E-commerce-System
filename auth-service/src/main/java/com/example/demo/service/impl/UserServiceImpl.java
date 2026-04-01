package com.example.demo.service.impl;

import com.example.demo.dto.UserInforDto;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.request.LoginResponse;
import com.example.demo.dto.request.UserRegistrationDto;
import com.example.demo.dto.response.RegistrationResponse;
import com.example.demo.entity.User;
import com.example.demo.exception.InvalidCredentialsException;
import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.exception.UserCreationFailedException;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final Keycloak keycloak;
    private final KeycloakBuilder keycloakBuilder;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${keycloak.realm}")
    private String realm;

    @Override
    @Transactional
    public RegistrationResponse createUser(UserRegistrationDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + dto.getEmail());
        }

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(resolveUsername(dto));
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(dto.getPassword());
        user.setCredentials(Collections.singletonList(credential));

        Response response = keycloak.realm(realm).users().create(user);
        try {
            log.info("Create Keycloak user status: {}", response.getStatus());

            if (response.getStatus() == 201) {
                String keycloakUserId = extractCreatedUserId(response);
                if (keycloakUserId == null || keycloakUserId.isBlank()) {
                    throw new UserCreationFailedException("Khong lay duoc userId tu Keycloak", 500);
                }
                if (userRepository.existsByCustomerId(keycloakUserId)) {
                    throw new UserAlreadyExistsException("Customer identity already exists");
                }

                User savedUser = userRepository.save(User.builder()
                        .customerId(keycloakUserId)
                        .email(dto.getEmail())
                        .password(passwordEncoder.encode(dto.getPassword()))
                        .build());

                log.info("User created successfully: {}", dto.getEmail());
                return new RegistrationResponse(savedUser.getCustomerId(), savedUser.getEmail());
            }

            String errorMessage = response.readEntity(String.class);
            log.error("Create user failed with status {}: {}", response.getStatus(), errorMessage);

            switch (response.getStatus()) {
                case 409:
                    throw new UserAlreadyExistsException(parseKeycloakError(errorMessage));
                case 400:
                    throw new UserCreationFailedException(parseKeycloakError(errorMessage), 400);
                case 403:
                    throw new UserCreationFailedException("Khong co quyen tao user trong Keycloak", 403);
                default:
                    throw new UserCreationFailedException(
                            "Khong the tao user: " + parseKeycloakError(errorMessage),
                            response.getStatus()
                    );
            }
        } finally {
            response.close();
        }
    }

    private String parseKeycloakError(String errorResponse) {
        try {
            // Parse JSON error response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(errorResponse);

            if (node.has("errorMessage")) {
                return node.get("errorMessage").asText();
            }

            if (node.has("error")) {
                return node.get("error").asText();
            }

            return errorResponse;
        } catch (Exception e) {
            log.warn("Failed to parse error response: {}", errorResponse);
            return errorResponse;
        }
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String usernameOrEmail = resolveEmail(request);

        Keycloak userKeycloakClient = keycloakBuilder
                .grantType(OAuth2Constants.PASSWORD)
                .username(usernameOrEmail)
                .password(request.getPassword())
                .build();

        AccessTokenResponse tokenResponse;
        try {
            tokenResponse = userKeycloakClient.tokenManager().getAccessToken();
        } catch (Exception ex) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String email = extractClaimFromJwt(tokenResponse.getToken(), "email");
        String subject = extractClaimFromJwt(tokenResponse.getToken(), "sub");

        User user = email != null ? userRepository.findByEmail(email).orElse(null) : null;
        String customerId = subject != null ? subject : (user != null ? user.getCustomerId() : null);

        if (customerId == null || customerId.isBlank()) {
            throw new InvalidCredentialsException("Unable to resolve customer identity from token");
        }

        return new LoginResponse(
                tokenResponse.getToken(),
                tokenResponse.getRefreshToken(),
                tokenResponse.getExpiresIn(),
                subject,
                customerId,
                email
        );
    }

    @Override
    public List<UserInforDto> getAllUsers() {
        List<User> userDb = userRepository.findAll();

        return userDb.stream()
                .map(user -> UserInforDto.builder()
                    .customerId(user.getCustomerId())
                    .email(user.getEmail())
                    .build())
                .collect(Collectors.toList());
    }

    private String resolveUsername(UserRegistrationDto dto) {
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            return dto.getUsername();
        }
        return dto.getEmail();
    }

    private String resolveEmail(LoginRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            return request.getEmail();
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            return request.getUsername();
        }
        throw new InvalidCredentialsException("Email is required");
    }

    private String extractCreatedUserId(Response response) {
        if (response.getLocation() == null) {
            return null;
        }
        String path = response.getLocation().getPath();
        if (path == null || path.isBlank()) {
            return null;
        }
        String[] segments = path.split("/");
        return segments.length == 0 ? null : segments[segments.length - 1];
    }

    private String extractClaimFromJwt(String token, String claimName) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] decodedPayload = java.util.Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = new ObjectMapper().readTree(decodedPayload);
            JsonNode claimValue = payload.get(claimName);
            return claimValue != null ? claimValue.asText(null) : null;
        } catch (Exception ex) {
            log.warn("Cannot extract claim {} from JWT", claimName);
            return null;
        }
    }
}
