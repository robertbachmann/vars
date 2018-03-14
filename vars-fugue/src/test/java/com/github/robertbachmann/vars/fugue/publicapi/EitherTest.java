package com.github.robertbachmann.vars.fugue.publicapi;

import com.github.robertbachmann.vars.Var;
import com.github.robertbachmann.vars.fugue.EitherVar;
import io.atlassian.fugue.Either;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

@Test
public class EitherTest {
    @Test
    public void dummy() {
        Var<Integer> var = Var.valueOf(1);
        Var<Either<Throwable, Integer>> lifted = EitherVar.lift(var);

        Either<Throwable, Integer> either = lifted.getLastValue();
        assertEquals(Integer.valueOf(1), either.right().get());
    }

    @Test
    public void dummy2() {
        Var<Integer> var = Var.valueOf(1);
        Var<Integer> derivedVar = Var.map(var, (x) -> x / 0);

        Var<Either<Throwable, Integer>> lifted = EitherVar.lift(derivedVar);
        System.out.println(lifted);

        Either<Throwable, Integer> either = lifted.getLastValue();
        assertTrue(either.isLeft());
        assertTrue(either.left().get() instanceof ArithmeticException);
    }
}
