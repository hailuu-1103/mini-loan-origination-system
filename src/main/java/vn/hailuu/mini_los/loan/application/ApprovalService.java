package vn.hailuu.mini_los.loan.application;

import org.springframework.stereotype.Service;
import vn.hailuu.mini_los.loan.domain.ILoanApplicationRepository;
import vn.hailuu.mini_los.loan.domain.LoanApplication;

import java.util.ConcurrentModificationException;

@Service
public class ApprovalService {
    private final ILoanApplicationRepository repository;

    public ApprovalService(ILoanApplicationRepository repository) {
        this.repository = repository;
    }

    // only when status == UNDER_REVIEW
    public LoanApplication approve(String id, String officer) {
        var current = repository.findById(id);
        if(current.isEmpty()) return null;
        var updated = current.get().approve(officer);
        if(!repository.saveIfVersionMatches(updated, current.get().version())) throw new ConcurrentModificationException(id);
        return updated;
    }

    // only when status == UNDER_REVIEW
    public LoanApplication reject(String id, String officer){
        var current = repository.findById(id);
        if(current.isEmpty()) return null;
        var updated = current.get().reject(officer);
        if(!repository.saveIfVersionMatches(updated, current.get().version())) throw new ConcurrentModificationException(id);
        return updated;
    }
}
