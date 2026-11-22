package vn.hailuu.mini_los.shared;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {
    public static final Currency VND = Currency.getInstance("VND");

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.signum() < 0)
            throw new IllegalArgumentException("Money cannot be negative: " + amount);
        int scale = currency.getDefaultFractionDigits(); //VND = 0, USD = 2
        if (scale < 0)
            throw new IllegalArgumentException("Unsupported currency: " + currency);
        try {
            amount = amount.setScale(scale, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("%s allows at most %d decimal places, got %s".formatted(currency, scale, amount), e);
        }
    }

    public static Money vnd(long amount) {
        return new Money(BigDecimal.valueOf(amount), VND);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor must not be null");
        // intended because multiply can cause ROUNDING issues
        BigDecimal result = amount.multiply(factor)
                .setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
        return new Money(result, currency);
    }

    public Money divide(BigDecimal factor){
        Objects.requireNonNull(factor, "factor must not be null");
        BigDecimal result = amount.divide(factor, MathContext.DECIMAL128)
                .setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
        return new Money(result, currency);
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!currency.equals(other.currency))
            throw new IllegalArgumentException("Currency mismatch: %s vs %s".formatted(currency, other.currency));
    }
}
