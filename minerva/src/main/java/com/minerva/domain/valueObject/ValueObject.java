package com.minerva.domain.valueObject;

import java.util.Objects;

import com.minerva.domain.exceptions.NullValueException;

public abstract class ValueObject<V> {
    private final V value;

    public ValueObject(V value) throws NullValueException {
        if (value == null) throw new NullValueException("El valor no puede ser nulo.");
        this.value = value;
    }

    public V getValue() {
        return value;
    }

    public String getClassName() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ValueObject<?> that = (ValueObject<?>) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
