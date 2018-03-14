package com.github.robertbachmann.vars.examples;

import com.github.robertbachmann.vars.SimpleVar;
import com.github.robertbachmann.vars.SystemOutSpyingTest;
import com.github.robertbachmann.vars.Var;
import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import org.testng.annotations.Test;
import reactor.core.publisher.Flux;

import static org.testng.AssertJUnit.*;

@Test
public class ExampleTest extends SystemOutSpyingTest {
    public ExampleTest() {
        super(false);
    }

    @Test
    public void example1Simple() {
        SimpleVar<Double> revenue = Var.valueOf(1000.0);
        SimpleVar<Double> expenses = Var.valueOf(800.0);
        Var<Double> earnings = Var.combine(revenue, expenses, (x, y) -> x - y);

        earnings.subscribe(x -> System.out.println("Earnings: " + x));

        System.out.println("-> Update Revenue to 1100.0");
        revenue.setValue(1100.0);

        assertLines(
                "Earnings: 200.0",
                "-> Update Revenue to 1100.0",
                "Earnings: 300.0");
    }

    @Test
    public void example1WithAssertions() {
        SimpleVar<Double> revenue = Var.valueOf(1000.0);
        SimpleVar<Double> expenses = Var.valueOf(800.0);
        Var<Double> earnings = Var.combine(revenue, expenses, (x, y) -> x - y);

        assertEquals(200.0, earnings.getLastValue());
        earnings.subscribe(x -> System.out.println("Earnings: " + x));

        System.out.println("-> Update Revenue to 1100.0");
        revenue.setValue(1100.0);

        assertEquals(300.0, earnings.getLastValue());

        assertLines(
                "Earnings: 200.0",
                "-> Update Revenue to 1100.0",
                "Earnings: 300.0");
    }


    @Test
    public void example1Lazy() {
        SimpleVar<Double> revenue = Var.valueOf(1000.0);
        SimpleVar<Double> expenses = Var.valueOf(800.0);
        Var<Double> earnings = Var.lazyCombine(revenue, expenses, (x, y) -> x - y);

        assertNull(earnings.getLastValue());
        earnings.subscribe(x -> System.out.println("Earnings: " + x));
        assertEquals(200.0, earnings.getLastValue());

        System.out.println("-> Update Revenue to 1100.0");
        revenue.setValue(1100.0);

        assertEquals(300.0, earnings.getLastValue());

        assertLines(
                "Earnings: 200.0",
                "-> Update Revenue to 1100.0",
                "Earnings: 300.0");
    }

    @Test
    public void example1LazyToEager() {
        SimpleVar<Double> revenue = Var.valueOf(1000.0);
        SimpleVar<Double> expenses = Var.valueOf(800.0);
        Var<Double> lazyEarnings = Var.lazyCombine(revenue, expenses, (x, y) -> x - y);
        Var<Double> earnings = Var.eager(lazyEarnings);

        assertEquals(200.0, earnings.getLastValue());
        earnings.subscribe(x -> System.out.println("Earnings: " + x));

        System.out.println("-> Update Revenue to 1100.0");
        revenue.setValue(1100.0);

        assertEquals(300.0, earnings.getLastValue());

        assertLines(
                "Earnings: 200.0",
                "-> Update Revenue to 1100.0",
                "Earnings: 300.0");
    }

    @Test
    public void example1EagerGet() {
        SimpleVar<Double> revenue = Var.valueOf(1000.0);
        SimpleVar<Double> expenses = Var.valueOf(800.0);
        Var<Double> earnings = Var.lazyCombine(revenue, expenses, (x, y) -> x - y);

        assertNull(earnings.getLastValue());
        assertEquals(200.0, earnings.get());
        assertEquals(200.0, earnings.getLastValue());

        earnings.subscribe(x -> System.out.println("Earnings: " + x));

        System.out.println("-> Update Revenue to 1100.0");
        revenue.setValue(1100.0);

        assertEquals(300.0, earnings.getLastValue());

        assertLines(
                "Earnings: 200.0",
                "-> Update Revenue to 1100.0",
                "Earnings: 300.0");
    }

    @Test
    public void example1b() {
        SimpleVar<Double> revenue = Var.valueOf(1000.);
        SimpleVar<Double> expenses = Var.valueOf(800.);
        Var<Double> earnings = Var.combine(revenue, expenses, (x, y) -> x - y);

        io.reactivex.Flowable.fromPublisher(earnings)
                .subscribe(x -> System.out.println("Earnings: " + x));


        System.out.println("-> Update Revenue to 1100.0");
        revenue.setValue(1100.0);

        assertLines(
                "Earnings: 200.0",
                "-> Update Revenue to 1100.0",
                "Earnings: 300.0");
    }

    @Test
    public void example2() {
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
        assertLines("Revenue: 1000.0",
                "Expenses: 800.0",
                "Taxes: 50.0",
                "Tax rate: 0.25",
                "Earnings: 200.0",
                "Earnings after taxes: 150.0",
                "-> Update Revenue to 1100.0",
                "Earnings after taxes: 225.0",
                "Taxes: 75.0",
                "Earnings: 300.0",
                "Revenue: 1100.0");
    }

    @Test
    public void example1RxJava() {
        BehaviorProcessor<Double> revenue = BehaviorProcessor.createDefault(1000.);
        Flowable<Double> revenueDistinct = revenue.distinct();
        Flowable<Double> expenses = BehaviorProcessor.createDefault(800.).distinct();

        Flowable<Double> earnings = Flowable.combineLatest(
                revenueDistinct, expenses, (x, y) -> x - y).distinct();

        earnings.subscribe(d -> System.out.println("Earnings: " + d));

        System.out.println("-> Update Revenue to 1100.0");
        revenue.onNext(1100.);

        assertLines(
                "Earnings: 200.0",
                "-> Update Revenue to 1100.0",
                "Earnings: 300.0");
    }

    @Test
    public void example2RxJava() {
        BehaviorProcessor<Double> revenue = BehaviorProcessor.createDefault(1000.);
        Flowable<Double> revenueDistinct = revenue.distinct();
        Flowable<Double> expenses = BehaviorProcessor.createDefault(800.).distinct();
        Flowable<Double> taxRate = BehaviorProcessor.createDefault(0.25).distinct();

        Flowable<Double> earnings = Flowable.combineLatest(
                revenueDistinct, expenses, (x, y) -> x - y).distinct();
        Flowable<Double> taxes = Flowable.combineLatest(
                earnings, taxRate, (x, y) -> x * y).distinct();
        Flowable<Double> earningsAfterTaxes = Flowable.combineLatest(
                revenueDistinct, expenses, (x, y) -> x - y).distinct();

        revenueDistinct.subscribe(d -> System.out.println("Revenue: " + d));
        expenses.subscribe(d -> System.out.println("Expenses: " + d));
        taxes.subscribe(d -> System.out.println("Taxes: " + d));
        taxRate.subscribe(d -> System.out.println("Tax rate: " + d));
        earnings.subscribe(d -> System.out.println("Earnings: " + d));
        earningsAfterTaxes.subscribe(d -> System.out.println("Earnings after taxes: " + d));

        System.out.println("-> Update Revenue to 1100.0");
        revenue.onNext(1100.);

        assertLines("Revenue: 1000.0",
                "Expenses: 800.0",
                "Taxes: 50.0",
                "Tax rate: 0.25",
                "Earnings: 200.0",
                "Earnings after taxes: 200.0",
                "-> Update Revenue to 1100.0",
                "Revenue: 1100.0",
                "Taxes: 75.0",
                "Earnings: 300.0",
                "Earnings after taxes: 300.0");
    }

    @Test
    public void companyStatementExample1() {
        CompanyStatement companyStatement = new CompanyStatement();
        companyStatement.setRevenue(300.0);
        companyStatement.setExpenses(200.0);

        assertEquals(100.0, companyStatement.getEarnings());
    }

    @Test
    public void companyStatementExample2() {
        CompanyStatement companyStatement = new CompanyStatement();
        companyStatement.setRevenue(300.0);
        companyStatement.setExpenses(200.0);

        assertEquals(100.0, companyStatement.getEarnings());

        companyStatement
                .getEarningsPublisher()
                .subscribe(x -> System.out.println("Earnings: " + x));

        companyStatement.setExpenses(275.0);
        assertEquals(25.0, companyStatement.getEarnings());


        assertLines("Earnings: 100.0",
                "Earnings: 25.0");
    }

    @Test
    public void companyStatementExample3() {
        CompanyStatement companyStatement = new CompanyStatement();
        TaxStatement taxStatement = new TaxStatement(companyStatement);
        taxStatement.setTaxRate(25 / 100.0);

        companyStatement.setRevenue(300.0);
        companyStatement.setExpenses(200.0);

        assertEquals(75.0, taxStatement.getEarningAfterTaxes());
    }

    @Test
    public void companyStatementExample4() {
        CompanyStatement companyStatement = new CompanyStatement();
        companyStatement.setRevenue(300.0);
        companyStatement.setExpenses(200.0);

        TaxStatement taxStatement = new TaxStatement(companyStatement);
        taxStatement.setTaxRate(25 / 100.0);

        taxStatement
                .getEarningAfterTaxesPublisher()
                .subscribe((x -> System.out.println("Earnings after taxes: " + x)));

        companyStatement.setRevenue(400.0);
        assertLines("Earnings after taxes: 75.0",
                "Earnings after taxes: 150.0");
    }

    @Test
    public void exampleFlowable() {
        SimpleVar<Double> revenue = Var.valueOf(1000.);
        SimpleVar<Double> expenses = Var.valueOf(800.);
        Var<Double> earnings = Var.combine(revenue, expenses, (e, a) -> e - a);

        io.reactivex.Flowable
                .fromPublisher(earnings)
                .subscribe(v -> System.out.println("Earnings: " + v));
    }

    @Test
    public void exampleFlux() {
        SimpleVar<Double> revenue = Var.valueOf(1000.);
        SimpleVar<Double> expenses = Var.valueOf(800.);
        Var<Double> earnings = Var.combine(revenue, expenses, (e, a) -> e - a);

        Flux
                .from(earnings)
                .subscribe(v -> System.out.println("Earnings: " + v));
    }


    @Test
    public void assertionsAreEnabled() {
        assertTrue(super.assertionsEnabled);
    }
}
