package vn.hailuu.mini_los;

import vn.hailuu.mini_los.shared.Money;

import java.time.LocalDate;

public record Installment(int period, LocalDate dueDate, Money principle, Money interest, Money remaining) {

}
