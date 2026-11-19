package bank.controller;

import bank.dao.CustomerDAO;
import bank.model.Customer;

/** Control class for registering a new customer. */
public class RegistrationController {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final CustomerDAO dao;

    public RegistrationController(CustomerDAO dao) {
        this.dao = dao;
    }

    public Customer register(String firstName, String surname, String address,
                             String username, String password,
                             boolean employed, String employerName, String employerAddress) {
        if (isBlank(firstName) || isBlank(surname) || isBlank(address) || isBlank(username)) {
            throw new IllegalArgumentException("First name, surname, address and username are required.");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        String name = username.trim();
        if (dao.findByUsername(name) != null) {
            throw new IllegalArgumentException("That username is already taken.");
        }
        String id = String.format("C%03d", nextCustomerNumber());
        // The Customer constructor checks that an employed customer gave employer details.
        Customer customer = new Customer(id, firstName.trim(), surname.trim(), address.trim(),
                name, password, employed,
                employed ? employerName.trim() : null,
                employed ? employerAddress.trim() : null);
        dao.save(customer);
        return customer;
    }

    /** One more than the highest existing customer number, so IDs never repeat. */
    private int nextCustomerNumber() {
        int highest = 0;
        for (Customer c : dao.findAll()) {
            String id = c.getCustomerID();
            if (id.length() > 1 && id.startsWith("C")) {
                try {
                    highest = Math.max(highest, Integer.parseInt(id.substring(1)));
                } catch (NumberFormatException ignored) {
                    // not an ID we generated; skip it
                }
            }
        }
        return highest + 1;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
