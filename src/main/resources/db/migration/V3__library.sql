-- Literature management: projects, categories (outline), references (Titel) and knowledge items
-- (Wissenselemente: quotes, paraphrases, comments, own thoughts).

CREATE TABLE project (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id       BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name           VARCHAR(200) NOT NULL,
    description    TEXT,
    citation_style VARCHAR(30)  NOT NULL DEFAULT 'APA7',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_project_owner ON project (owner_id);

-- Hierarchical outline of a project (e.g. the chapters of a thesis).
CREATE TABLE category (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT       NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    parent_id  BIGINT REFERENCES category (id) ON DELETE CASCADE,
    name       VARCHAR(200) NOT NULL,
    position   INT          NOT NULL DEFAULT 0
);

CREATE INDEX ix_category_project ON category (project_id);
CREATE INDEX ix_category_parent ON category (parent_id);

CREATE TABLE reference_entry (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id      BIGINT       NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    type            VARCHAR(30)  NOT NULL,
    title           TEXT         NOT NULL,
    subtitle        TEXT,
    year            VARCHAR(20),
    container_title TEXT,         -- journal, edited volume or proceedings title
    publisher       VARCHAR(300), -- publisher, university or institution
    place           VARCHAR(200),
    edition         VARCHAR(50),
    series          VARCHAR(300),
    volume          VARCHAR(50),
    issue           VARCHAR(50),
    pages           VARCHAR(50),
    genre           VARCHAR(100), -- e.g. "Masterarbeit", "Dissertation", "Arbeitspapier Nr. 12"
    isbn            VARCHAR(40),
    doi             VARCHAR(200),
    url             TEXT,
    accessed_on     DATE,
    abstract_text   TEXT,
    keywords        TEXT,
    notes           TEXT,
    citation_key    VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_reference_citation_key UNIQUE (project_id, citation_key)
);

CREATE INDEX ix_reference_project ON reference_entry (project_id);
CREATE INDEX ix_reference_doi ON reference_entry (project_id, lower(doi));

-- Authors and editors, ordered.
CREATE TABLE reference_contributor (
    reference_entry_id BIGINT       NOT NULL REFERENCES reference_entry (id) ON DELETE CASCADE,
    position           INT          NOT NULL,
    role               VARCHAR(10)  NOT NULL,
    last_name          VARCHAR(200) NOT NULL,
    first_name         VARCHAR(200),
    PRIMARY KEY (reference_entry_id, position)
);

CREATE TABLE reference_category (
    reference_entry_id BIGINT NOT NULL REFERENCES reference_entry (id) ON DELETE CASCADE,
    category_id        BIGINT NOT NULL REFERENCES category (id) ON DELETE CASCADE,
    PRIMARY KEY (reference_entry_id, category_id)
);

CREATE INDEX ix_reference_category_category ON reference_category (category_id);

CREATE TABLE knowledge_item (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id         BIGINT      NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    reference_entry_id BIGINT REFERENCES reference_entry (id) ON DELETE CASCADE,
    type               VARCHAR(20) NOT NULL,
    core_statement     VARCHAR(300),
    text               TEXT        NOT NULL,
    page_range         VARCHAR(50),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_knowledge_project ON knowledge_item (project_id);
CREATE INDEX ix_knowledge_reference ON knowledge_item (reference_entry_id);

CREATE TABLE knowledge_item_category (
    knowledge_item_id BIGINT NOT NULL REFERENCES knowledge_item (id) ON DELETE CASCADE,
    category_id       BIGINT NOT NULL REFERENCES category (id) ON DELETE CASCADE,
    PRIMARY KEY (knowledge_item_id, category_id)
);

CREATE INDEX ix_knowledge_category_category ON knowledge_item_category (category_id);

-- Permission to use the literature management; granted to all regular users and admins.
INSERT INTO permission (name) VALUES ('library:use');
INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p WHERE r.name IN ('USER', 'ADMIN') AND p.name = 'library:use';
