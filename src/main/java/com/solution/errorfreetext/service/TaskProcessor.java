package com.solution.errorfreetext.service;

import com.solution.errorfreetext.entity.Task;
import com.solution.errorfreetext.entity.TaskChunk;
import com.solution.errorfreetext.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskProcessor {
    private final TaskService taskService;
    private final YandexSpellerService yandexSpellerService;

    public void executeProcessing() {
        log.info("Fetching task IDs with status CREATED...");
        List<UUID> ids = taskService.findTasksIdsToProcess();
        log.info("Found {} tasks ready for processing", ids.size());
        for (UUID id : ids) {
            processTask(id);
        }
    }

    private void processTask(UUID taskId) {
        Task task = taskService.startTaskProcessing(taskId);
        if (task == null) {
            log.warn("Task {} is already being processing or not found, skipping", taskId);
            return;
        }

        List<TaskChunk> chunks = taskService.getChunksOrdered(taskId);
        if (chunks.isEmpty()) {
            log.warn("No chunks found for task {}", taskId);
            taskService.completeTask(taskId, task.getOriginalText());
            return;
        }

        StringBuilder correctedTextBuilder = new StringBuilder();
        try {
            for (TaskChunk chunk : chunks) {
                String correctChunk = yandexSpellerService.correctText(
                        chunk.getChunkText(),
                        task.getLanguage()
                );
                correctedTextBuilder.append(correctChunk);
            }

            taskService.completeTask(taskId, correctedTextBuilder.toString());
            log.info("Task{} successfully processed", taskId);
        } catch (AppException e) {
            log.error("Business error during task processing {}", taskId, e);
            taskService.failTask(taskId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during task processing {}", taskId, e);
            taskService.failTask(taskId, "An unexpected internal error occurred during text processing");
        }
    }
}
