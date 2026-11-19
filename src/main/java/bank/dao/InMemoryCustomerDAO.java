package bank.dao;

import bank.model.Customer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Temporary storage that keeps customers in memory. Data is lost when the program closes. */
public class InMemoryCustomerDAO implements CustomerDAO {

    private final Map<String, Customer> customers = new LinkedHashMap<>();

    @Override
    public void save(Customer customer) {
        customers.put(customer.getUsername(), customer);
    }

    @Override
    public Customer findByUsername(String username) {
        return customers.get(username);
    }

    @Override
    public Customer findById(String customerId) {
        for (Customer c : customers.values()) {
            if (c.getCustomerID().equals(customerId)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public void delete(String customerId) {
        customers.values().removeIf(c -> c.getCustomerID().equals(customerId));
    }

    @Override
    public List<Customer> findAll() {
        return new ArrayList<>(customers.values());
    }
}
