package com.github.robertbachmann.vars.fugue.publicapi;

import com.github.robertbachmann.vars.SimpleVar;
import com.github.robertbachmann.vars.Var;
import com.github.robertbachmann.vars.fugue.TryVar;
import io.atlassian.fugue.Try;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Test formula
 * <pre>
 *  e = a + b; b = (c/d)
 * </pre>
 * with a=40, c=6 and d=3 or 0
 */
@Test
public class TryTest {
    private static final int A = 40;
    private static final int C = 6;
    private static final Try<Integer> EXPECTED = Try.successful(42);

    private static Try<Integer> tryDivide(Try<Integer> c, Try<Integer> d) {
        Try<Try<Integer>> result = c.map(x -> d.map(y -> x / y));
        return Try.flatten(result);
    }

    private static Try<Integer> tryAdd(Try<Integer> a, Try<Integer> b) {
        Try<Try<Integer>> result = a.map(x -> b.map(y -> x + y));
        return Try.flatten(result);
    }

    public Try<Integer> f(Try<Integer> a, Try<Integer> b) {
        Try<Try<Integer>> result = a.map(x -> b.map(y -> x + y));
        return Try.flatten(result);
    }

    public Try<Integer> g(Try<Integer> fResult) {
        return fResult.map(x -> x + 1);
    }

    @Test
    public void tryExamplesPlainTry() {
        Try<Integer> a = Try.successful(82);
        Try<Integer> b = Try.successful(2);

        Try<Integer> result1 = g(f(a, b));
        assertEquals(Try.successful(42), result1);

        Try<Integer> b0 = Try.successful(0);

        Try<Integer> result2 = g(f(a, b0));
        assertTrue(result2.isFailure());
    }

    @Test
    public void tryExamplesTryVar() {
        Var<Try<Integer>> a = TryVar.valueOf(82);
        SimpleVar<Try<Integer>> b = TryVar.valueOf(2);
        final int c = 1;

        Var<Try<Integer>> f = TryVar.flatCombine(a, b, (x, y) -> x / y);
        Var<Try<Integer>> g = TryVar.flatMap(f, (x) -> x + c);
        assertEquals(Try.successful(42), g.get());

        b.setValue(Try.successful(0));

        assertTrue(g.get().isFailure());
    }

    @Test
    public void tryExamplesTryVar2() {
        Var<Integer> a = Var.valueOf(82);
        SimpleVar<Integer> b = Var.valueOf(2);
        final int c = 1;

        Var<Integer> f = Var.combine(a,b, (x, y) -> x / y);
        Var<Try<Integer>> tryF = TryVar.fromVar(f);

        Var<Try<Integer>> g = TryVar.flatMap(tryF, (x) -> x + c);
        assertEquals(Try.successful(42), g.get());

        b.setValue(0);
        assertTrue(g.get().isFailure());

        b.setValue(2);
        // TODO assertEquals(Try.successful(42), g.get());
    }

    @Test
    public void tryNonZeroD() {
        Try<Integer> a = Try.successful(A);
        Try<Integer> c = Try.successful(C);
        Try<Integer> d = Try.successful(3);

        Try<Integer> b = tryDivide(c, d);
        Try<Integer> e = tryAdd(a, b);

        assertEquals(EXPECTED, e);
    }

    @Test
    public void tryZeroD() {
        Try<Integer> a = Try.successful(A);
        Try<Integer> c = Try.successful(C);
        Try<Integer> d = Try.successful(0);

        Try<Integer> b = tryDivide(c, d);
        Try<Integer> e = tryAdd(a, b);

        assertTrue(e.isFailure());
    }

    @Test
    public void tryVarFlatCombineNonZeroD() {
        Var<Try<Integer>> a = Var.valueOf(Try.successful(A));
        Var<Try<Integer>> c = Var.valueOf(Try.successful(C));
        Var<Try<Integer>> d = TryVar.fromVar(Var.valueOf(3));

        Var<Try<Integer>> b = TryVar.flatCombine(c, d, (x, y) -> x / y);
        Var<Try<Integer>> e = TryVar.flatCombine(a, b, (x, y) -> x + y);

        assertEquals(EXPECTED, e.getLastValue());
    }

    @Test
    public void tryVarFlatCombineNonZeroD2() {
        Var<Integer> a = Var.valueOf(A);
        Var<Integer> c = Var.valueOf(C);
        Var<Integer> d = Var.valueOf(3);

        Var<Integer> b = Var.combine(c, d, (x, y) -> x / y);
        Var<Try<Integer>> e = TryVar.flatCombine(TryVar.fromVar(a), TryVar.fromVar(b), (x, y) -> x + y);

        assertEquals(EXPECTED, e.getLastValue());
    }

    @Test
    public void tryVarFlatCombineNonZeroD3() {
        Var<Integer> a = Var.valueOf(A);
        Var<Integer> c = Var.valueOf(C);
        Var<Integer> d = Var.valueOf(0);

        Var<Integer> b = Var.combine(c, d, (x, y) -> x / y);
        Var<Try<Integer>> e = TryVar.flatCombine(TryVar.fromVar(a), TryVar.fromVar(b), (x, y) -> x + y);

        assertTrue(e.getLastValue().isFailure());
    }

    @Test
    public void tryVarFromDivZeroVar() {
        SimpleVar<Integer> a = Var.valueOf(0);
        Var<Integer> b = Var.map(a, integer -> {
            if (integer == 0)
                System.out.println("Will divide by zero");
            return 100 / integer;
        });

        Var<Try<Integer>> tryB = TryVar.fromVar(b);

        assertTrue(tryB.get().isFailure());
        a.setValue(1);

        assertEquals(Try.successful(100), tryB.get());
    }

    @Test
    public void tryVarCombineNonZeroD() {
        Var<Try<Integer>> a = Var.valueOf(Try.successful(A));
        Var<Try<Integer>> c = Var.valueOf(Try.successful(C));
        Var<Try<Integer>> d = Var.valueOf(Try.successful(3));

        Var<Try<Integer>> b = Var.combine(c, d, TryTest::tryDivide);
        Var<Try<Integer>> e = Var.combine(a, b, TryTest::tryAdd);

        assertEquals(EXPECTED, e.getLastValue());
    }


    @Test
    public void tryVarFlatCombineZeroD() {
        Var<Try<Integer>> a = Var.valueOf(Try.successful(A));
        Var<Try<Integer>> c = Var.valueOf(Try.successful(C));
        Var<Try<Integer>> d = TryVar.fromVar(Var.valueOf(0));

        Var<Try<Integer>> b = TryVar.flatCombine(c, d, (x, y) -> x / y);
        Var<Try<Integer>> e = TryVar.flatCombine(a, b, (x, y) -> x + y);

        assertTrue(e.getLastValue().isFailure());
    }
}
