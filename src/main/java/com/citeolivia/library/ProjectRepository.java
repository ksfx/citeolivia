package com.citeolivia.library;

import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

interface ProjectRepository extends ListCrudRepository<Project, Long> {

    List<Project> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
