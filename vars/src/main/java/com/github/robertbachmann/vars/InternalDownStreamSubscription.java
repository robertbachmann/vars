package com.github.robertbachmann.vars;

import com.github.robertbachmann.vars.support.ErrorMappingSubscriber;
import com.github.robertbachmann.vars.support.PeekingSubscription;
import org.reactivestreams.Subscriber;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Internal implementation class
 */
final class InternalDownStreamSubscription<T> implements PeekingSubscription {
    private final RecursionFlag recursionFlag = new RecursionFlag();
    private final AtomicLong max = new AtomicLong();
    private final AtomicInteger theState = new AtomicInteger(State.SUBSCRIPTION_PENDING);
    private final Subscriber<? super T> subscriber;
    private final InternalPublisher<T> publisher;
    private final AtomicReference<Throwable> throwableReference;

    InternalDownStreamSubscription(InternalPublisher<T> publisher, Subscriber<? super T> subscriber,
                                   Throwable currentThrowable) {
        this.publisher = publisher;
        this.subscriber = subscriber;
        this.throwableReference = new AtomicReference<>(currentThrowable);
    }

    @Override
    public void request(long n) {
        if (n < 1) {
            terminalError(new IllegalArgumentException("negative subscription request"));
            return;
        }

        for (; ; ) {
            final long currentBound = max.get();
            if (currentBound == Long.MAX_VALUE)
                break;
            final long newBound = n != Long.MAX_VALUE ? safeAdd(currentBound, n) : Long.MAX_VALUE;
            if (max.compareAndSet(currentBound, newBound)) {
                break;
            }
        }

        Throwable t = throwableReference.get();
        if (t != null) {
            offerError(t);
        }

        if (!recursionFlag.isSet() && theState.compareAndSet(State.ITEM_AVAILABLE, State.DEFAULT)) {
            if (!tryOnNext(publisher.getValue())) {
                theState.compareAndSet(State.DEFAULT, State.ITEM_AVAILABLE);
            }
        } else if (publisher.isLazy()) {
            publisher.requestUpstream(n);
        }
    }

    @Override
    public void cancel() {
        boolean wasActive = theState.compareAndSet(State.DEFAULT, State.TERMINAL)
                || theState.compareAndSet(State.ITEM_AVAILABLE, State.TERMINAL);

        if (wasActive) {
            max.set(0);
            publisher.cancelSubscription(this);
        }
    }

    private static long safeAdd(long a, long b) {
        final long result = a + b;

        if (!((a ^ b) < 0 | (a ^ result) >= 0)) {
            return Long.MAX_VALUE; // overflow
        } else {
            return result;
        }
    }

    private boolean tryDecrementMax(final long count) {
        for (long i = 0; i < count; ++i) {
            for (; ; ) { // try decrement 1
                long n = max.get();

                if (n == Long.MAX_VALUE) {
                    return true; // no tracking necessary
                }

                if (n == 0) {
                    return false; // abort
                }

                long nextN = n - 1;
                if (max.compareAndSet(n, nextN))
                    break;
            }
        }
        return true;
    }

    private boolean tryOnNext(T value) {
        if (max.get() < 1 || recursionFlag.isSet()) {
            return false;
        }

        if (!tryDecrementMax(1))
            return false;

        recursionFlag.set();
        try {
            subscriber.onNext(value);
            recursionFlag.clear();
        } catch (Throwable t) {
            recursionFlag.clear();
            terminalError(t);
            return false;
        }

        return true;
    }

    void offerValue(T value) {
        throwableReference.set(null);
        if (theState.compareAndSet(State.DEFAULT, State.DEFAULT) ||
                theState.compareAndSet(State.ITEM_AVAILABLE, State.DEFAULT)) {
            if (!tryOnNext(value)) {
                theState.compareAndSet(State.DEFAULT, State.ITEM_AVAILABLE);
            }
        }
    }

    void offerError(Throwable t) {
        throwableReference.set(t);
        if (!(subscriber instanceof ErrorMappingSubscriber)) {
            terminalError(t);
        } else {
            handleError(t);
        }
    }

    void subscribeDone() {
        theState.compareAndSet(State.SUBSCRIPTION_PENDING, State.DEFAULT);
    }

    private void handleError(Throwable t) {


        int state = theState.get();
        if (state != State.DEFAULT && state != State.ITEM_AVAILABLE) {
            return;
        }

        try {
            throwableReference.compareAndSet(t, null);

            long n = ((ErrorMappingSubscriber) subscriber).mapError(t, max.get());

            if (n < 0) {
                throwableReference.compareAndSet(null, t);
            } else {
                tryDecrementMax(n);
            }
        } catch (Throwable recoveryFailed) {
            recoveryFailed.addSuppressed(t);
            terminalError(recoveryFailed);
        }
    }

    private void terminalError(Throwable t) {
        boolean wasActive = theState.compareAndSet(State.DEFAULT, State.TERMINAL)
                || theState.compareAndSet(State.ITEM_AVAILABLE, State.TERMINAL);

        if (wasActive) {
            subscriber.onError(t);
        }
    }

    void complete() {
        boolean wasActive = theState.compareAndSet(State.DEFAULT, State.TERMINAL)
                || theState.compareAndSet(State.ITEM_AVAILABLE, State.TERMINAL);
        if (wasActive) {
            subscriber.onComplete();
        }
    }

    @Override
    public boolean peek() {
        boolean fresh = theState.get() == State.ITEM_AVAILABLE;
        return fresh || publisher.peekUpstream();
    }

    static final class State {
        static final int SUBSCRIPTION_PENDING = -2;
        static final int TERMINAL = -1;
        static final int DEFAULT = 0;
        static final int ITEM_AVAILABLE = 1;

        private State() {
        }
    }

    private static final class RecursionFlag extends ThreadLocal<Object> {
        boolean isSet() {
            return get() != null;
        }

        void set() {
            set(Boolean.TRUE);
        }

        void clear() {
            set(null);
        }

        @Override
        public String toString() {
            return isSet() ? "ThreadLocalFlag(true)" : "ThreadLocalFlag(false)";
        }
    }
}
