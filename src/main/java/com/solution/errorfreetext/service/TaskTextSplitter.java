package com.solution.errorfreetext.service;

import com.solution.errorfreetext.entity.Task;
import com.solution.errorfreetext.entity.TaskChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TaskTextSplitter {

    public List<TaskChunk> split(Task task) {
        String text = task.getOriginalText();

        if (text == null || text.isBlank()) {
            return List.of();
        }

        String[] words = text.trim().split("\\s+");
        List<TaskChunk> chunks = new ArrayList<>(words.length);

        for (int i = 0; i < words.length; i++) {
            TaskChunk chunk = new TaskChunk();
            chunk.setTask(task);
            chunk.setSequenceNumber(i + 1);
            chunk.setChunkText(words[i]);
            chunks.add(chunk);
        }

        return chunks;
    }
}
