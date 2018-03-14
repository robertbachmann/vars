package com.github.robertbachmann.vars;

import java.util.Objects;

/**
 * A Var that can be set.
 *
 * @param <T>
 */
public final class SimpleVar<T> extends Var<T> {
    SimpleVar(T value) {
        super(new InternalPublisher.InternalValuePublisher<>(Objects.requireNonNull(value, "value")));
    }

    public void setValue(T newValue) {
        Objects.requireNonNull(newValue, "newValue");
        ((InternalPublisher.InternalValuePublisher<T>) impl).setValue(newValue);
    }
}
