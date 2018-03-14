package com.github.robertbachmann.vars.fugue;

import io.atlassian.fugue.Try;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class RecoveryStrategy<T> {
    static final RecoveryStrategy<?> NONE = new RecoveryStrategy<>();

    private final List<InternalExceptionHandler.HandlerMapping<T>> list = new ArrayList<>();

    public RecoveryStrategy<T> recover(final Function<? super Exception, T> fallbackValueFunction) {
        return recover(Exception.class, fallbackValueFunction);
    }

    @SuppressWarnings({"unchecked"})
    public <X extends Exception> RecoveryStrategy<T> recover(
            final Class<X> exceptionType,
            final Function<? super X, T> fallbackValueFunction) {

        Function<? super X, Try<T>> f = adapt(exceptionType, fallbackValueFunction);

        synchronized (list) {
            list.add(new InternalExceptionHandler.HandlerMapping<>(exceptionType, (Function<? super Exception, Try<T>>) f));
        }
        return this;
    }

    public RecoveryStrategy<T> recoverWith(
            final Function<? super Exception, Try<T>> f) {
        return recoverWith(Exception.class, f);
    }

    @SuppressWarnings("unchecked")
    public <X extends Exception> RecoveryStrategy<T> recoverWith(
            final Class<X> exceptionType, final Function<? super X, Try<T>> f) {
        synchronized (list) {
            list.add(new InternalExceptionHandler.HandlerMapping<>(exceptionType, (Function<? super Exception, Try<T>>) f));
        }
        return this;
    }

    private static <X extends Exception, T> Function<? super X, Try<T>> adapt(
            @SuppressWarnings("unused") final Class<X> exceptionType,
            final Function<? super X, T> fallbackValueFunction) {

        return (Function<X, Try<T>>) x -> {
            try {
                T result = fallbackValueFunction.apply(x);
                return Try.successful(result);
            } catch (Exception e) {
                return Try.failure(e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    InternalExceptionHandler.HandlerMapping<T>[] getArray() {
        synchronized (list) {
            return list.toArray(new InternalExceptionHandler.HandlerMapping[list.size()]);
        }
    }
}
