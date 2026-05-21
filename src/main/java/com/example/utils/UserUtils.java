package com.example.utils;

import com.example.exception.user.InvalidIdTokenException;
import com.example.exception.user.InvalidTokenException;
import com.example.exception.user.UserNotFoundException;
import com.example.model.entity.Users;
import com.example.repository.UsersRepository;
import com.example.service.AwsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserUtils {
    private final UsersRepository usersRepository;
    private final AwsService awsService;

    public Users getLoggedInUser(String userSub) {
        Users loggedInUser = null;
        if (userSub != null) {
            loggedInUser = usersRepository.findByUserSub(userSub)
                    .orElseThrow(() -> new UserNotFoundException(String.format("User %s not found", userSub)));
        }
        return loggedInUser;
    }

    public String extractUserSub(String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return null; // guest
            }
            String idToken = authorizationHeader.replace("Bearer ", "");
            return awsService.getUserSub(idToken);
        } catch(InvalidIdTokenException e) {
            throw new InvalidTokenException("Failed to parse ID token");
        }
    }
}
