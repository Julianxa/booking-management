package com.example.service;


import com.example.constant.Enums;
import com.example.exception.ResourceNotFoundException;
import com.example.exception.UnverifiedEmailException;
import com.example.mapper.UserMapper;
import com.example.model.entity.Users;
import com.example.model.dto.*;
import com.example.repository.OrganizationsRepository;
import com.example.repository.UsersRepository;
import com.example.utils.ReferenceNoGenerator;
import com.example.model.dto.UserRegistrationResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.constant.Enums.UserRole.USER;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UsersRepository usersRepository;
    private final UserMapper userMapper;
    private final AwsService awsService;
    private final ReferenceNoGenerator referenceNoGenerator;
    private final OrganizationsRepository organizationsRepository;

    public UserRegistrationResponseDTO register(UserRegistrationRequestDTO userRegistrationRequestDTO) throws SQLException {
        Users user;
        SignUpResponse res = awsService.signUp(userRegistrationRequestDTO);

        if (res.userSub() != null) { // sign up successfully
            user = new Users();
            user.setUserSub(res.userSub());
        } else { // User exists
            user = usersRepository.findByEmailAndStatus(userRegistrationRequestDTO.getEmail(), Enums.UserStatus.UNCONFIRMED)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        Long orgId = null;
        if(userRegistrationRequestDTO.getOrgId() != null) {
            orgId = organizationsRepository.findIdByRefNo(userRegistrationRequestDTO.getOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        }

        user.setRefNo(referenceNoGenerator.generateUserReference(userRegistrationRequestDTO.getRole()));
        user.setFirstName(userRegistrationRequestDTO.getFirstName());
        user.setLastName(userRegistrationRequestDTO.getLastName());
        user.setPhone(userRegistrationRequestDTO.getPhone());
        user.setEmail(userRegistrationRequestDTO.getEmail());
        user.setRole(userRegistrationRequestDTO.getRole() != null
                ? userRegistrationRequestDTO.getRole()
                : USER);
        user.setCountry(userRegistrationRequestDTO.getCountry());
        user.setGender(userRegistrationRequestDTO.getGender());
        user.setOrgId(orgId);
        user.setStatus(Enums.UserStatus.CONFIRMED);
        usersRepository.save(user);

        UserRegistrationResponseDTO userRegistrationResponseDTO = new UserRegistrationResponseDTO();
        userRegistrationResponseDTO.setId(user.getRefNo());
        userRegistrationResponseDTO.setUserSub(user.getUserSub());
        userRegistrationResponseDTO.setEmail(userRegistrationRequestDTO.getEmail());
        userRegistrationResponseDTO.setPhone(userRegistrationRequestDTO.getPhone());
        userRegistrationResponseDTO.setCountry(userRegistrationRequestDTO.getCountry());
        userRegistrationResponseDTO.setGender(userRegistrationRequestDTO.getGender());
        userRegistrationResponseDTO.setLastName(userRegistrationRequestDTO.getLastName());
        userRegistrationResponseDTO.setFirstName(userRegistrationRequestDTO.getFirstName());
        userRegistrationResponseDTO.setOrgId(userRegistrationResponseDTO.getOrgId());
        userRegistrationResponseDTO.setStatus(Enums.UserStatus.CONFIRMED);
        userRegistrationResponseDTO.setSession(res.session());
        userRegistrationResponseDTO.setCreatedAt(user.getCreatedAt());
        userRegistrationResponseDTO.setUpdatedAt(user.getUpdatedAt());
        userRegistrationResponseDTO.setMessage("User registered successfully");
        userRegistrationResponseDTO.setTimestamp(LocalDateTime.now());
        return userRegistrationResponseDTO;
    }

    public ConfirmUserRegistrationResponseDTO confirmSignUp(ConfirmUserRegistrationRequestDTO confirmSignUpRequestDTO) {
        Users user = usersRepository.findByEmailAndStatus(confirmSignUpRequestDTO.getEmail(), Enums.UserStatus.UNCONFIRMED)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user != null) {
            ConfirmSignUpResponse res = awsService.confirmSignUp(confirmSignUpRequestDTO);
            ConfirmUserRegistrationResponseDTO confirmSignUpResponseDTO = new ConfirmUserRegistrationResponseDTO();
            if(res.session() != null && !res.session().isEmpty()) {
                awsService.setEmailVerified(confirmSignUpRequestDTO.getEmail());

                confirmSignUpResponseDTO.setEmail(confirmSignUpRequestDTO.getEmail());
                confirmSignUpResponseDTO.setMessage("User registration confirmed successfully");
                confirmSignUpResponseDTO.setTimestamp(LocalDateTime.now());

                user.setStatus(Enums.UserStatus.CONFIRMED);
                user.setUpdatedAt(LocalDateTime.now());
                usersRepository.save(user);
            } else {
                confirmSignUpResponseDTO.setEmail(confirmSignUpResponseDTO.getEmail());
                confirmSignUpResponseDTO.setSession(res.session());
                confirmSignUpResponseDTO.setMessage("Wrong confirmation code");
                confirmSignUpResponseDTO.setTimestamp(LocalDateTime.now());
            }
            return confirmSignUpResponseDTO;
        } else {
            throw new RuntimeException("Failed to confirm user");
        }
    }

    public ForgotPasswordResponseDTO forgotPassword(ForgotPasswordRequestDTO forgotPasswordRequestDTO) throws UnverifiedEmailException {
        awsService.forgotPassword(forgotPasswordRequestDTO);

        ForgotPasswordResponseDTO forgotPasswordResponseDTO = new ForgotPasswordResponseDTO();
        forgotPasswordResponseDTO.setEmail(forgotPasswordRequestDTO.getEmail());
        forgotPasswordResponseDTO.setMessage("Forgot password initiated successfully");
        forgotPasswordResponseDTO.setTimestamp(LocalDateTime.now());
        return forgotPasswordResponseDTO;
    }

    public ConfirmForgotPasswordResponseDTO confirmForgotPassword(ConfirmForgotPasswordRequestDTO confirmForgotPasswordRequestDTO) {
        awsService.confirmForgotPassword(confirmForgotPasswordRequestDTO);
        ConfirmForgotPasswordResponseDTO confirmForgotPasswordResponseDTO = new ConfirmForgotPasswordResponseDTO();
        confirmForgotPasswordResponseDTO.setMessage("OTP for Forgot Password confirmed successfully");
        confirmForgotPasswordResponseDTO.setTimestamp(LocalDateTime.now());
        return confirmForgotPasswordResponseDTO;
    }

    public ResetPasswordResponseDTO resetPassword(ResetPasswordRequestDTO resetPasswordRequestDTO) {
        if (!resetPasswordRequestDTO.getPassword().equals(resetPasswordRequestDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirmation password do not match");
        }
        awsService.setPassword(resetPasswordRequestDTO);
        ResetPasswordResponseDTO resetPasswordResponseDTO = new ResetPasswordResponseDTO();
        resetPasswordResponseDTO.setMessage("Password reset successfully");
        resetPasswordResponseDTO.setTimestamp(LocalDateTime.now());
        return resetPasswordResponseDTO;
    }

    public ChangePasswordResponseDTO changePassword(String accessToken, ChangePasswordRequestDTO changePasswordRequestDTO) {
        if (!changePasswordRequestDTO.getPassword().equals(changePasswordRequestDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirmation password do not match");
        }
        awsService.changePassword(accessToken, changePasswordRequestDTO);
        ChangePasswordResponseDTO changePasswordResponseDTO = new ChangePasswordResponseDTO();
        changePasswordResponseDTO.setMessage("Password changed successfully");
        changePasswordResponseDTO.setTimestamp(LocalDateTime.now());
        return changePasswordResponseDTO;
    }

    @Transactional
    public DeleteUserResponseDTO deleteUser(String userSub, String accessToken, DeleteUserRequestDTO deleteUserRequestDTO) {
        Users user = usersRepository.findByUserSub(userSub)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        awsService.verifyUserCredentials(user.getEmail(), deleteUserRequestDTO.getPassword());

        // inactivate user status
        usersRepository.updateStatusToInactiveByOwnerUserId(user.getId(), LocalDateTime.now());

        awsService.deleteUser(accessToken);
        DeleteUserResponseDTO deleteUserResponseDTO = new DeleteUserResponseDTO();
        deleteUserResponseDTO.setMessage("User deleted successfully");
        deleteUserResponseDTO.setTimestamp(LocalDateTime.now());
        return deleteUserResponseDTO;
    }

    public GetUserResponseDTO getUserByUserSub(String userSub) {
        Users user = usersRepository.findByUserSub(userSub)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with userSub: " + userSub));

        String orgRefNo = organizationsRepository.findRefNoById(user.getOrgId()).orElse(null);

        return GetUserResponseDTO.builder()
                .id(user.getRefNo())
                .userSub(user.getUserSub())
                .role(user.getRole())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .country(user.getCountry())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .orgId(orgRefNo)
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .message("Retrieve user successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public GetUserResponseDTO getUserByIdAndRole(String userRefNo, Enums.UserRole role) {
        Long userId = usersRepository.findIdByRefNo(userRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with reference no: " + userRefNo));
        Users user = usersRepository.findByIdAndRole(userId, role)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with code: " + userId));
        String orgRefNo = organizationsRepository.findRefNoById(user.getOrgId()).orElse(null);

        return GetUserResponseDTO.builder()
                .id(user.getRefNo())
                .userSub(user.getUserSub())
                .role(user.getRole())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .country(user.getCountry())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .orgId(orgRefNo)
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .message("Retrieve user successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    public GetListUserResponseDTO getAllUsers(Pageable pageable, String search, Enums.UserRole role, String orgRefNo) {
        Page<Users> usersPage;

        if (orgRefNo != null) {
            Long orgId = organizationsRepository.findIdByRefNo(orgRefNo)
                    .orElseThrow(() -> new ResourceNotFoundException("Organization not found with reference no: " + orgRefNo));

            usersPage = usersRepository.findByOrganizationIdAndFilters(
                    orgId,
                    StringUtils.trimToNull(search),
                    role,
                    pageable
            );
        } else if (StringUtils.isNotBlank(search)) {
            usersPage = usersRepository.findBySearchTerm(search, pageable);
        } else if (role != null) {
            usersPage = usersRepository.findByRole(role, pageable);
        } else {
            usersPage = usersRepository.findAll(pageable);
        }

        List<GetUserResponseDTO> content = usersPage.getContent().stream()
                .map(user -> {
                    String userOrgRefNo = organizationsRepository.findRefNoById(user.getOrgId())
                            .orElse(null);
                    return userMapper.toResponseDto(user, userOrgRefNo);
                })
                .collect(Collectors.toList());

        GetListUserResponseDTO response = new GetListUserResponseDTO();
        response.setContent(content);
        response.setLast(usersPage.isLast());
        response.setTotalPages(usersPage.getTotalPages());
        response.setTotalElements(usersPage.getTotalElements());
        response.setSize(usersPage.getSize());
        response.setNumber(usersPage.getNumber());
        response.setFirst(usersPage.isFirst());
        response.setNumberOfElements(usersPage.getNumberOfElements());
        response.setEmpty(usersPage.isEmpty());
        return response;
    }

    @Transactional
    public GetUserResponseDTO updateUserByAdmin(String userRefNo, UpdateUserRequestDTO dto) {
        Long userId = usersRepository.findIdByRefNo(userRefNo)
                .orElseThrow(() -> new RuntimeException("Event not found with reference no: " + userRefNo));
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        if (dto.getCountry() != null) user.setCountry(dto.getCountry());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getOrgId() != null) user.setOrgId(
                organizationsRepository.findIdByRefNo(dto.getOrgId()).orElse(null)
        );

        user = usersRepository.save(user);

        return userMapper.toResponseDto(user, dto.getOrgId());
    }
}
