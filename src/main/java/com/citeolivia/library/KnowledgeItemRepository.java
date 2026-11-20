package com.citeolivia.library;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

interface KnowledgeItemRepository extends ListCrudRepository<KnowledgeItem, Long> {

    Optional<KnowledgeItem> findByIdAndProjectId(Long id, Long projectId);

    List<KnowledgeItem> findAllByProjectIdAndReferenceEntryIdOrderByCreatedAtAsc(Long projectId, Long referenceEntryId);

    List<KnowledgeItem> findAllByProjectIdOrderByCreatedAtAsc(Long projectId);
}
