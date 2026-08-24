package com.pipelinecrm.bootstrap;

import com.pipelinecrm.application.port.out.PasswordHasher;
import com.pipelinecrm.application.port.out.UserRepository;
import com.pipelinecrm.domain.identity.Email;
import com.pipelinecrm.domain.identity.PersonName;
import com.pipelinecrm.domain.identity.UserId;
import com.pipelinecrm.domain.identity.UserRole;
import com.pipelinecrm.domain.user.User;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoDataSeeder implements ApplicationRunner {

    static final String MANAGER_EMAIL = "manager@pipelinecrm.demo";
    static final String SALES_EMAIL = "sales@pipelinecrm.demo";
    static final String DEMO_PASSWORD = "password";

    private final UserRepository users;
    private final PasswordHasher hasher;

    public DemoDataSeeder(UserRepository users, PasswordHasher hasher) {
        this.users = users;
        this.hasher = hasher;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.findByEmail(Email.of(MANAGER_EMAIL)).isPresent()) {
            return;
        }
        users.save(User.register(
                UserId.generate(),
                Email.of(MANAGER_EMAIL),
                PersonName.of("Manager"),
                UserRole.MANAGER,
                hasher.hash(DEMO_PASSWORD)));
        users.save(User.register(
                UserId.generate(),
                Email.of(SALES_EMAIL),
                PersonName.of("Sales"),
                UserRole.SALES,
                hasher.hash(DEMO_PASSWORD)));
    }
}
