package com.minerva.application.port.driven;

import java.time.LocalDateTime;

public interface AbuseAttemptRepository {
    long countRecent(String action, String subjectKey, LocalDateTime since);
    void record(String action, String subjectKey, boolean successful, LocalDateTime registrationDate);
}