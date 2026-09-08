package com.minerva.domain.entities.auditEvent;

import java.util.List;
import java.util.Set;

public final class ArrayAttribute extends Attribute<List<Set<Attribute<?>>>> {
    public ArrayAttribute(String name, List<Set<Attribute<?>>> attributes) {
        super(name, attributes);
    }
}