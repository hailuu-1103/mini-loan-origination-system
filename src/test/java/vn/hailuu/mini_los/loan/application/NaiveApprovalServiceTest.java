package vn.hailuu.mini_los.loan.application;

import org.junit.jupiter.api.Test;
import vn.hailuu.mini_los.loan.domain.LoanApplication;
import vn.hailuu.mini_los.loan.domain.LoanStatus;
import vn.hailuu.mini_los.shared.Money;

import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/** Control experiment: proves the lost update that ApprovalService's version check prevents. */
class NaiveApprovalServiceTest {
    private static final int ITERATIONS = 500;
    private static final String ID = "LA-001";
    private static final long INITIAL_VERSION = 0;

    @Test
    void getThenPutWithoutVersionCheckLetsBothOfficersWin() throws InterruptedException {
        int overwritten = 0;

        try (var race = new TwoOfficerRace()) {
            for (int i = 0; i < ITERATIONS; i++) {
                var store = new ConcurrentHashMap<String, LoanApplication>();
                store.put(ID, underReview());
                var service = new NaiveApprovalService(store);

                var outcome = race.run(
                        () -> service.approve(ID, "officer-A"),
                        () -> service.reject(ID, "officer-B"));

                if (!outcome.bothSucceeded()) continue;
                overwritten++;

                // Both officers were told "success", but only the last put survives.
                var stored = store.get(ID);
                assertThat(stored.version()).as("iteration %d", i).isEqualTo(INITIAL_VERSION + 1);
                assertThat(stored).as("iteration %d", i)
                        .isIn(outcome.a().result(), outcome.b().result());
            }
        }

        System.out.printf("NaiveApprovalService: %d/%d runs overwritten (both officers succeeded)%n",
                overwritten, ITERATIONS);
        assertThat(overwritten).as("lost updates out of %d", ITERATIONS).isPositive();
    }

    private static LoanApplication underReview() {
        return new LoanApplication(ID, "CIF-001", Money.vnd(100_000_000),
                LoanStatus.UNDER_REVIEW, INITIAL_VERSION, null);
    }
}
