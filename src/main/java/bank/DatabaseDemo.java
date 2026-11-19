package bank;

import bank.dao.CustomerDAO;
import bank.dao.DatabaseManager;
import bank.dao.JdbcCustomerDAO;
import bank.dao.SampleData;
import bank.model.Account;
import bank.model.Customer;
import bank.model.InvestmentAccount;

import java.io.File;

/**
 * Console demo of the database layer (Step 7). It uses its own file, demo.db, so it never
 * touches the application's bank.db. Run it with:
 *   mvn compile exec:java -Dexec.mainClass=bank.DatabaseDemo
 */
public class DatabaseDemo {

    public static void main(String[] args) {
        new File("demo.db").delete(); // start clean every time
        DatabaseManager database = new DatabaseManager("demo.db");
        database.initialize();
        CustomerDAO dao = new JdbcCustomerDAO(database);

        System.out.println("=== CREATE: insert the 10 sample customers ===");
        SampleData.seed(dao);
        System.out.println("Customers stored: " + dao.findAll().size());

        System.out.println();
        System.out.println("=== READ: customers and their accounts ===");
        for (Customer c : dao.findAll()) {
            System.out.println(c);
            for (Account a : c.getAccounts()) {
                System.out.println("    " + a + "  (" + a.getTransactions().size() + " transactions)");
            }
        }

        System.out.println();
        System.out.println("=== UPDATE: deposit BWP 250.00 into Neo's savings account ===");
        Customer neo = dao.findByUsername("neo");
        System.out.println("Before: " + neo.getAccounts().get(0));
        neo.getAccounts().get(0).deposit(250.00, "Demo deposit");
        dao.save(neo);
        Customer neoAgain = dao.findByUsername("neo");   // read it back from the database
        System.out.println("After : " + neoAgain.getAccounts().get(0));

        System.out.println();
        System.out.println("=== UPDATE: open an investment account, then close it ===");
        Customer masego = dao.findByUsername("masego");
        System.out.println("Masego accounts before: " + masego.getAccounts().size());
        InvestmentAccount extra = new InvestmentAccount("INV-9999", "Main Mall", 600.00);
        masego.addAccount(extra);
        dao.save(masego);
        System.out.println("After opening INV-9999 : " + dao.findByUsername("masego").getAccounts().size());
        extra.withdraw(600.00);                  // an investment account must be empty to close
        masego.removeAccount("INV-9999");        // closing deletes the row from the account table
        dao.save(masego);
        System.out.println("After closing INV-9999 : " + dao.findByUsername("masego").getAccounts().size());

        System.out.println();
        System.out.println("=== DELETE: remove customer C010 (accounts and transactions go too) ===");
        dao.delete("C010");
        System.out.println("Customers left: " + dao.findAll().size()
                + "   C010 found: " + (dao.findById("C010") != null));
    }
}
