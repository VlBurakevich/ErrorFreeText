package com.solution.errorfreetext.service;

import com.solution.errorfreetext.dto.CreateTaskRequest;
import com.solution.errorfreetext.dto.CreateTaskResponse;
import com.solution.errorfreetext.dto.TaskResponse;
import com.solution.errorfreetext.entity.Task;
import com.solution.errorfreetext.entity.TaskChunk;
import com.solution.errorfreetext.entity.TaskStatus;
import com.solution.errorfreetext.exception.TaskNotFoundException;
import com.solution.errorfreetext.mapper.TaskMapper;
import com.solution.errorfreetext.repository.TaskChunkRepository;
import com.solution.errorfreetext.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskChunkRepository taskChunkRepository;
    private final TaskMapper taskMapper;
    private final TaskTextSplitter taskTextSplitter;

    @Transactional
    public CreateTaskResponse createTask(CreateTaskRequest createTaskRequest) {
        Task task = taskMapper.mapToNewTask(createTaskRequest);
        task.setChunks(taskTextSplitter.split(task));
        task = taskRepository.save(task);

        log.info("Task id:{} successfully created", task.getId());
        return new CreateTaskResponse(task.getId());
    }

    public List<UUID> findTasksIdsToProcess() {
        log.info("fetching task id with status created");
        List<UUID> ids = taskRepository.findIdsByStatus(TaskStatus.CREATED);

        log.info("found {} tasks ready for process", ids.size());
        return ids;
    }

    public List<TaskChunk> getChunksOrdered(UUID taskId) {
        return taskChunkRepository.findAllByTaskIdOrderBySequenceNumber(taskId);
    }

    public TaskResponse getTaskById(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        return taskMapper.mapToResponse(task);
    }

    @Transactional
    public Task startTaskProcessing(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (task.getStatus() != TaskStatus.CREATED) {
            log.warn("Task {} already processing, current status: {}", taskId, task.getStatus());
            return null;
        }

        task.setStatus(TaskStatus.IN_PROGRESS);
        return taskRepository.save(task);
    }

    @Transactional
    public void completeTask(UUID taskId, String finalCorrectedText) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.error("Critical: Task {} not found on completion", taskId);
                    return new TaskNotFoundException(taskId);
                });

        task.setCorrectedText(finalCorrectedText);
        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task);
        log.info("Task {} processed successfully", taskId);
    }

    @Transactional
    public void failTask(UUID taskId, String errorMessage) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    log.error("Critical: Task {} not found on fail", taskId);
                    return new TaskNotFoundException(taskId);
                });

        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage(errorMessage);
        taskRepository.save(task);
        log.warn("Task {} marked as FAILED: {}", taskId, errorMessage);
    }
}
