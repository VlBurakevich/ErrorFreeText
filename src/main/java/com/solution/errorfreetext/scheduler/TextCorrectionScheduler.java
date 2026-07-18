package com.solution.errorfreetext.scheduler;

import com.solution.errorfreetext.service.TaskProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TextCorrectionScheduler {
    private final TaskProcessor taskProcessor;

    @Scheduled(fixedDelayString = "${app.scheduler.delay-ms:5000}")
    public void processPendingTasks() {
        log.trace("Scheduler scanning fon new text correction tasks...");

        try {
            taskProcessor.executeProcessing();
        } catch (Exception e) {
            log.error("Critical error inside core scheduler loop", e);
        }
    }
}
