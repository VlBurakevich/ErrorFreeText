package com.solution.errorfreetext.repository;

import com.solution.errorfreetext.entity.TaskChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskChunkRepository extends JpaRepository<TaskChunk, UUID> {

    List<TaskChunk> findAllByTaskIdOrderBySequenceNumber(UUID taskId);
}
