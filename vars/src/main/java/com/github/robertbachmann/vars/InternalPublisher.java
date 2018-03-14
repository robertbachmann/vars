package com.github.robertbachmann.vars;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Internal implementation class
 */
abstract class InternalPublisher<T> implements Publisher<T> {
    private final List<InternalDownStreamSubscription<T>> downStreamSubscriptions = new CopyOnWriteArrayList<>();
    private final boolean isLazy = false;
    private volatile T volatileValue;
    private volatile Throwable volatileThrowable;

    @Override
    public final void subscribe(Subscriber<? super T> subscriber) {
        InternalDownStreamSubscription<T> subscription =
                new InternalDownStreamSubscription<>(this, subscriber, this.volatileThrowable);
        downStreamSubscriptions.add(subscription);
        subscriber.onSubscribe(subscription);
        subscription.subscribeDone();

        T currentValue = this.volatileValue;
        Throwable currentThrowable = this.volatileThrowable;

        if (currentThrowable != null) {
            subscription.offerError(currentThrowable);
        } else if (currentValue != null) {
            subscription.offerValue(currentValue);
        }
    }

    final T getValue() {
        return this.volatileValue;
    }

    final Throwable getThrowable() {
        return this.volatileThrowable;
    }

    final void updateValue(T newValue) {
        this.volatileValue = newValue;
    }

    final void updateThrowable(Throwable throwable) {
        this.volatileThrowable = throwable;
    }

    final void complete() {
        for (InternalDownStreamSubscription subscription : downStreamSubscriptions) {
            subscription.complete();
        }
    }

    final void cancelSubscription(InternalDownStreamSubscription<T> subscription) {
        downStreamSubscriptions.remove(subscription);
    }

    final List<InternalDownStreamSubscription<T>> getDownStreamSubscriptions() {
        return downStreamSubscriptions;
    }

    abstract boolean isLazy();

    protected abstract void requestUpstream(long n);

    protected abstract boolean peekUpstream();

    static final class InternalValuePublisher<T> extends InternalPublisher<T> {
        InternalValuePublisher(T t) {
            updateValue(t);
        }

        void setValue(T newValue) {
            T oldValue = getValue();
            updateValue(newValue);

            boolean different = (oldValue != null ? !oldValue.equals(newValue) : newValue != null);

            if (!different) {
                return;
            }

            for (InternalDownStreamSubscription<T> subscription : getDownStreamSubscriptions()) {
                subscription.offerValue(newValue);
            }
        }

        @Override
        boolean isLazy() {
            return false;
        }

        @Override
        protected void requestUpstream(long n) {
            // ignore
        }

        @Override
        protected boolean peekUpstream() {
            return false;
        }
    }
}
