package com.minerva.application.service;

import com.minerva.application.exceptions.ConflictException;
import com.minerva.application.port.driven.AbuseAttemptRepository;

import java.time.Duration;
import java.time.LocalDateTime;

public class AbuseMitigationService {
    private static final Duration REGISTRATION_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(10);
    private static final int MAX_REGISTRATION_ATTEMPTS = 5;
    private static final int MAX_LOGIN_ATTEMPTS = 10;

    private final AbuseAttemptRepository repository;

    public AbuseMitigationService(AbuseAttemptRepository repository) {
        this.repository = repository;
    }

    public void assertRegistrationAllowed(String clientKey, String username) {
        assertAllowed("CUSTOMER_REGISTER_IP", normalize(clientKey), REGISTRATION_WINDOW, MAX_REGISTRATION_ATTEMPTS);
        assertAllowed("CUSTOMER_REGISTER_USERNAME", normalize(username), REGISTRATION_WINDOW, MAX_REGISTRATION_ATTEMPTS);
    }

    public void recordRegistration(String clientKey, String username, boolean successful) {
        LocalDateTime now = LocalDateTime.now();
        repository.record("CUSTOMER_REGISTER_IP", normalize(clientKey), successful, now);
        repository.record("CUSTOMER_REGISTER_USERNAME", normalize(username), successful, now);
    }

    public void assertLoginAllowed(String clientKey, String username) {
        assertAllowed("LOGIN_IP", normalize(clientKey), LOGIN_WINDOW, MAX_LOGIN_ATTEMPTS);
        assertAllowed("LOGIN_USERNAME", normalize(username), LOGIN_WINDOW, MAX_LOGIN_ATTEMPTS);
    }

    public void recordLogin(String clientKey, String username, boolean successful) {
        LocalDateTime now = LocalDateTime.now();
        repository.record("LOGIN_IP", normalize(clientKey), successful, now);
        repository.record("LOGIN_USERNAME", normalize(username), successful, now);
    }

    private void assertAllowed(String action, String subjectKey, Duration window, int maxAttempts) {
        LocalDateTime since = LocalDateTime.now().minus(window);
        long recentAttempts = repository.countRecent(action, subjectKey, since);
        if (recentAttempts >= maxAttempts) {
            throw new ConflictException("Demasiados intentos. Esperá unos minutos e intentá nuevamente.");
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return "unknown";
        String normalized = value.trim().toLowerCase();
        return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
    }
}