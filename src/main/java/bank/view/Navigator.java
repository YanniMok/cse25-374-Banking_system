package bank.view;

import bank.model.Customer;

/** Lets a view ask the application to switch screen, without knowing how screens are built. */
public interface Navigator {
    void showLogin();
    void showRegister();
    void showAccounts(Customer customer);
}
