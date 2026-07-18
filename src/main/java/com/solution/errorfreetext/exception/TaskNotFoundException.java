package com.solution.errorfreetext.exception;

import java.util.UUID;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(UUID taskId) {
        super(taskId.toString());
    }//TODO rewrite
}
