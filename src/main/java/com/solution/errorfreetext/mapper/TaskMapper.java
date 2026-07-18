package com.solution.errorfreetext.mapper;

import com.solution.errorfreetext.dto.CreateTaskRequest;
import com.solution.errorfreetext.dto.TaskResponse;
import com.solution.errorfreetext.entity.Task;
import com.solution.errorfreetext.entity.TaskStatus;
import com.solution.errorfreetext.entity.TextLanguage;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TaskMapper {
    public Task mapToNewTask(CreateTaskRequest request) {
        if (request == null) {
            return null;
        }

        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setOriginalText(request.text());
        task.setLanguage(request.language() != null ? TextLanguage.valueOf(request.language().toUpperCase()) : null);
        task.setStatus(TaskStatus.CREATED);

        return task;
    }

    public TaskResponse mapToResponse(Task task) {
        if (task == null) {
            return null;
        }

        return switch (task.getStatus()) {
            case COMPLETED -> new TaskResponse(TaskStatus.COMPLETED, task.getCorrectedText(), null);
            case FAILED -> new TaskResponse(TaskStatus.FAILED, null, task.getErrorMessage());
            default -> new TaskResponse(task.getStatus(), null, null);
        };
    }
}
