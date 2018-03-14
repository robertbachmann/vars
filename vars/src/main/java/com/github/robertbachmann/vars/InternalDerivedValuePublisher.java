package com.github.robertbachmann.vars;

import com.github.robertbachmann.vars.support.ErrorMappingSubscriber;
import com.github.robertbachmann.vars.support.PeekingSubscription;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Internal implementation class
 */
final class InternalDerivedValuePublisher<T> extends InternalPublisher<T> {
    private final boolean lazy;
    private final Supplier<T> supplier;
    private final VarSubscriber[] upstreamSubscriptions;

    InternalDerivedValuePublisher(boolean lazy, Supplier<T> supplier, Publisher<?> publishers[]) {
        this.lazy = lazy;
        this.supplier = supplier;
        upstreamSubscriptions = new VarSubscriber[publishers.length];
        for (int i = 0; i < publishers.length; ++i) {
            upstreamSubscriptions[i] = createAndSubscribe(publishers[i], this);
        }
        for (int i = 0; !lazy && i < publishers.length; ++i) {
            upstreamSubscriptions[i].request(1);
        }
    }

    private static <A, T> VarSubscriber<A> createAndSubscribe(Publisher<A> publisher, InternalDerivedValuePublisher<T> self) {
        VarSubscriber<A> subscriber = new VarSubscriber<>(self);
        publisher.subscribe(subscriber);
        return subscriber;
    }

    private void reCalculate() {
        if (lazy) {
            for (VarSubscriber upstreamSubscription : upstreamSubscriptions) {
                if (upstreamSubscription.peek()) {
                    return;
                }
            }
        }


        T oldValue = getValue();
        T newValue;
        try {
            newValue = supplier.get();
        } catch (Throwable e) {
            updateThrowable(e);
            for (InternalDownStreamSubscription subscription : getDownStreamSubscriptions()) {
                subscription.offerError(e);
            }
            return;
        }

        updateValue(newValue);
        updateThrowable(null);

        if (!Objects.equals(oldValue, newValue)) {
            for (InternalDownStreamSubscription<T> subscription : getDownStreamSubscriptions()) {
                subscription.offerValue(newValue);
            }
        }
    }

    @Override
    boolean isLazy() {
        return lazy;
    }

    @Override
    protected void requestUpstream(long n) {
        for (VarSubscriber upstreamSubscription : upstreamSubscriptions) {
            upstreamSubscription.request(n);
        }
    }

    @Override
    protected boolean peekUpstream() {
        for (VarSubscriber upstreamSubscription : upstreamSubscriptions) {
            if (upstreamSubscription.peek()) {
                return true;
            }
        }
        return false;
    }

    private static final class VarSubscriber<V> implements ErrorMappingSubscriber<V> {
        private final InternalDerivedValuePublisher<?> publisher;
        private Subscription subscription;

        VarSubscriber(InternalDerivedValuePublisher<?> obj) {
            this.publisher = obj;
        }

        void request(long n) {
            subscription.request(n);
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
        }

        @Override
        public void onNext(V v) {
            publisher.reCalculate();
            if (!publisher.isLazy()) {
                subscription.request(1);
            }
        }

        @Override
        public void onError(Throwable t) {
            for (InternalDownStreamSubscription subscription : publisher.getDownStreamSubscriptions()) {
                subscription.offerError(t);
            }
        }

        @Override
        public void onComplete() {
            for (InternalDownStreamSubscription subscription : publisher.getDownStreamSubscriptions()) {
                subscription.complete();
            }
        }

        @Override
        public long mapError(Throwable t, long n) {
            for (InternalDownStreamSubscription subscription : publisher.getDownStreamSubscriptions()) {
                subscription.offerError(t);
            }
            return 0;
        }

        boolean peek() {
            return subscription instanceof PeekingSubscription && ((PeekingSubscription) subscription).peek();
        }
    }
}
