package com.citeolivia.user;

import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** Part of the {@link AppUser} aggregate: reference to a {@link Role} by id. */
@Table("user_role")
record UserRole(@Column("role_id") AggregateReference<Role, Long> role) {}
