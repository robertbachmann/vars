package com.github.robertbachmann.vars.support;

import org.reactivestreams.Subscription;

public interface PeekingSubscription extends Subscription {
    boolean peek();
}
