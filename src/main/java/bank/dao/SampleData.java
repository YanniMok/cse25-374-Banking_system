package bank.dao;

import bank.model.*;

/** Creates a few customers so the application has something to show at start-up. */
public final class SampleData {

    private SampleData() { }

    public static void seed(CustomerDAO dao) {
        Customer thabo = new Customer("C001", "Thabo", "Molefe", "Plot 12, Gaborone",
                "thabo", "pass123", true, "Choppies", "Broadhurst, Gaborone");
        thabo.addAccount(new SavingsAccount("SAV-1001", "Main Mall", 1000.00));
        thabo.addAccount(new InvestmentAccount("INV-1002", "Main Mall", 2000.00));
        ChequeAccount cheque = new ChequeAccount("CHQ-1003", "Main Mall", 0);
        thabo.addAccount(cheque);
        cheque.creditSalary(8500.00);
        dao.save(thabo);

        Customer neo = new Customer("C002", "Neo", "Kgosi", "Block 6, Francistown",
                "neo", "secret1", false, null, null);
        neo.addAccount(new SavingsAccount("SAV-1004", "Main Mall", 300.00));
        dao.save(neo);
    }
}
