package com.github.robertbachmann.vars;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

final class DerivedVar<T> extends Var<T> {
    private final InternalSubscriber<?>[] upstreamSubscriptions;
    private Supplier<T> supplier;

    <A> DerivedVar(Var<A> var1, Function<A, T> function) {
        supplier = () -> function.apply(var1.getValue());
        upstreamSubscriptions = new InternalSubscriber[]
                {
                        subscribe(var1)
                };
        subscribeAll();
    }

    <A, B> DerivedVar(Var<A> var1, Var<B> var2, BiFunction<A, B, T> function) {
        supplier = () -> function.apply(var1.getValue(), var2.getValue());
        upstreamSubscriptions = new InternalSubscriber[]
                {
                        subscribe(var1),
                        subscribe(var2),
                };
        subscribeAll();
    }

    private void subscribeAll() {
        for (InternalSubscriber<?> internalSubscriber : this.upstreamSubscriptions) {
            internalSubscriber.request();
        }
    }

    private <A> InternalSubscriber<A> subscribe(Var<A> var) {
        InternalSubscriber<A> subscriber = new InternalSubscriber<>(this);
        var.subscribe(subscriber);
        return subscriber;
    }

    private void reCalculate() {
        T oldValue = this.volatileValue;
        T newValue;
        try {
            this.volatileValue = newValue = supplier.get();
        }
        catch (Exception e) {
            // TODO proper handling
            e.printStackTrace();
            throw e;
        }

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

    @Override
    public T getValue() {
        return this.volatileValue;
    }

    private static final class InternalSubscriber<V> implements Subscriber<V> {
        private final DerivedVar<?> outer;
        private Subscription subscription;

        InternalSubscriber(DerivedVar<?> obj) {
            this.outer = obj;
        }

        void request() {
            subscription.request(1);
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
        }

        @Override
        public void onNext(V v) {
            outer.reCalculate();
            subscription.request(1);
        }

        @Override
        public void onError(Throwable t) {
            // TODO proper handling
            t.printStackTrace();
        }

        @Override
        public void onComplete() {
            // TODO proper handling
        }
    }
}
