package bank;

import bank.model.*;

/**
 * Console demo of the core model (Step 4). It shows every business rule working.
 * The GUI, controllers and database are added in later steps.
 */
public class Main {

    public static void main(String[] args) {
        // An employed customer, so a cheque account is allowed.
        Customer thabo = new Customer("C001", "Thabo", "Molefe", "Plot 12, Gaborone",
                "thabo", "pass123", true, "Choppies", "Broadhurst, Gaborone");

        SavingsAccount savings = new SavingsAccount("SAV-1001", "Main Mall", 1000.00);
        InvestmentAccount investment = new InvestmentAccount("INV-1001", "Main Mall", 2000.00);
        ChequeAccount cheque = new ChequeAccount("CHQ-1001", "Main Mall", 0);
        thabo.addAccount(savings);
        thabo.addAccount(investment);
        thabo.addAccount(cheque);

        section("1. Login check");
        System.out.println("Correct password : " + thabo.verifyCredentials("thabo", "pass123"));
        System.out.println("Wrong password   : " + thabo.verifyCredentials("thabo", "wrong"));

        section("2. Deposits (overloaded deposit methods)");
        savings.deposit(500.00);
        investment.deposit(250.00, "Bonus");
        cheque.deposit(100.00);

        section("3. Withdrawals");
        System.out.println("Savings withdraw 100   : " + savings.withdraw(100.00) + "  (must be false)");
        System.out.println("Investment withdraw 300: " + investment.withdraw(300.00));
        System.out.println("Cheque withdraw 5000   : " + cheque.withdraw(5000.00) + "  (more than balance, must be false)");

        section("4. Salary credit");
        cheque.creditSalary(8500.00);

        section("5. Monthly interest (polymorphism through the InterestBearing interface)");
        for (Account a : thabo.getAccounts()) {
            if (a instanceof InterestBearing ib) {
                double before = a.getBalance();
                ib.applyMonthlyInterest();
                System.out.printf("%-10s rate %.4f%%  %.2f -> %.2f%n",
                        a.getAccountType(), ib.getInterestRate() * 100, before, a.getBalance());
            }
        }

        section("6. Rule checks");
        try {
            new InvestmentAccount("INV-2", "Main Mall", 100.00);
        } catch (IllegalArgumentException e) {
            System.out.println("Investment under BWP500 rejected: " + e.getMessage());
        }
        Customer student = new Customer("C002", "Neo", "Kgosi", "Francistown",
                "neo", "secret", false, null, null);
        try {
            student.addAccount(new ChequeAccount("CHQ-2", "Main Mall", 0));
        } catch (IllegalStateException e) {
            System.out.println("Cheque for unemployed rejected: " + e.getMessage());
        }
        try {
            thabo.addAccount(new SavingsAccount("SAV-9", "Main Mall", 10));
        } catch (IllegalStateException e) {
            System.out.println("Fourth account rejected: " + e.getMessage());
        }

        section("7. Statement");
        for (Account a : thabo.getAccounts()) {
            System.out.println(a);
            for (Transaction t : a.getTransactions()) {
                System.out.println("   " + t);
            }
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
