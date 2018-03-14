package com.github.robertbachmann.vars;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Reactive variable holder.
 *
 * @param <T>
 */
public class Var<T> implements Publisher<T>, Supplier<T> {
    final InternalPublisher<T> impl;

    public static <T> Var<T> eager(Var<T> var) { // TODO: remove
        requireNonNull(var, "var");
        if (var.isLazy()) {
            return create(var::getLastValue, var);
        }
        return var;
    }

    public static <T> SimpleVar<T> valueOf(T val) {
        return new SimpleVar<>(val);
    }

    public static <A, T> Var<T> map(Var<A> var1, Function<A, T> function) {
        requireNonNull(var1, "var1");
        requireNonNull(function, "function");
        return create(() -> function.apply(var1.getLastValue()), var1);
    }

    public static <A, T> Var<T> lazyMap(Var<A> var1, Function<A, T> function) {
        requireNonNull(var1, "var1");
        requireNonNull(function, "function");
        return createLazy(() -> function.apply(var1.getLastValue()), var1);
    }

    public static <A, B, T> Var<T> combine(Var<A> var1, Var<B> var2, BiFunction<A, B, T> function) {
        requireNonNull(var1, "var1");
        requireNonNull(var2, "var2");
        requireNonNull(function, "function");
        return create(() -> function.apply(var1.getLastValue(), var2.getLastValue()), var1, var2);
    }

    public static <A, B, T> Var<T> lazyCombine(Var<A> var1, Var<B> var2, BiFunction<A, B, T> function) {
        requireNonNull(var1, "var1");
        requireNonNull(var2, "var2");
        requireNonNull(function, "function");
        return createLazy(() -> function.apply(var1.getLastValue(), var2.getLastValue()), var1, var2);
    }

    private static <T> Var<T> create(Supplier<T> supplier, Publisher<?>... vars) {
        return new Var<>(false, supplier, vars);
    }

    private static <T> Var<T> createLazy(Supplier<T> supplier, Publisher<?>... vars) {
        return new Var<>(true, supplier, vars);
    }

    public final Throwable getThrowable() {
        return impl.getThrowable();
    }

    public final T getLastValue() {
        return impl.getValue();
    }

    public final T get() {
        if (isLazy()) {
            impl.requestUpstream(1);
        }
        return impl.getValue();
    }

    @Override
    public final void subscribe(Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        impl.subscribe(subscriber);
    }

    private DisposableSubscription doSubscribe(Consumer<? super T> consumer, Consumer<? super Throwable> errorConsumer,
                                               Runnable completeAction) {
        DisposableSubscriptionImpl<T> disposableSubscription =
                new DisposableSubscriptionImpl<>(consumer, errorConsumer, completeAction);
        impl.subscribe(disposableSubscription);
        return disposableSubscription;
    }

    public final DisposableSubscription subscribe(Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        return doSubscribe(consumer, null, null);
    }

    public final DisposableSubscription subscribe(Consumer<? super T> consumer, Consumer<? super Throwable> errorConsumer) {
        Objects.requireNonNull(consumer, "consumer");
        Objects.requireNonNull(errorConsumer, "errorConsumer");
        return doSubscribe(consumer, errorConsumer, null);
    }

    public final DisposableSubscription subscribe(Consumer<? super T> consumer, Consumer<? super Throwable> errorConsumer,
                                                  Runnable completeAction) {
        Objects.requireNonNull(consumer, "consumer");
        Objects.requireNonNull(errorConsumer, "errorConsumer");
        Objects.requireNonNull(completeAction, "completeAction");
        return doSubscribe(consumer, errorConsumer, completeAction);
    }

    public final boolean isLazy() {
        return impl.isLazy();
    }

    Var(InternalPublisher<T> publisher) {
        this.impl = publisher;
    }

    protected Var(boolean lazy, Supplier<T> supplier, Publisher<?>[] vars) {
        this.impl = new InternalDerivedValuePublisher<>(lazy, supplier, vars);
    }
}
