package com.solution.errorfreetext.service;

import com.solution.errorfreetext.entity.Task;
import com.solution.errorfreetext.entity.TaskChunk;
import com.solution.errorfreetext.entity.TextLanguage;
import com.solution.errorfreetext.exception.AppException;
import com.solution.errorfreetext.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskProcessorTest {
    @Mock
    private TaskService taskService;

    @Mock
    private YandexSpellerService yandexSpellerService;

    @InjectMocks
    private TaskProcessor taskProcessor;

    @Nested
    @DisplayName("Тесты оркестрации и цикла обработки (executeProcessing)")
    class ExecuteProcessingTests {

        @Test
        @DisplayName("executeProcessing: должен корректно обработать все найденные задачи")
        void executeProcessing_ShouldProcessAllFoundTasks() {
            UUID taskId1 = UUID.randomUUID();
            UUID taskId2 = UUID.randomUUID();
            List<UUID> taskIds = List.of(taskId1, taskId2);

            Task task1 = new Task();
            task1.setId(taskId1);
            task1.setLanguage(TextLanguage.RU);

            Task task2 = new Task();
            task2.setId(taskId2);
            task2.setLanguage(TextLanguage.EN);

            TaskChunk chunk1 = new TaskChunk();
            chunk1.setChunkText("Привет");

            TaskChunk chunk2 = new TaskChunk();
            chunk2.setChunkText("Hello");

            when(taskService.findTasksIdsToProcess()).thenReturn(taskIds);
            when(taskService.startTaskProcessing(taskId1)).thenReturn(task1);
            when(taskService.startTaskProcessing(taskId2)).thenReturn(task2);

            when(taskService.getChunksOrdered(taskId1)).thenReturn(List.of(chunk1));
            when(taskService.getChunksOrdered(taskId2)).thenReturn(List.of(chunk2));

            when(yandexSpellerService.correctText("Привет", TextLanguage.RU)).thenReturn("Привет");
            when(yandexSpellerService.correctText("Hello", TextLanguage.EN)).thenReturn("Hello");

            taskProcessor.executeProcessing();

            verify(taskService).findTasksIdsToProcess();
            verify(taskService).completeTask(taskId1, "Привет");
            verify(taskService).completeTask(taskId2, "Hello");
        }

        @Test
        @DisplayName("executeProcessing: не должен ничего делать, если список задач для обработки пуст")
        void executeProcessing_ShouldDoNothing_WhenNoTasksFound() {
            when(taskService.findTasksIdsToProcess()).thenReturn(Collections.emptyList());

            taskProcessor.executeProcessing();

            verify(taskService).findTasksIdsToProcess();
            verify(taskService, never()).startTaskProcessing(any());
            verify(taskService, never()).getChunksOrdered(any());
        }

        @Test
        @DisplayName("executeProcessing: падение обработки одной задачи не должно прерывать обработку остальных")
        void executeProcessing_ShouldContinueProcessing_WhenOneTaskFails() {
            UUID failedTaskId = UUID.randomUUID();
            UUID successTaskId = UUID.randomUUID();

            Task failedTask = new Task();
            failedTask.setId(failedTaskId);
            failedTask.setLanguage(TextLanguage.RU);

            Task successTask = new Task();
            successTask.setId(successTaskId);
            successTask.setLanguage(TextLanguage.RU);

            TaskChunk chunk1 = new TaskChunk();
            chunk1.setChunkText("Ошибка");

            TaskChunk chunk2 = new TaskChunk();
            chunk2.setChunkText("Успех");

            when(taskService.findTasksIdsToProcess()).thenReturn(List.of(failedTaskId, successTaskId));
            when(taskService.startTaskProcessing(failedTaskId)).thenReturn(failedTask);
            when(taskService.startTaskProcessing(successTaskId)).thenReturn(successTask);

            when(taskService.getChunksOrdered(failedTaskId)).thenReturn(List.of(chunk1));
            when(taskService.getChunksOrdered(successTaskId)).thenReturn(List.of(chunk2));

            when(yandexSpellerService.correctText("Ошибка", TextLanguage.RU))
                    .thenThrow(new RuntimeException("Network error"));
            when(yandexSpellerService.correctText("Успех", TextLanguage.RU))
                    .thenReturn("Успех");

            taskProcessor.executeProcessing();

            verify(taskService).failTask(eq(failedTaskId), anyString());
            verify(taskService).completeTask(successTaskId, "Успех");
        }
    }

    @Nested
    @DisplayName("Тесты логики обработки отдельной задачи (processTask)")
    class ProcessTaskTests {

        @Test
        @DisplayName("processTask: должен объединить исправленные чанки и успешно завершить задачу")
        void processTask_ShouldCorrectChunksAndCompleteTask_WhenMultipleChunksExist() {
            UUID taskId = UUID.randomUUID();
            Task task = new Task();
            task.setId(taskId);
            task.setLanguage(TextLanguage.RU);

            TaskChunk chunk1 = new TaskChunk();
            chunk1.setChunkText("Превед ");

            TaskChunk chunk2 = new TaskChunk();
            chunk2.setChunkText("медвед");

            when(taskService.findTasksIdsToProcess()).thenReturn(List.of(taskId));
            when(taskService.startTaskProcessing(taskId)).thenReturn(task);
            when(taskService.getChunksOrdered(taskId)).thenReturn(List.of(chunk1, chunk2));

            when(yandexSpellerService.correctText("Превед ", TextLanguage.RU)).thenReturn("Привет ");
            when(yandexSpellerService.correctText("медвед", TextLanguage.RU)).thenReturn("медведь");

            taskProcessor.executeProcessing();

            verify(taskService).completeTask(taskId, "Привет медведь");
            verify(taskService, never()).failTask(any(), any());
        }

        @Test
        @DisplayName("processTask: должен пропустить обработку, если startTaskProcessing вернул null")
        void processTask_ShouldSkip_WhenTaskAlreadyProcessingOrNotFound() {
            UUID taskId = UUID.randomUUID();

            when(taskService.findTasksIdsToProcess()).thenReturn(List.of(taskId));
            when(taskService.startTaskProcessing(taskId)).thenReturn(null);

            taskProcessor.executeProcessing();

            verify(taskService).startTaskProcessing(taskId);
            verify(taskService, never()).getChunksOrdered(any());
            verify(taskService, never()).completeTask(any(), any());
            verify(taskService, never()).failTask(any(), any());
        }

        @Test
        @DisplayName("processTask: должен завершить задачу с оригинальным текстом, если список чанков пуст")
        void processTask_ShouldCompleteWithOriginalText_WhenChunksListIsEmpty() {
            UUID taskId = UUID.randomUUID();
            String originalText = "Оригинальный текст без чанков";

            Task task = new Task();
            task.setId(taskId);
            task.setOriginalText(originalText);

            when(taskService.findTasksIdsToProcess()).thenReturn(List.of(taskId));
            when(taskService.startTaskProcessing(taskId)).thenReturn(task);
            when(taskService.getChunksOrdered(taskId)).thenReturn(Collections.emptyList());

            taskProcessor.executeProcessing();

            verify(taskService).completeTask(taskId, originalText);
            verify(yandexSpellerService, never()).correctText(any(), any());
            verify(taskService, never()).failTask(any(), any());
        }

        @Test
        @DisplayName("processTask: должен помечать задачу как FAILED с сообщением из AppException при ошибке бизнеса")
        void processTask_ShouldFailTaskWithAppExceptionMessage_WhenAppExceptionThrown() {
            UUID taskId = UUID.randomUUID();
            Task task = new Task();
            task.setId(taskId);
            task.setLanguage(TextLanguage.EN);

            TaskChunk chunk = new TaskChunk();
            chunk.setChunkText("Some text");

            String errorMessage = "Yandex Speller Service Unavailable";

            when(taskService.findTasksIdsToProcess()).thenReturn(List.of(taskId));
            when(taskService.startTaskProcessing(taskId)).thenReturn(task);
            when(taskService.getChunksOrdered(taskId)).thenReturn(List.of(chunk));

            when(yandexSpellerService.correctText("Some text", TextLanguage.EN))
                    .thenThrow(new TestAppException(errorMessage));

            taskProcessor.executeProcessing();

            verify(taskService).failTask(taskId, errorMessage);
            verify(taskService, never()).completeTask(any(), any());
        }

        @Test
        @DisplayName("processTask: должен помечать задачу как FAILED с общим сообщением при нетипичной ошибке")
        void processTask_ShouldFailTaskWithGenericMessage_WhenUnexpectedExceptionThrown() {
            UUID taskId = UUID.randomUUID();
            Task task = new Task();
            task.setId(taskId);
            task.setLanguage(TextLanguage.RU);

            TaskChunk chunk = new TaskChunk();
            chunk.setChunkText("Текст");

            when(taskService.findTasksIdsToProcess()).thenReturn(List.of(taskId));
            when(taskService.startTaskProcessing(taskId)).thenReturn(task);
            when(taskService.getChunksOrdered(taskId)).thenReturn(List.of(chunk));

            when(yandexSpellerService.correctText("Текст", TextLanguage.RU))
                    .thenThrow(new NullPointerException("Unexpected Null Pointer"));

            taskProcessor.executeProcessing();

            verify(taskService).failTask(
                    taskId,
                    "An unexpected internal error occurred during text processing"
            );
            verify(taskService, never()).completeTask(any(), any());
        }
    }

    private static class TestAppException extends AppException {
        private final String customMessage;

        public TestAppException(String message) {
            super(ErrorCode.INTERNAL_SERVER_ERROR);
            this.customMessage = message;
        }

        @Override
        public String getMessage() {
            return customMessage;
        }
    }
}
