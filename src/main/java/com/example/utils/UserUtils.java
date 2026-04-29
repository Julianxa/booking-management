package com.example.utils;

import com.example.exception.InvalidIdTokenException;
import com.example.exception.ResourceNotFoundException;
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
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
        return loggedInUser;
    }

    public String extractUserSub(String authorizationHeader) throws InvalidIdTokenException {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null; // guest
        }
        String idToken = authorizationHeader.replace("Bearer ", "");
        return awsService.getUserSub(idToken);
    }
}
