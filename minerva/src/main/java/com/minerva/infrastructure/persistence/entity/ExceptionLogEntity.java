package com.minerva.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exception_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExceptionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exception_type", nullable = false)
    private String exceptionType;

    @Column(name = "message")
    private String message;

    @Column(name = "cause_type")
    private String causeType;

    @Column(name = "cause_message")
    private String causeMessage;

    @Column(name = "stack_trace")
    private String stackTrace;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
