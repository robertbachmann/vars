package com.github.robertbachmann.vars.publicapi;

import com.github.robertbachmann.vars.SimpleVar;
import com.github.robertbachmann.vars.Var;
import io.reactivex.subscribers.TestSubscriber;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

@SuppressWarnings("Duplicates")
@Test
public class LazyVarTest {
    static void assertLazy(Var<?> v) {
        assertTrue("Var not lazy", v.isLazy());
    }

    static void assertEager(Var<?> v) {
        assertTrue("Var not eager", !v.isLazy());
    }

    @Test
    public void testSingleVar() {
        SimpleVar<Integer> var = Var.valueOf(2);
        assertEager(var);
    }

    @Test
    public void testDerivedVarNotLazy() {
        SimpleVar<Integer> var = Var.valueOf(2);
        Var<Integer> var2 = Var.map(var, (x) -> x * 2);
        assertEager(var);
        assertEager(var2);
    }

    @Test
    public void test2Var() {
        SimpleVar<Integer> var = Var.valueOf(2);
        Var<Integer> var2 = Var.lazyMap(var, (x) -> x * 2);

        assertLazy(var2);
        assertNull(var2.getLastValue());
        assertNotNull(var2.get());

        TestSubscriber<Integer> s = TestSubscriber.create(1);
        var2.subscribe(s);

        s.assertSubscribed();
        s.assertValuesOnly(4);

        var.setValue(3);
        s.assertValuesOnly(4);

        s.request(1);
        s.assertValuesOnly(4, 6);
    }

    @Test
    public void test2Input() {
        SimpleVar<Integer> var1 = Var.valueOf(2);
        SimpleVar<Integer> var2 = Var.valueOf(3);
        Var<Integer> var3 = Var.lazyCombine(var1, var2, (x, y) -> (x + y));

        TestSubscriber<Integer> s = TestSubscriber.create(1);
        var3.subscribe(s);

        s.assertSubscribed();
        s.assertValuesOnly(5);

        var1.setValue(20);
        var2.setValue(30);
        s.assertValuesOnly(5);

        s.request(1);
        s.assertValuesOnly(5, 50);
    }

    @Test
    public void test3Var() {
        SimpleVar<Integer> var = Var.valueOf(1);
        Var<Integer> var2 = Var.map(var, (x) -> {
            System.out.println(x + " * 2" + " = " + (x * 2));
            return x * 2;
        });
        Var<Integer> var3 = Var.lazyCombine(var, var2, (x, y) -> {
            System.out.println(x + " + " + y + " = " + (x + y));
            return x + y;
        });

        assertLazy(var3);

        TestSubscriber<Integer> s = TestSubscriber.create(1);
        System.out.println("Request 1");
        var3.subscribe(s);

        s.assertSubscribed();
        s.assertValuesOnly(3);

        var.setValue(2);
        s.assertValuesOnly(3);

        System.out.println("Request 1");
        s.request(1);
        s.assertValuesOnly(3, 6);
    }

    @Test
    public void test3VarAllLazy() {
        SimpleVar<Integer> var = Var.valueOf(50);
        Var<Integer> var2 = Var.lazyMap(var, (x) -> {
            System.out.println(x + " * 2" + " = " + (x * 2));
            return x * 2;
        });
        Var<Integer> var3 = Var.lazyCombine(var, var2, (x, y) -> {
            System.out.println(x + " + " + y + " = " + (x + y));
            return x + y;
        });

        assertLazy(var2);
        assertLazy(var3);

        TestSubscriber<Integer> s = TestSubscriber.create(1);
        System.out.println("Request 1");
        var3.subscribe(s);

        s.assertSubscribed();
        s.assertValuesOnly(150);

        var.setValue(3);
        s.assertValuesOnly(150);

        System.out.println("Request 1");
        s.request(1);
        s.assertValuesOnly(150, 9);
    }

    @Test
    public void test4VarAllLazy() {
        SimpleVar<Integer> var1 = Var.valueOf(1);
        Var<Integer> var2 = Var.lazyMap(var1, (x) -> x * 2);
        SimpleVar<Integer> var3 = Var.valueOf(1000);
        Var<Integer> var4 = Var.lazyCombine(var2, var3, (x, y) -> x + y);

        assertEager(var1);
        assertLazy(var2);
        assertEager(var3);
        assertLazy(var4);

        TestSubscriber<Integer> s = TestSubscriber.create(1);
        var4.subscribe(s);

        s.assertSubscribed();
        s.assertValuesOnly(1002);

        var1.setValue(3);
        s.assertValuesOnly(1002);

        s.request(1);
        s.assertValuesOnly(1002, 1006);
    }
}
