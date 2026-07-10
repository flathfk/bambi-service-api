package com.bambi.service.auth.dto;

import com.bambi.service.user.Role;
import com.bambi.service.user.User;

import java.util.Set;
import java.util.stream.Collectors;

/** 사용자 노출용 요약 DTO. Entity 를 직접 응답에 노출하지 않는다. */
public record UserSummary(
        Long id,
        String email,
        String displayName,
        Set<String> roles) {

    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
    }
}
