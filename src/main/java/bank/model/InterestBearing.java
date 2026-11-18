package bank.model;

/**
 * Interface for accounts that earn interest.
 * SavingsAccount and InvestmentAccount implement it; ChequeAccount does not.
 */
public interface InterestBearing {

    /** Monthly interest rate as a fraction (0.05 means 5%). */
    double getInterestRate();

    /** Calculates this month's interest and adds it to the balance. */
    void applyMonthlyInterest();
}
