package com.bambi.service.config;

import com.bambi.service.common.error.ApiException;
import com.bambi.service.common.error.ErrorCode;
import com.bambi.service.user.Role;
import com.bambi.service.user.RoleRepository;
import com.bambi.service.user.User;
import com.bambi.service.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 계정 seed (P0 합의안).
 * 서버 시작 시 ADMIN_EMAIL / ADMIN_PASSWORD 가 모두 있고, 해당 계정이 없으면 ADMIN 을 생성한다.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminSeeder(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.admin.email:}") String adminEmail,
                       @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.info("[AdminSeeder] ADMIN_EMAIL/PASSWORD 미설정 → seed 건너뜀");
            return;
        }
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("[AdminSeeder] 이미 존재하는 관리자({}) → seed 건너뜀", adminEmail);
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR, "ADMIN 권한 seed 가 없습니다."));
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR, "USER 권한 seed 가 없습니다."));

        User admin = new User(adminEmail, passwordEncoder.encode(adminPassword), "관리자");
        admin.addRole(adminRole);
        admin.addRole(userRole);
        userRepository.save(admin);
        log.info("[AdminSeeder] 관리자 계정 생성 완료: {}", adminEmail);
    }
}
