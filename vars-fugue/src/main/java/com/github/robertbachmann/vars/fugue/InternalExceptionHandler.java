package com.github.robertbachmann.vars.fugue;

import io.atlassian.fugue.Try;

import java.util.function.Function;

final class InternalExceptionHandler<T> {
    @SuppressWarnings("unchecked")
    private static final InternalExceptionHandler<?> NO_OP = new InternalExceptionHandler<>(new HandlerMapping[0]);
    private final HandlerMapping<T>[] mappings;

    @SuppressWarnings("unchecked")
    static <T> InternalExceptionHandler<T> create(RecoveryStrategy<T> recoveryStrategy) {
        if (RecoveryStrategy.NONE == recoveryStrategy) {
            return (InternalExceptionHandler<T>) NO_OP;
        }
        HandlerMapping<T>[] m = recoveryStrategy.getArray();
        if (m.length == 0) {
            return (InternalExceptionHandler<T>) NO_OP;
        } else {
            return new InternalExceptionHandler<>(m);
        }
    }

    private InternalExceptionHandler(HandlerMapping<T>[] mappings) {
        this.mappings = mappings;
    }

    Try<T> handle(Exception e) {
        Try<T> result = Try.failure(e);
        if (mappings == null) {
            return result;
        }

        for (HandlerMapping<T> handler : mappings) {
            if (!handler.exceptionType.isAssignableFrom(e.getClass())) {
                continue;
            }

            result = handler.function.apply(e);

            if (result.isSuccess()) {
                break;
            }
        }

        return result;
    }

    static final class HandlerMapping<T> {
        final Class<? extends Exception> exceptionType;
        final Function<? super Exception, Try<T>> function;

        HandlerMapping(Class<? extends Exception> exceptionType, Function<? super Exception, Try<T>> f) {
            this.exceptionType = exceptionType;
            this.function = f;
        }
    }
}
