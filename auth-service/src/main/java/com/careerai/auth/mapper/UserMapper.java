package com.careerai.auth.mapper;

import com.careerai.auth.domain.entity.Role;
import com.careerai.auth.domain.entity.User;
import com.careerai.auth.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

/**
 * Maps {@link User} entities to {@link UserResponse} DTOs, flattening roles to
 * their string names.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @org.mapstruct.Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToNames")
    UserResponse toUserResponse(User user);

    @Named("rolesToNames")
    default List<String> rolesToNames(Set<Role> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(role -> role.getName().name())
                .sorted()
                .toList();
    }
}
