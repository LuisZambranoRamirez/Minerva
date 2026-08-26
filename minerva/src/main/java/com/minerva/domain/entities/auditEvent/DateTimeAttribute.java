package com.minerva.domain.entities.auditEvent;

import com.minerva.domain.exceptions.NullValueException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeAttribute extends Attribute<String> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DateTimeAttribute(String name, LocalDateTime attributeValue) throws NullValueException {
        super(
                name,
                attributeValue == null
                        ? null
                        : attributeValue.format(DATE_TIME_FORMATTER)
        );
    }
}