package com.api_caich.service;

import com.api_caich.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    public List<Role> getAllRoles() {
        return Arrays.asList(Role.values());
    }

    public List<String> getAllRoleNames() {
        return Arrays.stream(Role.values())
                .map(Role::getNombre)
                .collect(Collectors.toList());
    }

    public List<String> getAllAuthorities() {
        return Arrays.stream(Role.values())
                .map(Role::getAuthority)
                .collect(Collectors.toList());
    }

    public boolean isValidRole(String roleName) {
        return Role.isValid(roleName);
    }

    public Role getRoleFromName(String roleName) {
        return Role.fromNombre(roleName);
    }

    // Roles disponibles para registro (excluye ADMIN)
    public List<String> getRegisterableRoles() {
        return Arrays.stream(Role.values())
                .filter(role -> role != Role.ADMINISTRADOR)
                .map(Role::getNombre)
                .collect(Collectors.toList());
    }
}