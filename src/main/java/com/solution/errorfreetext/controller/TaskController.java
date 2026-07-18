package com.solution.errorfreetext.controller;

import com.solution.errorfreetext.dto.CreateTaskRequest;
import com.solution.errorfreetext.dto.CreateTaskResponse;
import com.solution.errorfreetext.dto.TaskResponse;
import com.solution.errorfreetext.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateTaskResponse createTask(
            @Valid @RequestBody CreateTaskRequest createTaskRequest
    ) {
        return taskService.createTask(createTaskRequest);
    }

    @GetMapping("/{id}")
    public TaskResponse getTaskById(
            @PathVariable UUID id
    ) {
        return taskService.getTaskById(id);
    }
}
