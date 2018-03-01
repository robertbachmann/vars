package com.github.robertbachmann.vars;

public class SimpleVar<T> extends Var<T> {
    SimpleVar(T value) {
        this.volatileValue = value;
    }

    @Override
    public T getValue() {
        return this.volatileValue;
    }

    public void setValue(T newValue) {
        T oldValue = this.volatileValue;
        this.volatileValue = newValue;
        updateValue(oldValue, newValue);
    }

    private void updateValue(T oldValue, T newValue) {
        boolean different = (oldValue != null ? !oldValue.equals(newValue) : newValue != null);

        if (!different) {
            return;
        }

        for (SubscriptionManager manager : managers) {
            manager.offer(newValue);
        }
    }
}
