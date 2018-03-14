package com.github.robertbachmann.vars.tck;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@SuppressWarnings("WeakerAccess")
class HookProcessor<T> implements Processor<T, T> {
    private Subscription upStream;
    private Subscriber<? super T> downStream;

    public HookProcessor(Publisher<T> src) {
        src.subscribe(this);
    }

    @Override
    public final void subscribe(Subscriber<? super T> s) {
        this.downStream = s;
        this.downStream.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                upStream.request(n);
            }

            @Override
            public void cancel() {
                upStream.cancel();
                afterCancel();
                HookProcessor.this.downStream = null;
            }
        });
    }

    @Override
    public void onSubscribe(Subscription s) {
        upStream = s;
    }

    @Override
    public void onNext(T t) {
        downStream.onNext(t);
        afterNext(t);
    }

    @Override
    public void onError(Throwable t) {
        downStream.onError(t);
        afterError(t);
    }

    @Override
    public void onComplete() {
        downStream.onComplete();
        afterComplete();
    }

    protected void afterCancel() {
    }

    protected void afterComplete() {
    }

    protected void afterNext(T t) {
    }

    protected void afterError(Throwable t) {
    }
}