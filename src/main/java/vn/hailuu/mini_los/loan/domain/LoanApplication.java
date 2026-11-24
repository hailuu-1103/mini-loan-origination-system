package vn.hailuu.mini_los.loan.domain;

import vn.hailuu.mini_los.shared.Money;

public record LoanApplication(String id, String cif, Money amount, LoanStatus status, long version, String decidedBy)
{
    // only when status == UNDER_REVIEW
    public LoanApplication approve(String officer) {
        return decide(LoanStatus.APPROVED, officer);
    }

    // only when status == UNDER_REVIEW
    public LoanApplication reject(String officer){
        return decide(LoanStatus.REJECTED, officer);
    }

    private LoanApplication decide(LoanStatus newStatus, String officer){
        if(status != LoanStatus.UNDER_REVIEW) throw new IllegalStateException("Application " + id + "is currently: " + status + ", only decide if the status is UNDER_REVIEW");
        return new LoanApplication(id, cif, amount, newStatus, version + 1, officer);
    }
}
