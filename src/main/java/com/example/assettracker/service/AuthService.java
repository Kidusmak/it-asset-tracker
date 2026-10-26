package com.example.assettracker.service;

import com.example.assettracker.dto.RegisterRequest;
import com.example.assettracker.dto.UserResponse;
import com.example.assettracker.entity.Role;
import com.example.assettracker.entity.User;
import com.example.assettracker.exception.ConflictException;
import com.example.assettracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username '" + username + "' is already taken");
        }
        User user = new User(username, passwordEncoder.encode(request.password()), Role.ROLE_STAFF);
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
