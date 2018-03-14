package com.github.robertbachmann.vars.publicapi;

import com.github.robertbachmann.vars.SimpleVar;
import com.github.robertbachmann.vars.Var;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.subscribers.TestSubscriber;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

@Test
public class DerivedVarTest {
    @Test
    public void testSingleVar() {
        SimpleVar<Integer> var = Var.valueOf(2);
        Var<Integer> varSquare = Var.map(var, a -> a * a);

        TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        varSquare.subscribe(subscriber);

        var.setValue(3);
        var.setValue(4);

        subscriber.assertSubscribed();
        subscriber.assertValuesOnly(4, 9, 16);
        subscriber.assertValueCount(3);
    }

    @Test
    public void testCancel() {
        SimpleVar<Integer> var = Var.valueOf(2);

        TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        var.subscribe(subscriber);
        subscriber.assertSubscribed();

        subscriber.cancel();
    }


    @Test
    public void testCancel2() {
        BehaviorProcessor<Integer> test = BehaviorProcessor.createDefault(1);
        TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        test.subscribe(subscriber);
        subscriber.cancel();
    }

    @Test
    public void testError() {
        SimpleVar<Integer> var = Var.valueOf(1);
        Var<Integer> varTimesTwo = Var.map(var, a -> {
            if (a < 0) {
                throw new IllegalArgumentException("value < 0");
            }
            return a * 2;
        });
        Var<Integer> varTimes4 = Var.map(varTimesTwo, a -> a * 2);

        TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        varTimes4.subscribe(subscriber);
        subscriber.assertSubscribed();

        var.setValue(2);
        subscriber.assertValuesOnly(4, 8);

        var.setValue(-1);
        subscriber.assertErrorMessage("value < 0");

        var.setValue(10);
        subscriber.assertErrorMessage("value < 0");

        assertEquals(Integer.valueOf(40), varTimes4.getLastValue());
    }

    @Test
    public void testErrorX() {
        SimpleVar<Integer> var = Var.valueOf(1);
        Var<Integer> varTimesTwo = Var.map(var, a -> {
            if (a < 0) {
                throw new IllegalArgumentException("value < 0");
            }
            return a * 2;
        });
        Var<Integer> varTimes4 = Var.map(varTimesTwo, a -> a * 2);

        TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        varTimes4.subscribe(subscriber);
        subscriber.assertSubscribed();

        var.setValue(2);
        subscriber.assertValuesOnly(4, 8);

        var.setValue(-1);
        subscriber.assertErrorMessage("value < 0");

        TestSubscriber<Integer> subscriber2 = new TestSubscriber<>();
        varTimes4.subscribe(subscriber2);
        subscriber.assertErrorMessage("value < 0");

        var.setValue(10);
        TestSubscriber<Integer> subscriber3 = new TestSubscriber<>();
        varTimes4.subscribe(subscriber3);
        subscriber3.assertValuesOnly(40);
        subscriber3.assertNotComplete();
    }

    @Test
    public void testMultipleVar() {
        SimpleVar<Double> revenue = Var.valueOf(1000.);
        SimpleVar<Double> expenses = Var.valueOf(800.);
        SimpleVar<Double> taxRate = Var.valueOf(0.25);

        Var<Double> earningsBeforeTaxes = Var.combine(revenue, expenses, (e, a) -> e - a);
        Var<Double> taxes = Var.combine(earningsBeforeTaxes, taxRate, (g, s) -> g * s);
        Var<Double> earningsAfterTaxes = Var.combine(earningsBeforeTaxes, taxes, (a, b) -> a - b);

        TestSubscriber<Double> revenueSubscriber = new TestSubscriber<>();
        TestSubscriber<Double> expensesSubscriber = new TestSubscriber<>();
        TestSubscriber<Double> taxesSubscriber = new TestSubscriber<>();
        TestSubscriber<Double> taxRateSubscriber = new TestSubscriber<>();
        TestSubscriber<Double> earningsBeforeTaxesSubscriber = new TestSubscriber<>();
        TestSubscriber<Double> earningsAfterTaxesSubscriber = new TestSubscriber<>();


        revenue.subscribe(revenueSubscriber);
        expenses.subscribe(expensesSubscriber);
        taxes.subscribe(taxesSubscriber);
        taxRate.subscribe(taxRateSubscriber);
        earningsBeforeTaxes.subscribe(earningsBeforeTaxesSubscriber);
        earningsAfterTaxes.subscribe(earningsAfterTaxesSubscriber);

        taxRate.setValue(0.2);

        revenueSubscriber.assertValuesOnly(1000.).assertSubscribed();
        expensesSubscriber.assertValuesOnly(800.).assertSubscribed();
        earningsBeforeTaxesSubscriber.assertValuesOnly(200.).assertSubscribed();
        taxRateSubscriber.assertValuesOnly(0.25, 0.20).assertSubscribed();
        taxesSubscriber.assertValuesOnly(50., 40.).assertSubscribed();
        earningsAfterTaxesSubscriber.assertValuesOnly(150., 160.).assertSubscribed();
    }
}
