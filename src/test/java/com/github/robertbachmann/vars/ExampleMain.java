package com.github.robertbachmann.vars;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ExampleMain {
    final static class PrintSubscriber<T> implements Subscriber<T> {
        private Subscription subscription;
        private final String label;

        public PrintSubscriber(String label) {
            this.label = String.format("%1$-25s", label);
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            subscription.request(1);
        }

        @Override
        public void onNext(T t) {
            System.out.printf("%s: %s%n", label, t);
            subscription.request(1);
        }

        @Override
        public void onError(Throwable t) {
            t.printStackTrace(System.err);
        }

        @Override
        public void onComplete() {
        }
    }

    public static void main(String[] args) {
        SimpleVar<Double> revenue = Var.createSimple(1000.);
        SimpleVar<Double> expenses = Var.createSimple(800.);
        SimpleVar<Double> taxRate = Var.createSimple(0.25);

        Var<Double> earningsBeforeTaxes = Var.create(revenue, expenses, (e, a) -> e - a);
        Var<Double> taxes = Var.create(earningsBeforeTaxes, taxRate, (g,s) -> g * s);
        Var<Double> earningsAfterTaxes = Var.create(earningsBeforeTaxes, taxes, (a,b) -> a - b);

        revenue.subscribe(new PrintSubscriber<>("Revenue"));
        expenses.subscribe(new PrintSubscriber<>("Expenses"));
        taxes.subscribe(new PrintSubscriber<>("Taxes"));
        taxRate.subscribe(new PrintSubscriber<>("Tax rate"));
        earningsBeforeTaxes.subscribe(new PrintSubscriber<>("Earnings before taxes"));
        earningsAfterTaxes.subscribe(new PrintSubscriber<>("Earnings after taxes"));

        System.out.println("-> Update Revenue to 1100.0");
        revenue.setValue(1100.);
    }
}
