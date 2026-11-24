package vn.hailuu.mini_los.loan.application;

import org.junit.jupiter.api.Test;
import vn.hailuu.mini_los.loan.domain.LoanApplication;
import vn.hailuu.mini_los.loan.domain.LoanStatus;
import vn.hailuu.mini_los.loan.infrastructure.InMemoryLoanApplicationRepository;
import vn.hailuu.mini_los.shared.Money;

import java.util.ConcurrentModificationException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Requires (not yet in production code):
 * - saveIfVersionMatches called with the version that was READ, not updated.version()
 */
class ApprovalServiceTest {
    private static final int ITERATIONS = 500;
    private static final String ID = "LA-001";
    private static final String OFFICER_A = "officer-A";
    private static final String OFFICER_B = "officer-B";
    private static final long INITIAL_VERSION = 0;

    @Test
    void approveMovesUnderReviewToApprovedAndBumpsVersion() {
        var repository = repositoryWithUnderReview();
        var service = new ApprovalService(repository);

        service.approve(ID, OFFICER_A);

        var stored = repository.findById(ID).orElseThrow();
        assertThat(stored.status()).isEqualTo(LoanStatus.APPROVED);
        assertThat(stored.decidedBy()).isEqualTo(OFFICER_A);
        assertThat(stored.version()).isEqualTo(INITIAL_VERSION + 1);
    }

    @Test
    void rejectMovesUnderReviewToRejectedAndBumpsVersion() {
        var repository = repositoryWithUnderReview();
        var service = new ApprovalService(repository);

        service.reject(ID, OFFICER_B);

        var stored = repository.findById(ID).orElseThrow();
        assertThat(stored.status()).isEqualTo(LoanStatus.REJECTED);
        assertThat(stored.decidedBy()).isEqualTo(OFFICER_B);
        assertThat(stored.version()).isEqualTo(INITIAL_VERSION + 1);
    }

    @Test
    void concurrentApproveAndRejectExactlyOneWins() throws InterruptedException {
        try (var race = new TwoOfficerRace()) {
            for (int i = 0; i < ITERATIONS; i++) {
                var repository = repositoryWithUnderReview();
                var service = new ApprovalService(repository);

                var outcome = race.run(
                        () -> service.approve(ID, OFFICER_A),
                        () -> service.reject(ID, OFFICER_B));

                // 1. Exactly one side succeeds, the other gets an exception.
                assertThat(outcome.exactlyOneSucceeded()).as("iteration %d: exactly one winner", i).isTrue();
                var approveWon = outcome.a().succeeded();
                var loser = approveWon ? outcome.b() : outcome.a();
                // Lost before reading -> domain rejects non-UNDER_REVIEW; lost after reading -> version mismatch.
                assertThat(loser.error()).as("iteration %d: loser's exception", i)
                        .isInstanceOfAny(ConcurrentModificationException.class, IllegalStateException.class);

                // 2. Final state matches the winner.
                var stored = repository.findById(ID).orElseThrow();
                assertThat(stored.status()).as("iteration %d: final status", i)
                        .isEqualTo(approveWon ? LoanStatus.APPROVED : LoanStatus.REJECTED);
                assertThat(stored.decidedBy()).as("iteration %d: decidedBy", i)
                        .isEqualTo(approveWon ? OFFICER_A : OFFICER_B);

                // 3. Version went up by exactly 1.
                assertThat(stored.version()).as("iteration %d: version", i).isEqualTo(INITIAL_VERSION + 1);
            }
        }
    }

    private static InMemoryLoanApplicationRepository repositoryWithUnderReview() {
        // InMemoryLoanApplicationRepository ships with sample application LA-001 (UNDER_REVIEW, version 0).
        var repository = new InMemoryLoanApplicationRepository();
        var seeded = repository.findById(ID).orElseThrow(() -> new AssertionError("sample " + ID + " missing"));
        assertThat(seeded.status()).as("sample status").isEqualTo(LoanStatus.UNDER_REVIEW);
        assertThat(seeded.version()).as("sample version").isEqualTo(INITIAL_VERSION);
        return repository;
    }
}
