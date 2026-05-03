package com.example.service;

import com.example.model.dto.ChangePasswordRequestDTO;
import com.example.model.dto.ResetPasswordRequestDTO;
import com.example.config.AwsConfig;
import com.example.exception.InvalidIdTokenException;
import com.example.exception.UnverifiedEmailException;
import com.example.utils.CognitoJwtParser;
import com.example.model.dto.*;
import com.example.utils.FileUtils;
import com.example.utils.HashGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.*;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType.*;

@Service
public class AwsService {
    private final FileUtils fileUtils;
    private final HashGenerator hashGenerator;
    private final CognitoIdentityProviderClient cognitoClient;
    private final KmsClient kmsClient;
    private final S3TransferManager s3TransferManager;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String clientId;
    private final String region;
    private final String appSecretKey;
    private final String userPoolId;
    private final String cmkKeyId;
    private final String bucketName;
    Logger logger = LoggerFactory.getLogger(getClass());
    public AwsService(
            FileUtils fileUtils,
            HashGenerator hashGenerator,
            AwsConfig awsConfig
    ) {
        this.fileUtils = fileUtils;
        this.hashGenerator = hashGenerator;

        this.appSecretKey = awsConfig.appSecretKey();
        this.region = awsConfig.region();
        this.clientId = awsConfig.clientId();
        this.userPoolId = awsConfig.userPoolId();
        this.cognitoClient = awsConfig.cognitoClient();
        this.kmsClient = awsConfig.kmsClient();
        this.cmkKeyId = awsConfig.cmkKeyId();
        this.s3Presigner = awsConfig.s3Presigner();
        this.s3TransferManager = awsConfig.s3TransferManager();
        this.s3Client = awsConfig.s3Client();
        this.bucketName= awsConfig.bucketName();
    }

    // confirm account without OTP confirmation
    public SignUpResponse signUp(UserRegistrationRequestDTO userRegistrationRequestDTO) {
        try {
            SignUpRequest signUpRequest = SignUpRequest.builder()
                    .clientId(clientId)
                    .username(userRegistrationRequestDTO.getEmail())
                    .password(userRegistrationRequestDTO.getPassword())
                    .secretHash(hashGenerator.calculateSecretHash(clientId, userRegistrationRequestDTO.getEmail(), appSecretKey))
                    .userAttributes(
                            AttributeType.builder().name("email").value(userRegistrationRequestDTO.getEmail()).build(),
                            AttributeType.builder().name("given_name").value(userRegistrationRequestDTO.getFirstName()).build(),
                            AttributeType.builder().name("family_name").value(userRegistrationRequestDTO.getLastName()).build()
                    ).build();

            SignUpResponse signUpResponse = cognitoClient.signUp(signUpRequest);

            AdminConfirmSignUpRequest confirmRequest = AdminConfirmSignUpRequest.builder()
                    .userPoolId(userPoolId)
                    .username(signUpResponse.userSub())
                    .build();
            cognitoClient.adminConfirmSignUp(confirmRequest);

            List<AttributeType> userAttributes = List.of(
                    AttributeType.builder()
                            .name("email_verified")
                            .value("true")
                            .build()
            );

            AdminUpdateUserAttributesRequest updateRequest = AdminUpdateUserAttributesRequest.builder()
                    .userPoolId(userPoolId)
                    .username(signUpResponse.userSub())
                    .userAttributes(userAttributes)
                    .build();
            cognitoClient.adminUpdateUserAttributes(updateRequest);

            return signUpResponse;
        } catch (UsernameExistsException e) {
            throw new RuntimeException("User account already exists. Please use a different email.");
        }
    }

    // require OTP confirmation
//    public SignUpResponse signUp(UserRegistrationRequestDTO userRegistrationRequestDTO) {
//        try {
//            SignUpRequest signUpRequest = SignUpRequest.builder()
//                    .clientId(clientId)
//                    .username(userRegistrationRequestDTO.getEmail())
//                    .password(userRegistrationRequestDTO.getPassword())
//                    .secretHash(hashGenerator.calculateSecretHash(clientId, userRegistrationRequestDTO.getEmail(), appSecretKey))
//                    .userAttributes(
//                            AttributeType.builder().name("email").value(userRegistrationRequestDTO.getEmail()).build(),
//                            AttributeType.builder().name("given_name").value(userRegistrationRequestDTO.getFirstName()).build(),
//                            AttributeType.builder().name("family_name").value(userRegistrationRequestDTO.getLastName()).build()
//                    ).build();
//
//            return cognitoClient.signUp(signUpRequest);
//        } catch (UsernameExistsException e) {
//            throw new RuntimeException("User account already exists. Please use a different email.");
//        }
//    }

    public ConfirmSignUpResponse confirmSignUp(ConfirmUserRegistrationRequestDTO confirmSignUpRequestDTO) {
        ConfirmSignUpRequest confirmSignUpRequest = ConfirmSignUpRequest.builder()
                .clientId(clientId)
                .username(confirmSignUpRequestDTO.getEmail())
                .confirmationCode(confirmSignUpRequestDTO.getConfirmationCode())
                .secretHash(hashGenerator.calculateSecretHash(clientId, confirmSignUpRequestDTO.getEmail(), appSecretKey))
                .session(confirmSignUpRequestDTO.getSession())
                .build();
        return cognitoClient.confirmSignUp(confirmSignUpRequest);
    }

    public void verifyUserCredentials(String email, String password) {
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", email);
        authParameters.put("PASSWORD", password);
        authParameters.put("SECRET_HASH", hashGenerator.calculateSecretHash(clientId, email, appSecretKey));

        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .clientId(clientId)
                .authFlow(USER_PASSWORD_AUTH)
                .authParameters(authParameters)
                .build();
        cognitoClient.initiateAuth(authRequest);
    }

    public InitiateAuthResponse login(LoginRequestDTO loginRequestDTO) throws UnverifiedEmailException {
        boolean isEmailVerified = isEmailVerified(loginRequestDTO.getEmail());

        if (!isEmailVerified) {
            throw new UnverifiedEmailException("Email not verified");
        }

        verifyUserCredentials(loginRequestDTO.getEmail(), loginRequestDTO.getPassword());

        // Initiate OTP email sending with CUSTOM_AUTH flow
        Map<String, String> customAuthParameters = new HashMap<>();
        customAuthParameters.put("USERNAME", loginRequestDTO.getEmail());
        customAuthParameters.put("PASSWORD", loginRequestDTO.getPassword());
        customAuthParameters.put("SECRET_HASH", hashGenerator.calculateSecretHash(clientId, loginRequestDTO.getEmail(), appSecretKey));

        InitiateAuthRequest customAuthRequest = InitiateAuthRequest.builder()
                .clientId(clientId)
                .authFlow(USER_PASSWORD_AUTH)
                .authParameters(customAuthParameters)
                .build();

        return cognitoClient.initiateAuth(customAuthRequest);
    }

    public InitiateAuthResponse refresh(TokenRenewalRequestDTO tokenRenewalRequestDTO) {
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("REFRESH_TOKEN", tokenRenewalRequestDTO.getRefreshToken());
        authParameters.put("SECRET_HASH", hashGenerator.calculateSecretHash(clientId, tokenRenewalRequestDTO.getUserSub(), appSecretKey));

        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .clientId(clientId)
                .authFlow(REFRESH_TOKEN)
                .authParameters(authParameters)
                .build();

        return cognitoClient.initiateAuth(authRequest);
    }

    public void forgotPassword(ForgotPasswordRequestDTO forgotPasswordRequestDTO) throws UnverifiedEmailException {
        boolean isEmailVerified = isEmailVerified(forgotPasswordRequestDTO.getEmail());

        if (!isEmailVerified) {
            throw new UnverifiedEmailException("Email not verified");
        }

        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .clientId(clientId)
                .username(forgotPasswordRequestDTO.getEmail())
                .secretHash(hashGenerator.calculateSecretHash(clientId, forgotPasswordRequestDTO.getEmail(), appSecretKey))
                .build();

        cognitoClient.forgotPassword(request);
    }

    public void confirmForgotPassword(ConfirmForgotPasswordRequestDTO confirmForgotPasswordRequestDTO) {
        ConfirmForgotPasswordRequest request = ConfirmForgotPasswordRequest.builder()
                .clientId(clientId)
                .username(confirmForgotPasswordRequestDTO.getEmail())
                .confirmationCode(confirmForgotPasswordRequestDTO.getConfirmationCode())
                .password(confirmForgotPasswordRequestDTO.getNewPassword())
                .secretHash(hashGenerator.calculateSecretHash(clientId, confirmForgotPasswordRequestDTO.getEmail(), appSecretKey))
                .build();

        cognitoClient.confirmForgotPassword(request);
    }

    public AdminSetUserPasswordResponse setPassword(ResetPasswordRequestDTO resetPasswordRequestDTO) {
        AdminSetUserPasswordRequest adminSetUserPasswordRequest = AdminSetUserPasswordRequest.builder()
                .userPoolId(userPoolId)
                .username(resetPasswordRequestDTO.getEmail())
                .password(resetPasswordRequestDTO.getPassword())
                .permanent(true)
                .build();

        return cognitoClient.adminSetUserPassword(adminSetUserPasswordRequest);
    }

    public ChangePasswordResponse changePassword(String accessToken, ChangePasswordRequestDTO changePasswordRequestDTO) {
        ChangePasswordRequest changePasswordRequest = ChangePasswordRequest.builder()
                .accessToken(accessToken)
                .previousPassword(changePasswordRequestDTO.getOldPassword())
                .proposedPassword(changePasswordRequestDTO.getPassword())
                .build();
        return cognitoClient.changePassword(changePasswordRequest);
    }

    public DeleteUserResponse deleteUser(String accessToken) {
        DeleteUserRequest deleteUserRequest = DeleteUserRequest.builder()
                .accessToken(accessToken)
                .build();
        return cognitoClient.deleteUser(deleteUserRequest);
    }

    public AdminDeleteUserResponse deleteUserByAdmin(String username) {
        AdminDeleteUserRequest deleteUserRequest = AdminDeleteUserRequest.builder()
                .userPoolId(userPoolId)
                .username(username)
                .build();
        return cognitoClient.adminDeleteUser(deleteUserRequest);
    }

    public GlobalSignOutResponse signOut(String accessToken) {
        if (accessToken != null) {
            GlobalSignOutRequest globalSignOutRequest = GlobalSignOutRequest.builder()
                    .accessToken(accessToken)
                    .build();
            return cognitoClient.globalSignOut(globalSignOutRequest);
        }
        return null;
    }

    public String getUserSub(String idToken) throws InvalidIdTokenException {
        try {
            return CognitoJwtParser.getUserSub(idToken, userPoolId, region);
        }  catch (Exception e) {
            throw new InvalidIdTokenException("Expired/Invalid JWT");
        }
    }

    public boolean isAccessTokenValid(String accessToken) {
        try {
            GetUserRequest request = GetUserRequest.builder()
                    .accessToken(accessToken)
                    .build();
            cognitoClient.getUser(request);
            return true;
        } catch (CognitoIdentityProviderException e) {
            return false; // Token is revoked or invalid
        }
    }

    public boolean isEmailVerified(String email) {
        AdminGetUserRequest userRequest = AdminGetUserRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .build();

        AdminGetUserResponse userResponse = cognitoClient.adminGetUser(userRequest);
        return userResponse.userAttributes().stream()
                .anyMatch(attr -> attr.name().equals("email_verified") && attr.value().equals("true"));
    }

    public void setEmailVerified(String email) {
        AdminUpdateUserAttributesRequest updateRequest = AdminUpdateUserAttributesRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .userAttributes(
                        AttributeType.builder()
                                .name("email_verified")
                                .value("true")
                                .build()
                ).build();
        cognitoClient.adminUpdateUserAttributes(updateRequest);
    }

    public void resetLegacyUserPassword(String email, String password) {
        AdminSetUserPasswordRequest setUserPassword = AdminSetUserPasswordRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .permanent(true)
                .password(password)
                .build();
        cognitoClient.adminSetUserPassword(setUserPassword);
    }

    public void resetLegacyUserFullName(String email, String firstName, String lastName) {
        AdminUpdateUserAttributesRequest updateRequest = AdminUpdateUserAttributesRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .userAttributes(
                        AttributeType.builder()
                                .name("given_name")
                                .value(firstName)
                                .build(),
                        AttributeType.builder()
                                .name("family_name")
                                .value(lastName)
                                .build()
                ).build();
        cognitoClient.adminUpdateUserAttributes(updateRequest);
    }

    public GenerateDataKeyResponse generateKmsKey() {
        GenerateDataKeyRequest generateDataKeyRequest = GenerateDataKeyRequest.builder()
                .keyId(cmkKeyId)
                .keySpec("AES_256")
                .build();
        return kmsClient.generateDataKey(generateDataKeyRequest);
    }

    public DecryptResponse decryptCiphertextBlob(byte[] ciphertextBlob) {
        if (ciphertextBlob == null || ciphertextBlob.length == 0) {
            throw new IllegalArgumentException("CiphertextBlob cannot be null or empty");
        }
        SdkBytes sdkCiphertextBlob = SdkBytes.fromByteArray(ciphertextBlob);
        DecryptRequest decryptRequest = DecryptRequest.builder()
                .ciphertextBlob(sdkCiphertextBlob)
                .build();
        return kmsClient.decrypt(decryptRequest);
    }

    public String getFileFromS3(String key) {
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(r ->
                r.getObjectRequest(b -> b.bucket(bucketName).key(key))
                        .signatureDuration(Duration.ofSeconds(30))
        );
        return presignedRequest.url().toString();
    }

    public String uploadFile(String uniqueIdentifier, MultipartFile file) throws IOException {
        if(file == null || file.isEmpty()) {
            return null;
        }
        if (!fileUtils.isValidImageFile(file)) {
            throw new IllegalArgumentException("Invalid file format. Only JPEG, PNG, GIF, BMP and HEIC allowed.");
        }
        String key = uniqueIdentifier + "_" + file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        UploadRequest uploadRequest = UploadRequest.builder()
                .putObjectRequest(putObjectRequest)
                .requestBody(AsyncRequestBody.fromBytes(file.getBytes()))
                .build();

        try {
            CompletedUpload uploadResult = s3TransferManager.upload(uploadRequest).completionFuture().join();
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public void deleteFile(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("File key cannot be null or empty");
        }

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try {
            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }
}
