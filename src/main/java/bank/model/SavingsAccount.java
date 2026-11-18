package bank.model;

/**
 * Savings account: deposits only, no withdrawals, 0.05% interest per month.
 */
public class SavingsAccount extends Account implements InterestBearing {

    private static final double INTEREST_RATE = 0.0005; // 0.05% per month

    public SavingsAccount(String accountNumber, String branch, double initialBalance) {
        super(accountNumber, branch, initialBalance);
    }

    /** Overrides Account.withdraw: a savings account never allows withdrawals. */
    @Override
    public boolean withdraw(double amount) {
        return false;
    }

    @Override
    public double getInterestRate() {
        return INTEREST_RATE;
    }

    @Override
    public void applyMonthlyInterest() {
        double interest = Math.round(getBalance() * INTEREST_RATE * 100.0) / 100.0;
        if (interest > 0) {
            record(TransactionType.INTEREST, interest, "Monthly interest");
        }
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }
}
