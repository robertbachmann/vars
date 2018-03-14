package com.github.robertbachmann.vars.fugue;

import com.github.robertbachmann.vars.SimpleVar;
import com.github.robertbachmann.vars.Var;
import com.github.robertbachmann.vars.support.SplitProcessor;
import io.atlassian.fugue.Try;
import org.reactivestreams.Publisher;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public final class TryVar {

    static final class Adapter<IN> extends SplitProcessor<IN, Try<IN>> implements Supplier<Try<IN>> {
        private final InternalExceptionHandler<IN> exceptionHandler;
        private volatile Try<IN> lastValue = null;

        Adapter(Publisher<IN> var, InternalExceptionHandler<IN> exceptionHandler) {
            super(var);
            this.exceptionHandler = exceptionHandler;
        }

        @Override
        protected Try<IN> right(IN value) {
            Try<IN> result = Try.successful(value);
            this.lastValue = result;
            return result;
        }

        @Override
        protected Try<IN> left(Throwable throwable) {
            Try<IN> result;
            if (throwable instanceof Exception) {
                result = exceptionHandler.handle((Exception) throwable);
            } else {
                return null;
            }

            if (result != null) {
                this.lastValue = result;
            }
            return result;
        }

        @Override
        public Try<IN> get() {
            Try<IN> result = this.lastValue;
            if (result == null) {
                return Try.failure(new IllegalStateException("Not initialised"));
            }
            return result;
        }
    }

    public static <T> SimpleVar<Try<T>> valueOf(T v) {
        return Var.valueOf(Try.successful(v));
    }

    public static <T> Var<Try<T>> fromVar(Var<T> var) {
        return fromVar(var, noRecovery());
    }

    public static <T> Var<Try<T>> fromVar(Var<T> var, RecoveryStrategy<T> recoveryStrategy) {
        // adapter performs the exception recovery
        Adapter<T> adapter = new Adapter<>(var, InternalExceptionHandler.create(recoveryStrategy));
        Supplier<Try<T>> supplier = adapter;
        return create(supplier, noRecovery(), adapter);
    }

    public static <A, T> Var<Try<T>> map(Var<Try<A>> var1, Function<Try<A>, Try<T>> function, RecoveryStrategy<T> recoveryStrategy) {
        Supplier<Try<T>> supplier = () -> function.apply(var1.getLastValue());
        return create(supplier, recoveryStrategy, var1);
    }

    public static <A, T> Var<Try<T>> flatMap(Var<Try<A>> var1, Function<A, T> function) {
        return flatMap(var1, function, noRecovery());
    }

    public static <A, T> Var<Try<T>> flatMap(Var<Try<A>> var1, Function<A, T> function, RecoveryStrategy<T> recoveryStrategy) {
        Supplier<Try<T>> supplier = () -> {
            Try<A> a = var1.getLastValue();

            if (a.isFailure()) {
                return Try.failure(a.toEither().left().get());
            }

            A theA = a.getOrElse(null);

            try {
                T t = function.apply(theA);
                return Try.successful(t);
            } catch (Exception e) {
                return Try.failure(e);
            }
        };
        return create(supplier, recoveryStrategy, var1);
    }

    public static <A, B, T> Var<Try<T>> combine(Var<Try<A>> var1, Var<Try<B>> var2, BiFunction<Try<A>, Try<B>, Try<T>> function, RecoveryStrategy<T> recoveryStrategy) {
        return create(
                () -> function.apply(var1.getLastValue(), var2.getLastValue()), recoveryStrategy,
                var1, var2
        );
    }

    public static <A, B, T> Var<Try<T>> flatCombine(Var<Try<A>> var1, Var<Try<B>> var2, BiFunction<A, B, T> function) {
        return flatCombine(var1, var2, function, noRecovery());
    }

    public static <A, B, T> Var<Try<T>> flatCombine(Var<Try<A>> var1, Var<Try<B>> var2, BiFunction<A, B, T> function, RecoveryStrategy<T> recoveryStrategy) {
        Supplier<Try<T>> supplier = () -> {
            Try<A> a = var1.getLastValue();
            Try<B> b = var2.getLastValue();

            if (a.isFailure()) {
                return Try.failure(a.toEither().left().get());
            }
            if (b.isFailure()) {
                return Try.failure(b.toEither().left().get());
            }

            A theA = a.toOption().get();
            B theB = b.toOption().get();

            try {
                T t = function.apply(theA, theB);
                return Try.successful(t);
            } catch (Exception e) {
                return Try.failure(e);
            }
        };
        return create(supplier, recoveryStrategy, var1, var2);
    }

    @SuppressWarnings("unchecked")
    private static <T> RecoveryStrategy<T> noRecovery() {
        return (RecoveryStrategy<T>) RecoveryStrategy.NONE;
    }

    private static <T> Var<Try<T>> create(Supplier<Try<T>> supplier,
                                          RecoveryStrategy<T> recoveryStrategy,
                                          Publisher<?>... vars) {
        if (recoveryStrategy == RecoveryStrategy.NONE) {
            return new TryVarImpl<>(supplier, vars);
        }

        InternalExceptionHandler exceptionHandler = InternalExceptionHandler.create(recoveryStrategy);
        Supplier<Try<T>> recoveringSupplier =
                () -> {
                    Try<T> result;
                    try {
                        result = supplier.get();
                    } catch (Exception e) {
                        result = Try.failure(e);
                    }
                    if (!result.isSuccess()) {
                        return result.recoverWith(exceptionHandler::handle);
                    }
                    return result;
                };

        return new TryVarImpl<>(recoveringSupplier, vars);
    }

    private TryVar() {
    }

    private static class TryVarImpl<T> extends Var<Try<T>> {
        TryVarImpl(Supplier<Try<T>> supplier, Publisher<?>[] vars) {
            super(false, supplier, vars);
        }
    }
}


