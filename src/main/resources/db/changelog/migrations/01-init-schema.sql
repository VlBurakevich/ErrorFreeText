CREATE TABLE tasks
(
    id             UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    original_text  TEXT        NOT NULL,
    corrected_text TEXT,
    language    VARCHAR(10)  NOT NULL,
    status         VARCHAR(20) NOT NULL,
    error_message  TEXT,
    created_at     TIMESTAMPTZ   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL,
    CONSTRAINT chk_tasks_language CHECK (language IN ('RU', 'EN'))
);

CREATE TABLE task_chunks
(
    id                   UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    task_id              UUID NOT NULL,
    sequence_number      INT  NOT NULL,
    chunk_text           TEXT NOT NULL,
    corrected_chunk_text TEXT,
    CONSTRAINT fk_chunks_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE
);

CREATE INDEX idx_tasks_status_created ON tasks (status, created_at);