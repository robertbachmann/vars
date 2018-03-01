package com.github.robertbachmann.vars;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Var<T> implements Publisher<T>  {
    public static <T> SimpleVar<T> createSimple(T val) {
        return new SimpleVar<>(val);
    }

    public static <A,T> Var<T> create(Var<A> var1, Function<A, T> function) {
        return new DerivedVar<>(var1, function);
    }

    public static <A,B,T> Var<T> create(Var<A> var1, Var<B> var2, BiFunction<A, B, T> function) {
        return new DerivedVar<>(var1, var2, function);
    }

    final List<SubscriptionManager> managers = new CopyOnWriteArrayList<>();
    volatile T volatileValue;

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        SubscriptionManager manager = new SubscriptionManager(subscriber);
        managers.add(manager);
        subscriber.onSubscribe(manager);
        manager.subscribeDone();
        manager.offer(this.volatileValue);
    }

    public abstract T getValue();

    final class SubscriptionManager implements Subscription {
        private final ThreadLocalFlag recursionFlag = new ThreadLocalFlag();
        private final AtomicLong bound = new AtomicLong();
        private final AtomicBoolean isNew = new AtomicBoolean(true);
        private final Subscriber<? super T> subscriber;

        SubscriptionManager(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            // TODO proper handling of exceptions

            if (n < 1) {
                throw new IllegalArgumentException("n < 1");
            }

            for (;;) {
                final long currentBound = bound.get();
                if (currentBound == Long.MAX_VALUE)
                    break;
                final long newBound = n != Long.MAX_VALUE ? safeAdd(currentBound, n) : Long.MAX_VALUE;
                if (bound.compareAndSet(currentBound, newBound)) {
                    break;
                }
            }

            if (recursionFlag.isSet()) {
                return;
            }

            if (isNew.compareAndSet(true,false)) {
                dispatch(volatileValue);
            }
        }

        long safeAdd(long a, long b) {
            final long result = a + b;

            if (!((a ^ b) < 0 | (a ^ result) >= 0)) {
                return Long.MAX_VALUE; // overflow
            } else {
                return result;
            }
        }

        boolean dispatch(T value) {
            if (recursionFlag.isSet() || bound.get() < 1) {
                return false;
            }

            for (;;) { // try decrement 1
                long n = bound.get();

                if (n == Long.MAX_VALUE) {
                    break; // no tracking necessary
                }

                if (n == 0) {
                    return false; // abort dispatch
                }

                long nextN = n - 1;
                if (bound.compareAndSet(n, nextN))
                    break;
            }

            try {
                recursionFlag.set();
                subscriber.onNext(value);
            }
            finally {
                recursionFlag.clear();
            }

            return true;
        }

        @Override
        public void cancel() {
            bound.set(0);
            managers.remove(this);
        }

        void offer(T value) {
            if (dispatch(value)) {
                isNew.compareAndSet(true,false);
            }
        }

        void subscribeDone() {
            recursionFlag.clear();
        }
    }

    final static class ThreadLocalFlag extends ThreadLocal<Object> {
        private static final Object MARKER = new Object[0];

        @Override
        protected Object initialValue() {
            return MARKER;
        }

        public boolean isSet() {
            return get() != null;
        }

        public void set() {
            set(MARKER);
        }

        public void clear() {
            set(null);
        }
    }
}
