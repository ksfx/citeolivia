package com.citeolivia.library;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

interface CategoryRepository extends ListCrudRepository<Category, Long> {

    List<Category> findAllByProjectIdOrderByPositionAscIdAsc(Long projectId);

    Optional<Category> findByIdAndProjectId(Long id, Long projectId);
}
