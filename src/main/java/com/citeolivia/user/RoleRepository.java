package com.citeolivia.user;

import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

interface RoleRepository extends ListCrudRepository<Role, Long> {

    Optional<Role> findByName(String name);
}
