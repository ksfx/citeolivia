package com.citeolivia.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Role aggregate. Permissions are assigned to roles in the database (role_permission). */
@Table("role")
public class Role {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    @Id
    private final Long id;

    private final String name;

    Role(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
