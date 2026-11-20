package com.citeolivia.library;

import org.springframework.data.relational.core.mapping.Table;

/** Assignment of a reference to a {@link Category} (by id – categories are a separate aggregate). */
@Table("reference_category")
record ReferenceCategory(Long categoryId) {}
