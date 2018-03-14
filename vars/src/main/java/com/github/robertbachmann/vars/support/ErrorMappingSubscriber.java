package com.github.robertbachmann.vars.support;

import org.reactivestreams.Subscriber;

public interface ErrorMappingSubscriber<T> extends Subscriber<T> {
    long mapError(Throwable t, long n);
}
