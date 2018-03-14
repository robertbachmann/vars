package com.github.robertbachmann.vars.fugue.publicapi;

import com.github.robertbachmann.vars.SimpleVar;
import com.github.robertbachmann.vars.Var;
import com.github.robertbachmann.vars.fugue.RecoveryStrategy;
import com.github.robertbachmann.vars.fugue.TryVar;
import io.atlassian.fugue.Try;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

@Test
public class RecoveryStrategyTest {
    private final Var<Try<Integer>> C = Var.valueOf(Try.successful(3));
    private final Var<Try<Integer>> D = Var.valueOf(Try.successful(0));

    @Test
    public void div() {
        Var<Try<Integer>> result = TryVar.flatCombine(C, D, (x, y) -> x / y);
        assertTrue(result.getLastValue().isFailure());
    }

    @Test
    public void recoverDivideByZero() {
        RecoveryStrategy<Integer> recoveryStrategy = new RecoveryStrategy<>();
        recoveryStrategy.recover(e -> -1);
        Var<Try<Integer>> result = TryVar.flatCombine(C, D, (x, y) -> x / y, recoveryStrategy);

        assertTrue(result.getLastValue().isSuccess());
    }

    @Test
    public void recoverDivideByZeroInVar() {
        RecoveryStrategy<Integer> recoveryStrategy = new RecoveryStrategy<>();
        recoveryStrategy.recover(e -> -1);

        Var<Integer> c = Var.valueOf(100);
        SimpleVar<Integer> d = Var.valueOf(0);
        Var<Integer> e = Var.combine(c, d, (x, y) -> x / y);
        Var<Try<Integer>> result = TryVar.fromVar(e, recoveryStrategy);

        assertTrue(result.getLastValue().isSuccess());
        assertEquals(Try.successful(-1), result.getLastValue());

        d.setValue(2);

        assertEquals(Try.successful(50), result.getLastValue());
    }
}
