package com.evstation.ev_charging_backend.serviceImpl;

import com.evstation.ev_charging_backend.dto.AdminDashboardResponse;
import com.evstation.ev_charging_backend.dto.DashboardResponse;
import com.evstation.ev_charging_backend.dto.UserDashboardResponse;
import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.enums.Role;
import com.evstation.ev_charging_backend.repository.UserRepository;
import com.evstation.ev_charging_backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;

    @Override
    public DashboardResponse getDashboardData(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = user.getRole();

        switch (role) {
            case USER:
                return new DashboardResponse(
                        "User Dashboard",
                        new UserDashboardResponse(
                                user.getFirstName(),
                                user.getLastName(),
                                user.getEmail(),
                                user.getPhone(),
                                user.getRole().name(),
                                user.getCreatedAt()
                        )
                );

            case HOST:
                return new DashboardResponse(
                        "Host Dashboard",
                        new UserDashboardResponse(
                                user.getFirstName(),
                                user.getLastName(),
                                user.getEmail(),
                                user.getPhone(),
                                user.getRole().name(),
                                user.getCreatedAt()
                        )
                );

            case ADMIN:
                List<User> allUsers = userRepository.findAll();
                long totalUsers = allUsers.stream().filter(u -> u.getRole() == Role.USER).count();
                long totalHosts = allUsers.stream().filter(u -> u.getRole() == Role.HOST).count();
                long totalAdmins = allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count();

                return new DashboardResponse(
                        "Admin Dashboard",
                        new AdminDashboardResponse(totalUsers, totalHosts, totalAdmins)
                );

            default:
                return new DashboardResponse("Unknown Role", null);
        }
    }
}
