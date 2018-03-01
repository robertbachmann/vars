package com.github.robertbachmann.vars;

import io.reactivex.subscribers.TestSubscriber;
import org.junit.jupiter.api.Test;

public class DerivedVarTest {
    @Test
    public void testSingleVar() {
        SimpleVar<Integer> var = Var.createSimple(2);
        Var<Integer> varSquare = Var.create(var, a -> a*a);

        TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        varSquare.subscribe(subscriber);

        var.setValue(3);
        var.setValue(4);

        subscriber.assertSubscribed();
        subscriber.assertValuesOnly(4,9,16);
        subscriber.assertValueCount(3);
    }

    @Test
    public void testMultipleVar() {
        SimpleVar<Double> revenue = Var.createSimple(1000.);
        SimpleVar<Double> expenses = Var.createSimple(800.);
        SimpleVar<Double> taxRate = Var.createSimple(0.25);

        Var<Double> earningsBeforeTaxes = Var.create(revenue, expenses, (e, a) -> e - a);
        Var<Double> taxes = Var.create(earningsBeforeTaxes, taxRate, (g,s) -> g * s);
        Var<Double> earningsAfterTaxes = Var.create(earningsBeforeTaxes, taxes, (a,b) -> a - b);

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
        taxesSubscriber.assertValuesOnly(50.,40.).assertSubscribed();
        earningsAfterTaxesSubscriber.assertValuesOnly(150., 160.).assertSubscribed();
    }
}
