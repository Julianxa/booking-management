package com.example.service;


import com.example.constant.Enums;
import com.example.exception.organization.OrganizationNotFoundException;
import com.example.exception.user.UserNotFoundException;
import com.example.mapper.UserMapper;
import com.example.model.dto.*;
import com.example.model.entity.Users;
import com.example.repository.OrganizationsRepository;
import com.example.repository.UsersRepository;
import com.example.utils.ReferenceNoGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.constant.Enums.UserRole.USER;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UsersRepository usersRepository;
    private final UserMapper userMapper;
    private final AwsService awsService;
    private final AuditService auditService;
    private final ReferenceNoGenerator referenceNoGenerator;
    private final OrganizationsRepository organizationsRepository;

    public UserRegistrationResponseDTO register(UserRegistrationRequestDTO userRegistrationRequestDTO) {
        Users user;
        SignUpResponse res = awsService.signUp(userRegistrationRequestDTO);

        if (res.userSub() != null) { // sign up successfully
            user = new Users();
            user.setUserSub(res.userSub());
        } else {
            throw new RuntimeException("User exists");
        }

        Long orgId = null;
        if (userRegistrationRequestDTO.getOrgId() != null) {
            orgId = organizationsRepository.findIdByRefNo(userRegistrationRequestDTO.getOrgId())
                    .orElseThrow(() -> new OrganizationNotFoundException(String.format("Organization %s not found", userRegistrationRequestDTO.getOrgId())));
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
        userRegistrationResponseDTO.setTimestamp(ZonedDateTime.now());

        auditService.record("REGISTER_USER",
                Users.class.getName(),
                user.getId(),
                null,
                user.getRefNo()
        );
        return userRegistrationResponseDTO;
    }

    public ConfirmUserRegistrationResponseDTO confirmSignUp(ConfirmUserRegistrationRequestDTO confirmSignUpRequestDTO) {
        Users user = usersRepository.findByEmailAndStatus(confirmSignUpRequestDTO.getEmail(), Enums.UserStatus.UNCONFIRMED)
                .orElseThrow(() -> new UserNotFoundException(String.format("User not found with email %s", confirmSignUpRequestDTO.getEmail())));
        if (user != null) {
            ConfirmSignUpResponse res = awsService.confirmSignUp(confirmSignUpRequestDTO);
            ConfirmUserRegistrationResponseDTO confirmSignUpResponseDTO = new ConfirmUserRegistrationResponseDTO();
            if (res.session() != null && !res.session().isEmpty()) {
                awsService.setEmailVerified(confirmSignUpRequestDTO.getEmail());

                confirmSignUpResponseDTO.setEmail(confirmSignUpRequestDTO.getEmail());
                confirmSignUpResponseDTO.setMessage("User registration confirmed successfully");
                confirmSignUpResponseDTO.setTimestamp(ZonedDateTime.now());

                user.setStatus(Enums.UserStatus.CONFIRMED);
                user.setUpdatedAt(ZonedDateTime.now());
                usersRepository.save(user);
            } else {
                confirmSignUpResponseDTO.setEmail(confirmSignUpResponseDTO.getEmail());
                confirmSignUpResponseDTO.setSession(res.session());
                confirmSignUpResponseDTO.setMessage("Wrong confirmation code");
                confirmSignUpResponseDTO.setTimestamp(ZonedDateTime.now());
            }
            auditService.record("REGISTER_USER",
                    Users.class.getName(),
                    user.getId(),
                    null,
                    String.format("Confirm user %s successfully", user.getRefNo())
            );
            return confirmSignUpResponseDTO;
        } else {
            auditService.record("REGISTER_USER",
                    Users.class.getName(),
                    null,
                    null,
                    "Failed to confirm user"
            );
            throw new RuntimeException("Failed to confirm user");
        }
    }

    public ForgotPasswordResponseDTO forgotPassword(ForgotPasswordRequestDTO forgotPasswordRequestDTO) {
        awsService.forgotPassword(forgotPasswordRequestDTO);

        ForgotPasswordResponseDTO forgotPasswordResponseDTO = new ForgotPasswordResponseDTO();
        forgotPasswordResponseDTO.setEmail(forgotPasswordRequestDTO.getEmail());
        forgotPasswordResponseDTO.setMessage("Forgot password initiated successfully");
        forgotPasswordResponseDTO.setTimestamp(ZonedDateTime.now());
        return forgotPasswordResponseDTO;
    }

    public ConfirmForgotPasswordResponseDTO confirmForgotPassword(ConfirmForgotPasswordRequestDTO confirmForgotPasswordRequestDTO) {
        awsService.confirmForgotPassword(confirmForgotPasswordRequestDTO);
        ConfirmForgotPasswordResponseDTO confirmForgotPasswordResponseDTO = new ConfirmForgotPasswordResponseDTO();
        confirmForgotPasswordResponseDTO.setMessage("OTP for Forgot Password confirmed successfully");
        confirmForgotPasswordResponseDTO.setTimestamp(ZonedDateTime.now());
        return confirmForgotPasswordResponseDTO;
    }

    public ResetPasswordResponseDTO resetPassword(ResetPasswordRequestDTO resetPasswordRequestDTO) {
        if (!resetPasswordRequestDTO.getPassword().equals(resetPasswordRequestDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirmation password do not match");
        }
        awsService.setPassword(resetPasswordRequestDTO);
        ResetPasswordResponseDTO resetPasswordResponseDTO = new ResetPasswordResponseDTO();
        resetPasswordResponseDTO.setMessage("Password reset successfully");
        resetPasswordResponseDTO.setTimestamp(ZonedDateTime.now());
        return resetPasswordResponseDTO;
    }

    public ChangePasswordResponseDTO changePassword(String accessToken, ChangePasswordRequestDTO changePasswordRequestDTO) {
        if (!changePasswordRequestDTO.getPassword().equals(changePasswordRequestDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirmation password do not match");
        }
        awsService.changePassword(accessToken, changePasswordRequestDTO);
        ChangePasswordResponseDTO changePasswordResponseDTO = new ChangePasswordResponseDTO();
        changePasswordResponseDTO.setMessage("Password changed successfully");
        changePasswordResponseDTO.setTimestamp(ZonedDateTime.now());
        return changePasswordResponseDTO;
    }

    @Transactional
    public DeleteUserResponseDTO deleteUser(String userSub, String accessToken, DeleteUserRequestDTO deleteUserRequestDTO) {
        Users user = usersRepository.findByUserSub(userSub)
                .orElseThrow(() -> new UserNotFoundException(String.format("User %s not found", userSub)));
        awsService.verifyUserCredentials(user.getEmail(), deleteUserRequestDTO.getPassword());

        // inactivate user status
        usersRepository.updateStatusToInactiveByOwnerUserId(user.getId(), ZonedDateTime.now());

        awsService.deleteUser(accessToken);
        DeleteUserResponseDTO deleteUserResponseDTO = new DeleteUserResponseDTO();
        deleteUserResponseDTO.setMessage("User deleted successfully");
        deleteUserResponseDTO.setTimestamp(ZonedDateTime.now());
        return deleteUserResponseDTO;
    }

    @Transactional
    public DeleteUserResponseDTO deleteUserById(String userRefNo) {
        Users user = usersRepository.findByRefNo(userRefNo)
                .orElseThrow(() -> new UserNotFoundException(String.format("User %s not found", userRefNo)));

        // inactivate user status
        usersRepository.updateStatusToInactiveByOwnerUserId(user.getId(), ZonedDateTime.now());

        awsService.deleteUserByAdmin(user.getUserSub());
        DeleteUserResponseDTO deleteUserResponseDTO = new DeleteUserResponseDTO();
        deleteUserResponseDTO.setMessage("User deleted successfully");
        deleteUserResponseDTO.setTimestamp(ZonedDateTime.now());
        return deleteUserResponseDTO;
    }

    public GetUserResponseDTO getUserByUserSub(String userSub) {
        Users user = usersRepository.findByUserSub(userSub)
                .orElseThrow(() -> new UserNotFoundException(String.format("User %s not found", userSub)));

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
                .timestamp(ZonedDateTime.now())
                .build();
    }

    public GetUserResponseDTO getUserByIdAndRole(String userRefNo, Enums.UserRole role) {
        Users user = usersRepository.findByRefNoAndRole(userRefNo, role)
                .orElseThrow(() -> new UserNotFoundException(String.format("User %s not found", userRefNo)));
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
                .timestamp(ZonedDateTime.now())
                .build();
    }

    public GetListUserResponseDTO getAllUsers(Pageable pageable, String search, Enums.UserRole role, String orgRefNo) {
        Page<Users> usersPage;

        if (orgRefNo != null) {
            Long orgId = organizationsRepository.findIdByRefNo(orgRefNo)
                    .orElseThrow(() -> new OrganizationNotFoundException(String.format("Organization %s not found", orgRefNo)));

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
                    return userMapper.toResponseDTO(user, userOrgRefNo);
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

        return userMapper.toResponseDTO(user, dto.getOrgId());
    }
}
