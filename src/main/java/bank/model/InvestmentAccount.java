package bank.model;

/**
 * Investment account: deposits and withdrawals allowed, 5% interest per month.
 * It can only be opened with an initial deposit of at least BWP 500.00.
 */
public class InvestmentAccount extends Account implements InterestBearing {

    public static final double MIN_OPENING_DEPOSIT = 500.00;
    private static final double INTEREST_RATE = 0.05; // 5% per month

    public InvestmentAccount(String accountNumber, String branch, double initialDeposit) {
        super(accountNumber, branch, checkOpeningDeposit(initialDeposit));
    }

    /** Runs before the Account constructor so a bad account is never created. */
    private static double checkOpeningDeposit(double initialDeposit) {
        if (initialDeposit < MIN_OPENING_DEPOSIT) {
            throw new IllegalArgumentException(String.format(
                    "An investment account needs an opening deposit of at least BWP %.2f.",
                    MIN_OPENING_DEPOSIT));
        }
        return initialDeposit;
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
        return "Investment";
    }
}
