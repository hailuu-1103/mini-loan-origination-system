package vn.hailuu.mini_los.loan.domain;

import java.util.Optional;

public interface ILoanApplicationRepository {
    Optional<LoanApplication> findById(String id);
    // only save if current version == expected version, false if it is already edited before.
    boolean saveIfVersionMatches(LoanApplication updated, long expectedVersion);
}
