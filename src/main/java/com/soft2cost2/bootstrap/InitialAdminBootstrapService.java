package com.soft2cost2.bootstrap;

import com.soft2cost2.admin.service.RoleService;
import com.soft2cost2.admin.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Profile("dev")
@Service
public class InitialAdminBootstrapService {

    private final UserService userService;
    private final RoleService roleService;
    private final Environment environment;
    private final String adminLoginId;
    private final String adminName;
    private final String companyCode;

    public InitialAdminBootstrapService(
            UserService userService,
            RoleService roleService,
            Environment environment,
            @Value("${soft2cost2.bootstrap.admin-login-id:ADMIN}") String adminLoginId,
            @Value("${soft2cost2.bootstrap.admin-name:시스템관리자}") String adminName,
            @Value("${soft2cost2.bootstrap.company-code:SOFT2COST}") String companyCode
    ) {
        this.userService = userService;
        this.roleService = roleService;
        this.environment = environment;
        this.adminLoginId = adminLoginId;
        this.adminName = adminName;
        this.companyCode = companyCode;
    }

    @Transactional
    public Long initialize() {
        userService.requireActiveCompany(companyCode);
        roleService.requireBootstrapMenuStructure();

        Long userId = userService.findUserIdByLoginId(adminLoginId);
        if (userId == null) {
            String configured = environment.getProperty(
                    "soft2cost2.bootstrap.admin-password", ""
            );
            if (configured.isBlank()) {
                throw new IllegalStateException(
                        "최초 ADMIN 생성용 secret이 설정되지 않았습니다."
                );
            }
            char[] rawPassword = configured.toCharArray();
            try {
                userId = userService.createInitialAdmin(
                        adminLoginId, adminName, rawPassword, companyCode
                );
            } finally {
                Arrays.fill(rawPassword, '\0');
            }
        } else {
            userService.requireExistingAdminConforms(
                    userId, adminLoginId, companyCode
            );
        }

        Long roleId = roleService.ensureSystemAdminRole();
        roleService.ensureAssignmentAndPermissions(userId, roleId);
        roleService.requireNoBlockingAdminOverrides(userId);
        return userId;
    }
}