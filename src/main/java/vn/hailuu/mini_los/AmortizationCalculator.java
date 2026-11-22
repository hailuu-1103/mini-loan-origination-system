package vn.hailuu.mini_los;

import vn.hailuu.mini_los.shared.Money;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Equated monthly installment (EMI) schedule:
 * EMI = P * r * (1+r)^n / ((1+r)^n - 1), r = annualRate / 12.
 */
public class AmortizationCalculator {
    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * @param annualRate annual interest rate as a decimal fraction (12% = 0.12)
     */
    public List<Installment> schedule(Money principal, BigDecimal annualRate, int months, LocalDate disbursementDate) {
        Objects.requireNonNull(principal, "principal must not be null");
        Objects.requireNonNull(annualRate, "annualRate must not be null");
        Objects.requireNonNull(disbursementDate, "disbursementDate must not be null");
        if (months <= 0)
            throw new IllegalArgumentException("months must be positive: " + months);
        if (annualRate.signum() < 0)
            throw new IllegalArgumentException("annualRate must not be negative: " + annualRate);

        var monthlyRate = annualRate.divide(MONTHS_PER_YEAR, MC);
        var emi = toMoney(emiAmount(principal.amount(), monthlyRate, months), principal);

        var result = new ArrayList<Installment>(months);
        var remaining = principal;
        for (var period = 1; period <= months; period++) {
            // Always offset from disbursementDate: chaining plusMonths(1) drifts after a short month.
            var dueDate = disbursementDate.plusMonths(period);
            var interest = toMoney(remaining.amount().multiply(monthlyRate, MC), principal);
            // Last period pays off whatever is left, absorbing accumulated rounding.
            var periodPrincipal = period == months ? remaining : emi.subtract(interest);
            remaining = remaining.subtract(periodPrincipal);
            result.add(new Installment(period, dueDate, periodPrincipal, interest, remaining));
        }
        return List.copyOf(result);
    }

    private static BigDecimal emiAmount(BigDecimal principal, BigDecimal monthlyRate, int months) {
        if (monthlyRate.signum() == 0)
            return principal.divide(BigDecimal.valueOf(months), MC);
        var growth = BigDecimal.ONE.add(monthlyRate).pow(months, MC); // (1+r)^n
        return principal.multiply(monthlyRate, MC)
                .multiply(growth, MC)
                .divide(growth.subtract(BigDecimal.ONE), MC);
    }

    private static Money toMoney(BigDecimal amount, Money sameCurrencyAs) {
        var currency = sameCurrencyAs.currency();
        return new Money(amount.setScale(currency.getDefaultFractionDigits(), ROUNDING), currency);
    }
}
