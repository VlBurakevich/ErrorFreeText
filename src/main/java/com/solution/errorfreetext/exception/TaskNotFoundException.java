package com.solution.errorfreetext.exception;

import java.util.UUID;

public class TaskNotFoundException extends AppException {
    public TaskNotFoundException(UUID taskId) {
        super(ErrorCode.TASK_NOT_FOUND, taskId);
    }
}
