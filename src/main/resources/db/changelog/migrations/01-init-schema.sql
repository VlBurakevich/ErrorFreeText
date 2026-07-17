CREATE TABLE languages
(
    id   VARCHAR(2) PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE tasks
(
    id             UUID PRIMARY KEY,
    original_text  TEXT        NOT NULL,
    corrected_text TEXT,
    language_id    VARCHAR(2)  NOT NULL,
    status         VARCHAR(20) NOT NULL,
    error_message  TEXT,
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP   NOT NULL,
    CONSTRAINT fk_tasks_language FOREIGN KEY (language_id) REFERENCES languages (id)
);

CREATE TABLE task_chunks
(
    id                   UUID PRIMARY KEY,
    task_id              UUID NOT NULL,
    sequence_number      INT  NOT NULL,
    chunk_text           TEXT NOT NULL,
    corrected_chunk_text TEXT,
    CONSTRAINT fk_chunks_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE
);

CREATE INDEX idx_tasks_status_created ON tasks (status, created_at);