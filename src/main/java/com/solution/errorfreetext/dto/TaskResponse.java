package com.solution.errorfreetext.dto;

import com.solution.errorfreetext.entity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private TaskStatus status;
    private String correctedText;
    private String errorMessage;
}
