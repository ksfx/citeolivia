package com.citeolivia.user;

import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

interface UserRepository extends ListCrudRepository<AppUser, Long> {

    boolean existsByEmail(String email);

    @Query("""
            SELECT u.id, u.email, u.display_name, u.enabled, u.created_at,
                   COALESCE(string_agg(r.name, ', ' ORDER BY r.name), '') AS roles
            FROM app_user u
            LEFT JOIN user_role ur ON ur.user_id = u.id
            LEFT JOIN role r ON r.id = ur.role_id
            GROUP BY u.id
            ORDER BY u.created_at
            """)
    List<UserSummary> findAllSummaries();
}
