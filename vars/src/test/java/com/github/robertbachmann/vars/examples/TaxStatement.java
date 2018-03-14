package com.github.robertbachmann.vars.examples;

import com.github.robertbachmann.vars.SimpleVar;
import com.github.robertbachmann.vars.Var;

import java.util.Objects;

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
