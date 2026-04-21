package com.whatsnext.authapi.service;

import com.whatsnext.authapi.dto.response.UserProfileResponse;
import com.whatsnext.authapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getProfile(String email) {
        return userRepository.findByEmail(email)
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
