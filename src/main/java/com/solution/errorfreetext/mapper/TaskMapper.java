package com.solution.errorfreetext.mapper;

import com.solution.errorfreetext.dto.CreateTaskRequest;
import com.solution.errorfreetext.dto.TaskResponse;
import com.solution.errorfreetext.entity.Task;
import com.solution.errorfreetext.entity.TaskStatus;
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
        task.setOriginalText(request.getText());
        task.setLanguageId(request.getLanguage() != null ? request.getLanguage().toUpperCase() : null);
        task.setStatus(TaskStatus.CREATED);

        return task;
    }

    public TaskResponse mapToResponse(Task task) {
        if (task == null) {
            return null;
        }

        TaskResponse taskResponse = new TaskResponse();
        taskResponse.setStatus(task.getStatus());

        if (task.getStatus() == TaskStatus.COMPLETED) {
            taskResponse.setCorrectedText(task.getCorrectedText());
        } else if (task.getStatus() == TaskStatus.FAILED) {
            taskResponse.setErrorMessage(task.getErrorMessage());
        }

        return taskResponse;
    }
}
