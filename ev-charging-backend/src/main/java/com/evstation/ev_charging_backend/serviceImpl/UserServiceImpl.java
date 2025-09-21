package com.evstation.ev_charging_backend.serviceImpl;

import org.springframework.stereotype.Service;

import com.evstation.ev_charging_backend.dto.RegisterRequest;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.service.UserService;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User registerUser(RegisterRequest request) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // later we’ll encode this
        user.setRole(request.getRole());
        return userRepository.save(user);
    }
}
