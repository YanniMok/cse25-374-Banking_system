package bank.dao;

import bank.model.*;

/**
 * Creates 10 sample customers (with accounts and transactions) for testing and for the
 * presentation. The password of every sample customer is "pass123", except neo ("secret1").
 */
public final class SampleData {

    private static final String BRANCH = "Main Mall";
    private static int nextNumber = 1001;

    private SampleData() { }

    /** Adds the sample customers only if the database has no customers yet. */
    public static void seedIfEmpty(CustomerDAO dao) {
        if (dao.findAll().isEmpty()) {
            seed(dao);
        }
    }

    public static void seed(CustomerDAO dao) {
        nextNumber = 1001;

        // 1. Employed, all three account types
        Customer thabo = customer("C001", "Thabo", "Molefe", "Plot 12, Gaborone", "thabo", "pass123",
                "Choppies", "Broadhurst, Gaborone");
        SavingsAccount thaboSavings = savings(thabo, 1000.00);
        InvestmentAccount thaboInvestment = investment(thabo, 2000.00);
        ChequeAccount thaboCheque = cheque(thabo, 0);
        thaboSavings.deposit(500.00);
        thaboInvestment.deposit(250.00, "Bonus");
        thaboInvestment.withdraw(300.00);
        thaboCheque.creditSalary(8500.00);
        thaboCheque.withdraw(1200.00);
        thaboSavings.applyMonthlyInterest();
        thaboInvestment.applyMonthlyInterest();
        dao.save(thabo);

        // 2. Not employed, savings only
        Customer neo = customer("C002", "Neo", "Kgosi", "Block 6, Francistown", "neo", "secret1", null, null);
        savings(neo, 300.00).deposit(120.00);
        dao.save(neo);

        // 3. Employed: cheque and savings
        Customer lesego = customer("C003", "Lesego", "Dube", "Mathangwane, Maun", "lesego", "pass123",
                "Okavango Foods", "Industrial Site, Maun");
        ChequeAccount lesegoCheque = cheque(lesego, 0);
        lesegoCheque.creditSalary(7200.00);
        lesegoCheque.withdraw(900.00);
        savings(lesego, 1500.00);
        dao.save(lesego);

        // 4. Employed: cheque and investment
        Customer kagiso = customer("C004", "Kagiso", "Seretse", "Plot 88, Selebi-Phikwe", "kagiso", "pass123",
                "Kalahari Mining Co", "Mine Road, Selebi-Phikwe");
        ChequeAccount kagisoCheque = cheque(kagiso, 0);
        kagisoCheque.creditSalary(12500.00);
        kagisoCheque.withdraw(2000.00);
        InvestmentAccount kagisoInvestment = investment(kagiso, 5000.00);
        kagisoInvestment.applyMonthlyInterest();
        dao.save(kagiso);

        // 5. Not employed: savings and investment
        Customer boitumelo = customer("C005", "Boitumelo", "Sithole", "Ward 4, Lobatse", "boitumelo", "pass123",
                null, null);
        savings(boitumelo, 800.00).applyMonthlyInterest();
        investment(boitumelo, 1200.00).deposit(300.00);
        dao.save(boitumelo);

        // 6. Employed, all three account types
        Customer tebogo = customer("C006", "Tebogo", "Mosweu", "Phase 2, Gaborone", "tebogo", "pass123",
                "Gaborone Hardware", "Tlokweng Road, Gaborone");
        savings(tebogo, 2500.00);
        InvestmentAccount tebogoInvestment = investment(tebogo, 4000.00);
        tebogoInvestment.withdraw(1000.00);
        cheque(tebogo, 0).creditSalary(6400.00);
        dao.save(tebogo);

        // 7. Not employed, small savings
        Customer masego = customer("C007", "Masego", "Phiri", "Kanye Ward 2", "masego", "pass123", null, null);
        savings(masego, 150.00);
        dao.save(masego);

        // 8. Employed, cheque only
        Customer onalenna = customer("C008", "Onalenna", "Ramotswe", "Extension 9, Gaborone", "onalenna", "pass123",
                "Delta Logistics", "Airport Road, Gaborone");
        ChequeAccount onalennaCheque = cheque(onalenna, 100.00);
        onalennaCheque.creditSalary(9800.00);
        onalennaCheque.withdraw(3500.00);
        dao.save(onalenna);

        // 9. Not employed, investment and a small savings account
        Customer gofaone = customer("C009", "Gofaone", "Tau", "Mochudi Ward 5", "gofaone", "pass123", null, null);
        investment(gofaone, 750.00).applyMonthlyInterest();
        savings(gofaone, 50.00);
        dao.save(gofaone);

        // 10. Employed: cheque and investment
        Customer mpho = customer("C010", "Mpho", "Kebonang", "Block 3, Gaborone", "mpho", "pass123",
                "Savannah Bank", "Main Mall, Gaborone");
        cheque(mpho, 0).creditSalary(15000.00);
        InvestmentAccount mphoInvestment = investment(mpho, 2500.00);
        mphoInvestment.deposit(500.00);
        mphoInvestment.applyMonthlyInterest();
        dao.save(mpho);
    }

    // ----- small helpers so the data above is easy to read -----

    private static Customer customer(String id, String first, String surname, String address,
                                     String username, String password, String employer, String employerAddress) {
        return new Customer(id, first, surname, address, username, password,
                employer != null, employer, employerAddress);
    }

    private static SavingsAccount savings(Customer owner, double opening) {
        SavingsAccount account = new SavingsAccount("SAV-" + nextNumber++, BRANCH, opening);
        owner.addAccount(account);
        return account;
    }

    private static InvestmentAccount investment(Customer owner, double opening) {
        InvestmentAccount account = new InvestmentAccount("INV-" + nextNumber++, BRANCH, opening);
        owner.addAccount(account);
        return account;
    }

    private static ChequeAccount cheque(Customer owner, double opening) {
        ChequeAccount account = new ChequeAccount("CHQ-" + nextNumber++, BRANCH, opening);
        owner.addAccount(account);
        return account;
    }
}
