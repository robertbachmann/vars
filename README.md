# Vars

A proof-of-concept implementation of variables
that implement [org.reactivestreams.Publisher](http://www.reactive-streams.org/reactive-streams-1.0.2-javadoc/org/reactivestreams/Publisher.html).

Code example:

~~~java

public class ExampleMain {
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
}

~~~

Example output:

    Revenue                  : 1000.0
    Expenses                 : 800.0
    Taxes                    : 50.0
    Tax rate                 : 0.25
    Earnings before taxes    : 200.0
    Earnings after taxes     : 150.0
    -> Update Revenue to 1100.0
    Earnings after taxes     : 225.0
    Taxes                    : 75.0
    Earnings before taxes    : 300.0
    Revenue                  : 1100.0
