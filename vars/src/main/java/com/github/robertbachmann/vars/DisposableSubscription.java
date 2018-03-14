package com.github.robertbachmann.vars;

public interface DisposableSubscription {
    void dispose();

    boolean isDisposed();
}
