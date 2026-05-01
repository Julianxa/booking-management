package com.example.service;

import com.example.constant.Enums;
import com.example.exception.InvalidEmailPasswordException;
import com.example.exception.ResourceNotFoundException;
import com.example.exception.UnverifiedEmailException;
import com.example.model.dto.*;
import com.example.model.entity.Users;
import com.example.model.entity.UsersLoginHistory;
import com.example.repository.UsersLoginHistoryRepository;
import com.example.repository.UsersRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GlobalSignOutResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;

import java.time.LocalDateTime;
import static com.example.constant.Enums.UserStatus.CONFIRMED;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final AwsService awsService;
    private final UserService userService;
    private final UsersRepository usersRepository;
    private final UsersLoginHistoryRepository usersLoginHistoryRepository;

    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO, HttpServletRequest httpServletRequest) throws UnverifiedEmailException, InvalidEmailPasswordException, BadRequestException {
        Users user = usersRepository.findByEmail(loginRequestDTO.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        try {
            InitiateAuthResponse res = awsService.login(loginRequestDTO);

            insertLoginActivity(user, httpServletRequest, Enums.LoginActivityStatus.SUCCESS);

            LoginResponseDTO loginResponseDTO = new LoginResponseDTO();

            if(res.authenticationResult() != null) {
                loginResponseDTO.setEmail(loginRequestDTO.getEmail());
                loginResponseDTO.setSession(res.session());
                loginResponseDTO.setAccessToken(res.authenticationResult().accessToken());
                loginResponseDTO.setIdToken(res.authenticationResult().idToken());
                loginResponseDTO.setRefreshToken(res.authenticationResult().refreshToken());
                loginResponseDTO.setExpiresIn(res.authenticationResult().expiresIn());
                loginResponseDTO.setMessage("Login successfully");
                loginResponseDTO.setTimestamp(LocalDateTime.now());
            }
            return loginResponseDTO;
        } catch (NotAuthorizedException e) {
            insertLoginActivity(user, httpServletRequest, Enums.LoginActivityStatus.FAILED);
            throw new InvalidEmailPasswordException("Incorrect username or password");
        }
    }

    public TokenRenewalResponseDTO refresh(TokenRenewalRequestDTO tokenRenewalRequestDTO) {
        Users user = usersRepository.findByEmailAndStatus(tokenRenewalRequestDTO.getEmail(), CONFIRMED)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user != null) {
            tokenRenewalRequestDTO.setUserSub(user.getUserSub());
            InitiateAuthResponse res = awsService.refresh(tokenRenewalRequestDTO);
            TokenRenewalResponseDTO tokenRenewalResponseDTO = new TokenRenewalResponseDTO();
            tokenRenewalResponseDTO.setEmail(tokenRenewalResponseDTO.getEmail());
            tokenRenewalResponseDTO.setAccessToken(res.authenticationResult().accessToken());
            tokenRenewalResponseDTO.setExpiresIn(res.authenticationResult().expiresIn());
            tokenRenewalResponseDTO.setIdToken(res.authenticationResult().idToken());
            tokenRenewalResponseDTO.setTokenType(res.authenticationResult().tokenType());
            tokenRenewalResponseDTO.setMessage("Tokens refreshed successfully");
            tokenRenewalResponseDTO.setTimestamp(LocalDateTime.now());
            return tokenRenewalResponseDTO;
        } else {
            throw new RuntimeException("Failed to renew token");
        }
    }

    public LogoutResponseDTO logout(String accessToken) {
        GlobalSignOutResponse globalSignOutResponse = awsService.signOut(accessToken);
        if (globalSignOutResponse.sdkHttpResponse().statusCode() == 200) {
            return LogoutResponseDTO.builder()
                    .message("Logout successfully: " + globalSignOutResponse.sdkHttpResponse().statusText())
                    .timestamp(LocalDateTime.now())
                    .build();
        } else {
            throw new RuntimeException("Logout failed: " + globalSignOutResponse.sdkHttpResponse().statusText());
        }
    }

    public void insertLoginActivity(Users user, HttpServletRequest httpServletRequest, Enums.LoginActivityStatus loginActivityStatus) {
        String ipAddress = httpServletRequest.getHeader("X-Forwarded-For");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = ipAddress.split(",")[0].trim();
        } else {
            ipAddress = httpServletRequest.getHeader("X-Real-IP") != null ? httpServletRequest.getHeader("X-Real-IP") : httpServletRequest.getRemoteAddr();
        }
        LocalDateTime loginAt = LocalDateTime.now();
        String userAgent = httpServletRequest.getHeader("User-Agent");

        if(loginActivityStatus == Enums.LoginActivityStatus.SUCCESS)
            user.setLastLoginAt(loginAt);

        UsersLoginHistory loginActivity = UsersLoginHistory.builder()
                .user(user)
                .loginAt(loginAt)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status(loginActivityStatus)
                .build();
        usersLoginHistoryRepository.save(loginActivity);
    }
}
