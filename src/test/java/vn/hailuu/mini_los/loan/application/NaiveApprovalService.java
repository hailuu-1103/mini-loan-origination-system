package vn.hailuu.mini_los.loan.application;

import vn.hailuu.mini_los.loan.domain.LoanApplication;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Control subject for the concurrency test: read with get, write with put, never checks version.
 * Even with a ConcurrentHashMap behind it, get-then-put is two separate steps, so the later
 * put silently overwrites the earlier one (lost update).
 */
final class NaiveApprovalService {
    private final Map<String, LoanApplication> store;

    NaiveApprovalService(Map<String, LoanApplication> store) {
        this.store = store;
    }

    LoanApplication approve(String id, String officer) {
        return decide(id, app -> app.approve(officer));
    }

    LoanApplication reject(String id, String officer) {
        return decide(id, app -> app.reject(officer));
    }

    private LoanApplication decide(String id, UnaryOperator<LoanApplication> decision) {
        var current = store.get(id);
        var updated = decision.apply(current);
        store.put(id, updated);
        return updated;
    }
}
