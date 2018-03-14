package com.github.robertbachmann.vars.support;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public abstract class SplitProcessor<T, R> implements Processor<T, R>, ErrorMappingSubscriber<T> {
    private final Publisher<T> publisher;
    private Subscriber<? super R> downStream;
    private Subscription upStreamSubscription;

    protected SplitProcessor(Publisher<T> publisher) {
        this.publisher = publisher;
    }

    @Override
    public final void subscribe(Subscriber<? super R> s) {
        publisher.subscribe(this);
        downStream = s;
        downStream.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                upStreamSubscription.request(n);
            }

            @Override
            public void cancel() {
                upStreamSubscription.cancel();
            }
        });
    }

    @Override
    public final void onSubscribe(Subscription s) {
        this.upStreamSubscription = s;
    }

    @Override
    public final void onNext(T item) {
        R result = right(item);
        if (result != null) {
            downStream.onNext(result);
        }
    }

    @Override
    public final void onError(Throwable t) {
        downStream.onError(t);
    }

    @Override
    public final void onComplete() {
        downStream.onComplete();
    }

    @Override
    public final long mapError(Throwable t, long n) {
        if (downStream == null || n < 1) {
            return -1;
        }

        R output = left(t);
        if (output == null) {
            throw new IllegalStateException("mapError (left) returned null");
        }

        downStream.onNext(output);
        return 1;
    }

    /**
     * @return {@code R} or null
     */
    protected abstract R right(T value);

    /**
     * @return {@code R} or null
     */
    protected abstract R left(Throwable throwable);
}
