package vn.hailuu.mini_los;

import org.junit.jupiter.api.Test;
import vn.hailuu.mini_los.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AmortizationCalculatorTest {
    private static final Money LOAN = Money.vnd(100_000_000);
    private static final BigDecimal ANNUAL_RATE = new BigDecimal("0.12");
    private static final int MONTHS = 12;
    private static final LocalDate DISBURSEMENT = LocalDate.of(2026, 1, 15);

    private final AmortizationCalculator calculator = new AmortizationCalculator();

    // 1. EMI = 8.884.879 (exact value 8.884.878,87 rounded HALF_UP)
    @Test
    void calculatesCorrectMonthlyPayment() {
        List<Installment> schedule = calculator.schedule(LOAN, ANNUAL_RATE, MONTHS, DISBURSEMENT);

        Money expectedEmi = Money.vnd(8_884_879);
        // Every period except the last pays exactly EMI; the last one absorbs rounding.
        for (Installment row : schedule.subList(0, MONTHS - 1)) {
            assertThat(paymentOf(row)).as("payment of period %d", row.period()).isEqualTo(expectedEmi);
        }
    }

    // 2. Sum of principal over all periods equals the loan exactly
    @Test
    void totalPrincipalPaidEqualsLoanAmount() {
        List<Installment> schedule = calculator.schedule(LOAN, ANNUAL_RATE, MONTHS, DISBURSEMENT);

        assertThat(schedule).hasSize(MONTHS);
        assertThat(sumOfPrincipal(schedule)).isEqualTo(LOAN);
    }

    // 3. Remaining balance after the last period is zero
    @Test
    void loanIsFullyPaidAfterLastPeriod() {
        List<Installment> schedule = calculator.schedule(LOAN, ANNUAL_RATE, MONTHS, DISBURSEMENT);

        assertThat(schedule.getLast().remaining()).isEqualTo(Money.vnd(0));
    }

    // 4. Disbursed 31/01 -> first due date is the last day of February
    @Test
    void plusMonthsFallsBackToLastDayOfFebruary() {
        // Pin down the JDK behaviour this calculator relies on.
        assertThat(LocalDate.of(2026, 1, 31).plusMonths(1)).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(LocalDate.of(2028, 1, 31).plusMonths(1)).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void dueDateFallsOnLastDayOfFebruaryInCommonYear() {
        List<Installment> schedule = calculator.schedule(LOAN, ANNUAL_RATE, MONTHS, LocalDate.of(2026, 1, 31));

        assertThat(schedule.getFirst().dueDate()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void dueDateFallsOnLastDayOfFebruaryInLeapYear() {
        List<Installment> schedule = calculator.schedule(LOAN, ANNUAL_RATE, MONTHS, LocalDate.of(2028, 1, 31));

        assertThat(schedule.getFirst().dueDate()).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    // 5. Invalid input throws
    @Test
    void rejectsZeroMonths() {
        assertThatThrownBy(() -> calculator.schedule(LOAN, ANNUAL_RATE, 0, DISBURSEMENT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeMonths() {
        assertThatThrownBy(() -> calculator.schedule(LOAN, ANNUAL_RATE, -1, DISBURSEMENT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeInterestRate() {
        assertThatThrownBy(() -> calculator.schedule(LOAN, new BigDecimal("-0.01"), MONTHS, DISBURSEMENT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // 6. Zero interest rate must not divide by zero
    @Test
    void handlesZeroInterestRateWithoutDividingByZero() {
        assertThatCode(() -> calculator.schedule(LOAN, BigDecimal.ZERO, MONTHS, DISBURSEMENT))
                .doesNotThrowAnyException();

        List<Installment> schedule = calculator.schedule(LOAN, BigDecimal.ZERO, MONTHS, DISBURSEMENT);

        assertThat(schedule).hasSize(MONTHS);
        assertThat(schedule).allSatisfy(row -> assertThat(row.interest()).isEqualTo(Money.vnd(0)));
        // 100.000.000 / 12 = 8.333.333,33 -> 8.333.333 per period, last period absorbs the remainder
        assertThat(schedule.getFirst().principle()).isEqualTo(Money.vnd(8_333_333));
        assertThat(sumOfPrincipal(schedule)).isEqualTo(LOAN);
        assertThat(schedule.getLast().remaining()).isEqualTo(Money.vnd(0));
    }

    private static Money paymentOf(Installment row) {
        return row.principle().add(row.interest());
    }

    private static Money sumOfPrincipal(List<Installment> schedule) {
        return schedule.stream().map(Installment::principle).reduce(Money.vnd(0), Money::add);
    }
}
