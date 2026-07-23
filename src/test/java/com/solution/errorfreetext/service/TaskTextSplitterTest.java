package com.solution.errorfreetext.service;

import com.solution.errorfreetext.entity.Task;
import com.solution.errorfreetext.entity.TaskChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskTextSplitterTest {
    private TaskTextSplitter taskTextSplitter;

    @BeforeEach
    void setUp() {
        taskTextSplitter = new TaskTextSplitter(10);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", " \n \t "})
    @DisplayName("Должен возвращать пустой список чанков, если текст задачи null, пустой или из пробелов")
    void split_ShouldReturnEmptyList_WhenOriginalTextIsNullOrEmpty(String inputText) {
        Task task = new Task();
        task.setOriginalText(inputText);

        List<TaskChunk> chunks = taskTextSplitter.split(task);

        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
    }

    @Test
    @DisplayName("Должен возвращать один чанк, если длина текста меньше или равна MAX_CHUNK_SIZE")
    void split_ShouldReturnSingleChunk_WhenTextIsShorterThanMaxLimit() {
        taskTextSplitter = new TaskTextSplitter(100);

        Task task = new Task();
        task.setOriginalText("Короткий текст задачи");

        List<TaskChunk> chunks = taskTextSplitter.split(task);

        assertEquals(1, chunks.size());

        TaskChunk firstChunk = chunks.getFirst();
        assertEquals(1, firstChunk.getSequenceNumber());
        assertEquals("Короткий текст задачи", firstChunk.getChunkText());
        assertEquals(task, firstChunk.getTask());
    }

    @Test
    @DisplayName("Должен корректно делить текст по пробелу, не разрывая слова посередине")
    void split_ShouldSplitBySpace_WhenTextExceedsLimit() {
        Task task = new Task();
        task.setOriginalText("Привет мир всем");

        List<TaskChunk> chunks = taskTextSplitter.split(task);

        assertEquals(2, chunks.size());

        assertEquals(1, chunks.get(0).getSequenceNumber());
        assertEquals("Привет ", chunks.get(0).getChunkText());

        assertEquals(2, chunks.get(1).getSequenceNumber());
        assertEquals("мир всем", chunks.get(1).getChunkText());

        String restoredText = chunks.stream().map(TaskChunk::getChunkText).reduce("", String::concat);
        assertEquals(task.getOriginalText(), restoredText);
    }

    @Test
    @DisplayName("Должен резать жестко по лимиту, если слово длиннее MAX_CHUNK_SIZE (нет пробелов)")
    void split_ShouldHardSplit_WhenSingleWordExceedsLimit() {
        taskTextSplitter = new TaskTextSplitter(5);

        Task task = new Task();
        task.setOriginalText("1234567890");
        List<TaskChunk> chunks = taskTextSplitter.split(task);

        assertEquals(2, chunks.size());

        assertEquals("12345", chunks.get(0).getChunkText());
        assertEquals("67890", chunks.get(1).getChunkText());
    }

    @Test
    @DisplayName("Должен корректно проставлять связи с Task и нумерацию sequenceNumber для нескольких чанков")
    void split_ShouldSetTaskAndSequenceNumbersCorrectly() {
        Task task = new Task();
        task.setOriginalText("one two three");

        List<TaskChunk> chunks = taskTextSplitter.split(task);

        for (int i = 0; i < chunks.size(); i++) {
            TaskChunk chunk = chunks.get(i);
            assertEquals(task, chunk.getTask(), "Каждый чанк должен ссылаться на исходную таску");
            assertEquals(i + 1, chunk.getSequenceNumber(), "Нумерация должна идти строго по порядку с 1");
        }
    }
}
