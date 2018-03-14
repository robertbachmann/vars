package com.github.robertbachmann.vars.fugue;

import com.github.robertbachmann.vars.Var;
import com.github.robertbachmann.vars.support.SplitProcessor;
import io.atlassian.fugue.Either;
import org.reactivestreams.Publisher;

import java.util.function.Supplier;

public final class EitherVar {

    public static <L extends Throwable, R> Var<Either<L, R>> lift(Var<R> var) {
        Adapter<L, R> adapter = new Adapter<>(var);
        return new EVar<>(adapter, adapter);
    }

    private static final class Adapter<L extends Throwable, R>
            extends SplitProcessor<R, Either<L, R>>
            implements Supplier<Either<L, R>> {
        private volatile Either<L, R> lastValue = null;

        Adapter(Publisher<R> var) {
            super(var);
        }

        @Override
        protected Either<L, R> right(R value) {
            Either<L, R> result = Either.right(value);
            lastValue = result;
            return result;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected Either<L, R> left(Throwable throwable) {
            Either<L, R> result = Either.left((L) throwable);
            lastValue = result;
            return result;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Either<L, R> get() {
            Either<L, R> result = lastValue;
            if (result == null) {
                return Either.left((L) new IllegalStateException("Not initialised"));
            }
            return result;
        }
    }

    private static final class EVar<L, R> extends Var<Either<L, R>> {
        private EVar(Supplier<Either<L, R>> supplier, Publisher<?>... vars) {
            super(false, supplier, vars);
        }
    }
}
