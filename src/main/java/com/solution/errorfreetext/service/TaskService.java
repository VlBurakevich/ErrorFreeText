package com.solution.errorfreetext.service;

import com.solution.errorfreetext.dto.CreateTaskRequest;
import com.solution.errorfreetext.dto.CreateTaskResponse;
import com.solution.errorfreetext.dto.TaskResponse;
import com.solution.errorfreetext.repository.TaskRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final YandexSpellerService yandexSpellerService;
    private static int CHUNK_LIMIT = 10000;

    public CreateTaskResponse createTask(CreateTaskRequest createTaskRequest) {

    }

    public TaskResponse getTaskById(UUID id) {

    }
}
