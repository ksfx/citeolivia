package com.citeolivia.library;

import org.springframework.data.relational.core.mapping.Table;

@Table("knowledge_item_category")
record KnowledgeItemCategory(Long categoryId) {}
