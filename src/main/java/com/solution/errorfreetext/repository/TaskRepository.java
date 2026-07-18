package com.solution.errorfreetext.repository;

import com.solution.errorfreetext.entity.Task;
import com.solution.errorfreetext.entity.TaskStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    @Query("Select t.id FROM Task t Where t.status = :status")
    List<UUID> findIdsByStatus(@Param("status")TaskStatus taskStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Task t WHERE t.id = :id")
    Optional<Task> findByIdForProcessing(@Param("id") UUID id);
}
