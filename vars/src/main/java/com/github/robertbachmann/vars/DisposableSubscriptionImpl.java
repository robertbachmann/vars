package com.github.robertbachmann.vars;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Internal implementation class
 */
final class DisposableSubscriptionImpl<T> implements DisposableSubscription, Subscriber<T> {
    private final AtomicReference<Subscription> subscriptionAtomicReference = new AtomicReference<>(null);
    private final Consumer<? super T> consumer;
    private final Consumer<? super Throwable> errorConsumer;
    private final Runnable completeAction;

    DisposableSubscriptionImpl(Consumer<? super T> consumer, Consumer<? super Throwable> errorConsumer, Runnable completeAction) {
        this.consumer = consumer;
        this.errorConsumer = errorConsumer;
        this.completeAction = completeAction;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (!subscriptionAtomicReference.compareAndSet(null, s)) {
            throw new AssertionError("onSubscribe called more than once");
        }
        s.request(1);
    }

    @Override
    public void onNext(T t) {
        if (consumer != null) {
            consumer.accept(t);
        }

        Subscription subscription = subscriptionAtomicReference.get();
        if (subscription != null) {
            subscription.request(1);
        }
    }

    @Override
    public void onError(Throwable t) {
        doDispose(false);
        if (errorConsumer != null) {
            errorConsumer.accept(t);
        }
    }

    @Override
    public void onComplete() {
        doDispose(false);
        if (completeAction != null) {
            completeAction.run();
        }
    }

    @Override
    public void dispose() {
        doDispose(true);
    }

    private void doDispose(boolean performCancel) {
        Subscription subscription = subscriptionAtomicReference.get();
        if (subscription == null) {
            return; // we are done
        }

        if (!subscriptionAtomicReference.compareAndSet(subscription, null)) {
            return; // some other Thread preempted us, we are done
        }

        if (performCancel) {
            subscription.cancel();
        }
    }

    @Override
    public boolean isDisposed() {
        return subscriptionAtomicReference.get() == null;
    }
}
