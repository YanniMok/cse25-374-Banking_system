package bank.model;

/**
 * Cheque account: used for salaries; deposits and withdrawals of any amount.
 * It earns no interest, so it does not implement InterestBearing.
 * The rule that the customer must be employed is checked in Customer.addAccount().
 */
public class ChequeAccount extends Account {

    public ChequeAccount(String accountNumber, String branch, double initialBalance) {
        super(accountNumber, branch, initialBalance);
    }

    /** Credits a salary payment from the customer's employer. */
    public void creditSalary(double amount) {
        record(TransactionType.SALARY, amount, "Salary");
    }

    @Override
    public String getAccountType() {
        return "Cheque";
    }
}
