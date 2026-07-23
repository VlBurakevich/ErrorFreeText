package com.solution.errorfreetext.service;

import com.solution.errorfreetext.entity.Task;
import com.solution.errorfreetext.entity.TaskChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TaskTextSplitter {

    private final int maxChunkSize;

    public TaskTextSplitter(
            @Value("${app.text-splitter.max-chunk-size:10000}") int maxChunkSize
    ) {
        this.maxChunkSize = maxChunkSize;
    }

    public List<TaskChunk> split(Task task) {
        String text = task.getOriginalText();
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<TaskChunk> chunks = new ArrayList<>();
        int length = text.length();
        int start = 0;
        int sequenceNumber = 1;

        while (start < length) {
            int end = start + maxChunkSize;

            if (end >= length) {
                end = length;
            } else {
                int lastSpace = text.lastIndexOf(' ', end - 1);

                if (lastSpace > start) {
                    end = lastSpace + 1;
                }
            }

            TaskChunk chunk = new TaskChunk();
            chunk.setTask(task);
            chunk.setSequenceNumber(sequenceNumber++);
            chunk.setChunkText(text.substring(start, end));
            chunks.add(chunk);

            start = end;
        }

        return chunks;
    }
}
