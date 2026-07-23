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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {
    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskChunkRepository taskChunkRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private TaskTextSplitter taskTextSplitter;

    @InjectMocks
    private TaskService taskService;

    @Nested
    @DisplayName("Тесты создания и получения задач")
    class CreateAndGetTests {

        @Test
        @DisplayName("createTask: должен успешно мапить, разбивать на чанки и сохранять задачу")
        void createTask_ShouldSaveTaskAndReturnResponse_WhenRequestIsValid() {
            CreateTaskRequest request = new CreateTaskRequest("Текст для проверки", "RU");
            UUID taskId = UUID.randomUUID();

            Task mappedTask = new Task();
            mappedTask.setOriginalText("Текст для проверки");

            TaskChunk chunk = new TaskChunk();
            chunk.setChunkText("Текст для проверки");
            List<TaskChunk> chunks = List.of(chunk);

            Task savedTask = new Task();
            savedTask.setId(taskId);
            savedTask.setOriginalText("Текст для проверки");
            savedTask.setChunks(chunks);

            when(taskMapper.mapToNewTask(request)).thenReturn(mappedTask);
            when(taskTextSplitter.split(mappedTask)).thenReturn(chunks);
            when(taskRepository.save(mappedTask)).thenReturn(savedTask);

            CreateTaskResponse response = taskService.createTask(request);

            assertNotNull(response);
            assertEquals(taskId, response.id());

            verify(taskMapper).mapToNewTask(request);
            verify(taskTextSplitter).split(mappedTask);
            verify(taskRepository).save(mappedTask);
        }

        @Test
        @DisplayName("getTaskById: должен возвращать TaskResponse, если задача найдена")
        void getTaskById_ShouldReturnTaskResponse_WhenTaskExists() {
            UUID taskId = UUID.randomUUID();
            Task task = new Task();
            task.setId(taskId);

            TaskResponse expectedResponse = new TaskResponse(
                    TaskStatus.COMPLETED,
                    "Исправленный текст",
                    null
            );

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.mapToResponse(task)).thenReturn(expectedResponse);

            TaskResponse actualResponse = taskService.getTaskById(taskId);

            assertEquals(expectedResponse, actualResponse);
            verify(taskRepository).findById(taskId);
            verify(taskMapper).mapToResponse(task);
        }

        @Test
        @DisplayName("getTaskById: должен выбрасывать TaskNotFoundException, если задача не найдена")
        void getTaskById_ShouldThrowTaskNotFoundException_WhenTaskDoesNotExist() {
            UUID taskId = UUID.randomUUID();
            when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

            assertThrows(
                    TaskNotFoundException.class,
                    () -> taskService.getTaskById(taskId)
            );

            verify(taskRepository).findById(taskId);
            verify(taskMapper, never()).mapToResponse(any());
        }

        @Test
        @DisplayName("findTasksIdsToProcess: должен возвращать список ID задач со статусом CREATED")
        void findTasksIdsToProcess_ShouldReturnListOfTaskIds() {
            List<UUID> expectedIds = List.of(UUID.randomUUID(), UUID.randomUUID());
            when(taskRepository.findIdsByStatus(TaskStatus.CREATED)).thenReturn(expectedIds);

            List<UUID> actualIds = taskService.findTasksIdsToProcess();

            assertEquals(expectedIds, actualIds);
            verify(taskRepository).findIdsByStatus(TaskStatus.CREATED);
        }

        @Test
        @DisplayName("getChunksOrdered: должен вызывать метод репозитория с сортировкой по sequenceNumber")
        void getChunksOrdered_ShouldReturnChunksInOrder() {
            UUID taskId = UUID.randomUUID();
            List<TaskChunk> expectedChunks = List.of(new TaskChunk(), new TaskChunk());

            when(taskChunkRepository.findAllByTaskIdOrderBySequenceNumber(taskId)).thenReturn(expectedChunks);

            List<TaskChunk> actualChunks = taskService.getChunksOrdered(taskId);

            assertEquals(expectedChunks, actualChunks);
            verify(taskChunkRepository).findAllByTaskIdOrderBySequenceNumber(taskId);
        }
    }

    @Nested
    @DisplayName("Тесты управления статусами задач (Start / Complete / Fail)")
    class StateManagementTests {

        @Test
        @DisplayName("startTaskProcessing: должен переводить задачу в IN_PROGRESS, если статус CREATED")
        void startTaskProcessing_ShouldSetStatusInProgress_WhenStatusIsCreated() {
            UUID taskId = UUID.randomUUID();
            Task task = new Task();
            task.setId(taskId);
            task.setStatus(TaskStatus.CREATED);

            when(taskRepository.findByIdForProcessing(taskId)).thenReturn(Optional.of(task));
            when(taskRepository.save(task)).thenAnswer(invocation -> invocation.getArgument(0));

            Task result = taskService.startTaskProcessing(taskId);

            assertNotNull(result);
            assertEquals(TaskStatus.IN_PROGRESS, result.getStatus());
            verify(taskRepository).findByIdForProcessing(taskId);
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("startTaskProcessing: должен возвращать null и не сохранять, если задача уже не в статусе CREATED")
        void startTaskProcessing_ShouldReturnNull_WhenTaskAlreadyProcessing() {
            UUID taskId = UUID.randomUUID();
            Task task = new Task();
            task.setId(taskId);
            task.setStatus(TaskStatus.IN_PROGRESS);

            when(taskRepository.findByIdForProcessing(taskId)).thenReturn(Optional.of(task));

            Task result = taskService.startTaskProcessing(taskId);

            assertNull(result);
            verify(taskRepository).findByIdForProcessing(taskId);
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("startTaskProcessing: должен выбрасывать TaskNotFoundException, если задача не найдена")
        void startTaskProcessing_ShouldThrowTaskNotFoundException_WhenTaskNotFound() {
            UUID taskId = UUID.randomUUID();
            when(taskRepository.findByIdForProcessing(taskId)).thenReturn(Optional.empty());

            assertThrows(
                    TaskNotFoundException.class,
                    () -> taskService.startTaskProcessing(taskId)
            );

            verify(taskRepository).findByIdForProcessing(taskId);
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("completeTask: должен сохранять исправленный текст и выставлять статус COMPLETED")
        void completeTask_ShouldSetCorrectedTextAndStatusCompleted() {
            UUID taskId = UUID.randomUUID();
            String correctedText = "Исправленный текст";

            Task task = new Task();
            task.setId(taskId);
            task.setStatus(TaskStatus.IN_PROGRESS);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

            taskService.completeTask(taskId, correctedText);

            assertEquals(correctedText, task.getCorrectedText());
            assertEquals(TaskStatus.COMPLETED, task.getStatus());
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("completeTask: должен выбрасывать TaskNotFoundException, если задача не найдена")
        void completeTask_ShouldThrowTaskNotFoundException_WhenTaskNotFound() {
            UUID taskId = UUID.randomUUID();
            when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

            assertThrows(
                    TaskNotFoundException.class,
                    () -> taskService.completeTask(taskId, "Текст")
            );

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("failTask: должен сохранять сообщение об ошибке и выставлять статус FAILED")
        void failTask_ShouldSetErrorMessageAndStatusFailed() {
            UUID taskId = UUID.randomUUID();
            String errorMessage = "Сервер Яндекса недоступен";

            Task task = new Task();
            task.setId(taskId);
            task.setStatus(TaskStatus.IN_PROGRESS);

            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

            taskService.failTask(taskId, errorMessage);

            assertEquals(errorMessage, task.getErrorMessage());
            assertEquals(TaskStatus.FAILED, task.getStatus());
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("failTask: должен выбрасывать TaskNotFoundException, если задача не найдена")
        void failTask_ShouldThrowTaskNotFoundException_WhenTaskNotFound() {
            UUID taskId = UUID.randomUUID();
            when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

            assertThrows(
                    TaskNotFoundException.class,
                    () -> taskService.failTask(taskId, "Ошибка")
            );

            verify(taskRepository, never()).save(any());
        }
    }
}
