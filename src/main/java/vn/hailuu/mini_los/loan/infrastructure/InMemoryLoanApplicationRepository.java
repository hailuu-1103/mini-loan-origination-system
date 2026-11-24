package vn.hailuu.mini_los.loan.infrastructure;

import org.springframework.stereotype.Repository;
import vn.hailuu.mini_los.loan.domain.LoanApplication;
import vn.hailuu.mini_los.loan.domain.ILoanApplicationRepository;
import vn.hailuu.mini_los.loan.domain.LoanStatus;
import vn.hailuu.mini_los.shared.Money;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Repository
public class InMemoryLoanApplicationRepository implements ILoanApplicationRepository {
    private final ConcurrentHashMap<String, LoanApplication> cachedApplication = new ConcurrentHashMap<>();
    private static final Object LOCK = new Object();

    // Sample data for learning: one application waiting for an officer's decision.
    public InMemoryLoanApplicationRepository() {
        var sample = new LoanApplication("LA-001", "CIF-001", Money.vnd(100_000_000),
                LoanStatus.UNDER_REVIEW, 0, null);
        cachedApplication.put(sample.id(), sample);
    }

    @Override
    public Optional<LoanApplication> findById(String id) {
        return Optional.ofNullable(cachedApplication.get(id));
    }

    @Override
    public boolean saveIfVersionMatches(LoanApplication updated, long expectedVersion) {
        var saved = new AtomicBoolean(false);
        cachedApplication.computeIfPresent(updated.id(), (_, current) -> {
            synchronized (LOCK){
                if(current.version() != expectedVersion) return current;
                saved.set(true);
                return updated;
            }
        });
        return saved.get();
    }
}
