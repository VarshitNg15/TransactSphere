package com.transactsphere.user.service;

import com.transactsphere.user.dto.UserProfileResponse;
import com.transactsphere.user.dto.UserProfileUpdateRequest;
import com.transactsphere.user.model.KycStatus;
import com.transactsphere.user.model.UserProfile;
import com.transactsphere.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    /**
     * Gets profile of a user. If not found, auto-initializes it with skeleton data.
     */
    @Transactional
    public UserProfileResponse getOrCreateProfile(Long userId, String username) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> {
                    UserProfile newProfile = UserProfile.builder()
                            .id(userId)
                            .username(username)
                            .email(username + "@transactsphere.com")
                            .kycStatus(KycStatus.PENDING)
                            .build();
                    return userProfileRepository.save(newProfile);
                });
        return mapToResponse(profile);
    }

    /**
     * Updates an existing profile or creates one if not present.
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, String username, UserProfileUpdateRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElse(UserProfile.builder()
                        .id(userId)
                        .username(username)
                        .kycStatus(KycStatus.PENDING)
                        .build());

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setEmail(request.getEmail());
        profile.setAddress(request.getAddress());
        if (request.getKycDocument() != null && !request.getKycDocument().isEmpty()) {
            profile.setKycDocument(request.getKycDocument());
        }

        UserProfile saved = userProfileRepository.save(profile);
        return mapToResponse(saved);
    }

    /**
     * Update KYC status (Restricted to Admin/Employee).
     */
    @Transactional
    public UserProfileResponse updateKycStatus(Long userId, KycStatus status) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found with ID: " + userId));
        profile.setKycStatus(status);
        UserProfile saved = userProfileRepository.save(profile);
        return mapToResponse(saved);
    }

    /**
     * List all profiles (Restricted to Admin/Employee).
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllProfiles() {
        return userProfileRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get profile by ID (Restricted to Admin/Employee).
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileById(Long userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found with ID: " + userId));
        return mapToResponse(profile);
    }

    private UserProfileResponse mapToResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .username(profile.getUsername())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .phoneNumber(profile.getPhoneNumber())
                .email(profile.getEmail())
                .address(profile.getAddress())
                .kycDocument(profile.getKycDocument())
                .kycStatus(profile.getKycStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
