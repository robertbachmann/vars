package com.github.robertbachmann.vars.examples;

import com.github.robertbachmann.vars.SimpleVar;
import com.github.robertbachmann.vars.Var;

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
