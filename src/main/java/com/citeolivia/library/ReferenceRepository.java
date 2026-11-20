package com.citeolivia.library;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

interface ReferenceRepository extends ListCrudRepository<Reference, Long> {

    Optional<Reference> findByIdAndProjectId(Long id, Long projectId);

    List<Reference> findAllByProjectId(Long projectId);

    boolean existsByProjectIdAndCitationKey(Long projectId, String citationKey);
}
