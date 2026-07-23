package com.solution.errorfreetext.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solution.errorfreetext.dto.CreateTaskRequest;
import com.solution.errorfreetext.dto.CreateTaskResponse;
import com.solution.errorfreetext.dto.TaskResponse;
import com.solution.errorfreetext.entity.TaskStatus;
import com.solution.errorfreetext.exception.ErrorCode;
import com.solution.errorfreetext.exception.GlobalExceptionHandler;
import com.solution.errorfreetext.exception.TaskNotFoundException;
import com.solution.errorfreetext.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    private static final UUID TEST_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Nested
    @DisplayName("GET /tasks/{id}")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Должен возвращать 200 OK и TaskResponse при успешном поиске")
        void getTaskById_ShouldReturnTask_WhenTaskExists() throws Exception {
            TaskResponse mockResponse = new TaskResponse(TaskStatus.COMPLETED, "Sample text", "COMPLETED");
            when(taskService.getTaskById(TEST_ID)).thenReturn(mockResponse);

            mockMvc.perform(get("/tasks/{id}", TEST_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correctedText").value("Sample text"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));

            verify(taskService).getTaskById(TEST_ID);
        }

        @Test
        @DisplayName("Должен возвращать 404 NOT FOUND при поиске несуществующего таска")
        void getTaskById_ShouldReturn404_WhenTaskNotFound() throws Exception {
            TaskNotFoundException exception = new TaskNotFoundException(TEST_ID);

            when(taskService.getTaskById(TEST_ID)).thenThrow(exception);

            mockMvc.perform(get("/tasks/{id}", TEST_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.TASK_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.errorMessage").value("Task with id: " + TEST_ID + " not found"))
                    .andExpect(jsonPath("$.path").value("/tasks/" + TEST_ID));

            verify(taskService).getTaskById(TEST_ID);
        }

        @Test
        @DisplayName("Должен возвращать 400 BAD REQUEST при некорректном UUID в пути")
        void getTaskById_ShouldReturn400_WhenInvalidUuidFormat() throws Exception {
            mockMvc.perform(get("/tasks/{id}", "not-a-valid-uuid")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_ARGUMENTS.getCode()))
                    .andExpect(jsonPath("$.errorMessage").value("Invalid value 'not-a-valid-uuid' for parameter 'id'"));

            verifyNoInteractions(taskService);
        }
    }

    @Nested
    @DisplayName("POST /tasks")
    class CreateTaskTests {

        @Test
        @DisplayName("Должен возвращать 201 CREATED и CreateTaskResponse при валидном запросе")
        void createTask_ShouldReturn201_WhenRequestIsValid() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest("Hello world", "EN");
            CreateTaskResponse mockResponse = new CreateTaskResponse(TEST_ID);

            when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(mockResponse);

            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(TEST_ID.toString()));

            verify(taskService).createTask(any(CreateTaskRequest.class));
        }

        @Test
        @DisplayName("Должен возвращать 400 BAD REQUEST, если тело запроса не проходит @Valid")
        void createTask_ShouldReturn400_WhenValidationFails() throws Exception {
            CreateTaskRequest invalidRequest = new CreateTaskRequest("", "INVALID_LANG");

            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_ARGUMENTS.getCode()));

            verifyNoInteractions(taskService);
        }

        @Test
        @DisplayName("Должен возвращать 400 BAD REQUEST при передаче сломанного JSON")
        void createTask_ShouldReturn400_WhenMalformedJson() throws Exception {
            String malformedJson = "{\"text\": \"hello\", ";

            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_ARGUMENTS.getCode()))
                    .andExpect(jsonPath("$.errorMessage").value("Request body is malformed or invalid"));

            verifyNoInteractions(taskService);
        }
    }
}
