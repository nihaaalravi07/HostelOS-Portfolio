package com.hostelos.security;

import com.hostelos.user.Role;

/** The authenticated caller, rebuilt from the JWT on every request. */
public record AuthUser(Long userId, String email, Role role) {}
