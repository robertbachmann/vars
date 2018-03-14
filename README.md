# Vars

An experimental library for "functional reactive variables". For example 
given `z = f(x,y)` any change to `x` or `y` will cause `z` to be re-calculated. 

## Implementation notes

* The library is compatible with Java 8+ (`module-info` for Java 9 is included)
* The library implements the [org.reactivestreams.Publisher](http://www.reactive-streams.org/reactive-streams-1.0.2-javadoc/org/reactivestreams/Publisher.html) API.  
  (1:1 sceptically equivalent to Java 9's `Flow.Publisher`)


## Examples

### Simple example with three variables

~~~java
SimpleVar<Double> revenue = Var.valueOf(1000.0);
SimpleVar<Double> expenses = Var.valueOf(800.0);
Var<Double> earnings = Var.combine(revenue, expenses, (x, y) -> x - y);

earnings.subscribe(x -> System.out.println("Earnings: " + x));

System.out.println("-> Update Revenue to 1100.0");
revenue.setValue(1100.0);

// Output:
// Earnings: 200.0
// -> Update Revenue to 1100.0
// Earnings: 300.0
~~~ 

* `Var<T>` provides the following instance methods:
    * `T get()` to get current value (from `Supplier<T>`)
    * `void subscribe​(Subscriber<? super T> s)` to allow subscription (from `Publisher<T>`)
    * Convenience overloads of `subscribe`
* `SimpleVar<T>` extends `Var<T>` and provides a `setValue(T)` method to set the current value  

### Example with six variables

~~~java
SimpleVar<Double> revenue = Var.valueOf(1000.);
SimpleVar<Double> expenses = Var.valueOf(800.);
SimpleVar<Double> taxRate = Var.valueOf(0.25);

Var<Double> earnings = Var.combine(revenue, expenses, (e, a) -> e - a);
Var<Double> taxes = Var.combine(earnings, taxRate, (g, s) -> g * s);
Var<Double> earningsAfterTaxes = Var.combine(earnings, taxes, (a, b) -> a - b);

revenue.subscribe(d -> System.out.println("Revenue: " + d));
expenses.subscribe(d -> System.out.println("Expenses: " + d));
taxes.subscribe(d -> System.out.println("Taxes: " + d));
taxRate.subscribe(d -> System.out.println("Tax rate: " + d));
earnings.subscribe(d -> System.out.println("Earnings: " + d));
earningsAfterTaxes.subscribe(d -> System.out.println("Earnings after taxes: " + d));

System.out.println("-> Update Revenue to 1100.0");
revenue.setValue(1100.);
// Output:
// Revenue: 1000.0
// Expenses: 800.0
// Taxes: 50.0
// Tax rate: 0.25
// Earnings: 200.0
// Earnings after taxes: 150.0
// -> Update Revenue to 1100.0
// Earnings after taxes: 225.0
// Taxes: 75.0
// Earnings: 300.0
// Revenue: 1100.0
~~~

### Comparision with the RxJava API 

[RxJava](https://github.com/ReactiveX/RxJava) provides a `BehaviorProcessor<T>` that is similar to
`SimpleVar<T>`. The following code re-implements the "Simple example with three variables" example:

~~~java
BehaviorProcessor<Double> revenue = BehaviorProcessor.createDefault(1000.);
Flowable<Double> revenueDistinct = revenue.distinct();
Flowable<Double> expenses = BehaviorProcessor.createDefault(800.).distinct();

Flowable<Double> earnings = Flowable.combineLatest(
        revenueDistinct, expenses, (x, y) -> x - y).distinct();

earnings.subscribe(d -> System.out.println("Earnings: " + d));

System.out.println("-> Update Revenue to 1100.0");
revenue.onNext(1100.);

// Output:
// Earnings: 200.0
// -> Update Revenue to 1100.0
// Earnings: 300.0
~~~

Note that in the provided example we use the RxJava's `distinct()` operator, in order to avoid
unnecessary re-calculations of `earnings`. (This behaviour is already built into `Var<T>`). 

### Interoperability

Since each _Var_ implements the _Publisher_ interface they can also be used with other reactive libraries
that support the [_reactive-streams.org_](http://www.reactive-streams.org/) API.

Bellow is an example using [RxJava](https://github.com/ReactiveX/RxJava)'s [Flowable](http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Flowable.html):

~~~java
Var<Double> earnings = Var.combine(revenue, expenses, (e, a) -> e - a);

io.reactivex.Flowable
        .fromPublisher(earnings)
        .subscribe(v -> System.out.println("Earnings: " + v));
~~~ 

Bellow is an example using [Reactor](https://projectreactor.io/)'s [Flux](http://projectreactor.io/docs/core/release/reference/#flux):

~~~java
Var<Double> earnings = Var.combine(revenue, expenses, (e, a) -> e - a);

reactor.core.publisher.Flux
        .fromPublisher(earnings)
        .subscribe(v -> System.out.println("Earnings: " + v));
~~~ 

### Examples with generated classes 

Given the following class diagram, we could generate the classes below: 

![Class diagram](https://robertbachmann.github.io/vars/images/class-diagram1.png)

~~~java
public class CompanyStatement { //Generated
    private final SimpleVar<Double> revenue;
    private final SimpleVar<Double> expenses;
    private final Var<Double> earnings;

    public CompanyStatement(Double revenue, Double expenses) {
        this.revenue = Var.valueOf(revenue);
        this.expenses = Var.valueOf(expenses);
        this.earnings = Var.combine(this.revenue, this.expenses, (x, y) -> x - y);
    }

    public CompanyStatement() {
        this(0.0, 0.0);
    }

    public Double getRevenue() {
        return revenue.get();
    }

    public void setRevenue(Double revenue) {
        this.revenue.setValue(revenue);
    }

    public Var<Double> getRevenuePublisher() {
        return revenue;
    }

    public double getExpenses() {
        return expenses.get();
    }

    public void setExpenses(Double expenses) {
        this.expenses.setValue(expenses);
    }

    public Var<Double> getExpensesPublisher() {
        return expenses;
    }

    public double getEarnings() {
        return earnings.get();
    }

    public Var<Double> getEarningsPublisher() {
        return earnings;
    }
}

public class TaxStatement { //Generated
    private final CompanyStatement companyStatement;
    private final SimpleVar<Double> taxRate;
    private final Var<Double> taxes;
    private final Var<Double> earningAfterTaxes;

    public TaxStatement(CompanyStatement companyStatement, Double taxRate) {
        this.companyStatement = Objects.requireNonNull(companyStatement, "companyStatement");
        this.taxRate = Var.valueOf(taxRate);
        this.taxes = Var.combine(
                companyStatement.getEarningsPublisher(), this.taxRate, (x, y) -> x * y);
        this.earningAfterTaxes = Var.combine(
                this.companyStatement.getEarningsPublisher(), this.taxes, (x, y) -> x - y);
    }

    public TaxStatement(CompanyStatement companyStatement) {
        this(companyStatement, 0.0);
    }

    public CompanyStatement getCompanyStatement() {
        return companyStatement;
    }

    public double getTaxRate() {
        return taxRate.get();
    }

    public void setTaxRate(double taxRate) {
        this.taxRate.setValue(taxRate);
    }

    public double getTaxes() {
        return taxes.get();
    }

    public Var<Double> getTaxesPublisher() {
        return taxes;
    }

    public double getEarningAfterTaxes() {
        return earningAfterTaxes.get();
    }

    public Var<Double> getEarningAfterTaxesPublisher() {
        return earningAfterTaxes;
    }
}
~~~~

**Usage example (1):**

~~~~java
CompanyStatement companyStatement = new CompanyStatement();
companyStatement.setRevenue(300);
companyStatement.setExpenses(200);

assertEquals(100.0, companyStatement.getEarnings());
~~~~ 

**Usage example (2):** 

~~~~java
CompanyStatement companyStatement = new CompanyStatement();
companyStatement.setRevenue(300);
companyStatement.setExpenses(200);

TaxStatement taxStatement = new TaxStatement(companyStatement);
taxStatement.setTaxRate(25 / 100.0);

taxStatement
        .getEarningAfterTaxesPublisher()
        .subscribe((x -> System.out.println("Earnings after taxes: " + x)));

companyStatement.setRevenue(400);

// Output:
// Earnings after taxes: 75.0
// Earnings after taxes: 150.0
~~~~


### Examples for functional exception handling

When dealing with function chains such as `g(f(...))` care must be taken if `f(...)` might
fail. 

One way deal with this problem is to Java's `Optional<T>` monad and change `f` and `g` to
return `Optional.empty` on error. This approach has the drawback that available exception information.
gets lost. To keep the exception information the pattern from Scala's [Try](http://www.scala-lang.org/api/current/scala/util/Try.html)  
type can be used. 

Atlassian's Fugue library provides a Java implementation of the `Try` type. The
module `vars-fugue` provides integration with Fugue.     

Bellow is an example for Fugue's API.

~~~
// calculate g(f(a,b)) with f(a,b)=x/y and g(v)=v+1
// (b might be zero)
@Test
public void tryExamplesPlainTry() {
    Try<Integer> a = Try.successful(82);
    Try<Integer> b = Try.successful(2);

    Try<Integer> result1 = g(f(a, b));
    assertEquals(Try.successful(42), result1);

    Try<Integer> b0 = Try.successful(0);

    Try<Integer> result2 = g(f(a, b0));
    assertTrue(result2.isFailure()); // div-by-zero
}

public Try<Integer> f(Try<Integer> a, Try<Integer> b) {
    Try<Try<Integer>> result = a.map(x -> b.map(y -> x + y));
    return Try.flatten(result);
}

public Try<Integer> g(Try<Integer> fResult) {
    return fResult.map(x -> x + 1);
}
~~~

Bellow is an example for the `TryVar` helper class provided by `var-fugue`
~~~java
Var<Try<Integer>> a = TryVar.valueOf(82);
SimpleVar<Try<Integer>> b = TryVar.valueOf(2);

Var<Try<Integer>> f = TryVar.flatCombine(a, b, (x, y) -> x / y);
Var<Try<Integer>> g = TryVar.flatMap(f, (x) -> x + 1);
assertEquals(Try.successful(42), g.get());

b.setValue(Try.successful(0));

assertTrue(g.get().isFailure()); // div-by-zero
~~~


## Open issues

* Add N-ary (n>2) functions to `Var.combine` `TryVar.flatCombine` 
* Add Kotlin extension functions so that `Var.valueOf(x)` can be written as
  `x.toVar()` (similar to Reactor's extensions)
* Add support for Kotlin [`Try`](https://www.javacodegeeks.com/2017/12/kotlin-try-type-functional-exception-handling.html) implementation.
* Find a (user-friendly) way to re-use Thread Scheduler implementations from RxJava or Reactor.     
* **Unit Tests**
    * Raise test code coverage about (currently 70-80%)
    * org.reactivestreams TCK test adapter currently has a race condition that causes flip-flop tests.
     
     