package bank.view;

import bank.controller.AccountController;
import bank.controller.LoginController;
import bank.model.Account;
import bank.model.Customer;
import bank.model.Transaction;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * Boundary class: shows the customer's accounts, balances and transaction history and
 * collects their requests (deposit, withdraw, open, close, search). It contains no business
 * logic: every action is passed to AccountController, and the view only displays the result.
 */
public class AccountView extends BorderPane {

    private final AccountController controller;
    private final Customer customer;

    private final TableView<Account> accountTable = new TableView<>();
    private final TableView<Transaction> transactionTable = new TableView<>();
    private final TextField searchField = new TextField();
    private final TextField amountField = new TextField();
    private final Label statusLabel = new Label();

    public AccountView(AccountController controller, LoginController loginController,
                       Customer customer, Navigator navigator) {
        this.controller = controller;
        this.customer = customer;
        setStyle("-fx-background-color: #eef6fc;");

        setTop(buildHeader(loginController, navigator));
        setCenter(buildCenter());
        setBottom(buildActions());

        reload(null);
    }

    // ----- layout -----

    private HBox buildHeader(LoginController loginController, Navigator navigator) {
        Label welcome = new Label("Welcome, " + customer.getFirstName() + " " + customer.getSurname());
        welcome.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button logoutButton = new Button("Log out");
        logoutButton.setOnAction(e -> {
            loginController.logout();
            navigator.showLogin();
        });
        HBox header = new HBox(12, welcome, spacer, logoutButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle("-fx-background-color: #1f4e79;");
        return header;
    }

    private VBox buildCenter() {
        // accounts table
        TableColumn<Account, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAccountType()));
        TableColumn<Account, String> numberCol = new TableColumn<>("Account number");
        numberCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAccountNumber()));
        TableColumn<Account, String> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(c -> new SimpleStringProperty(money(c.getValue().getBalance())));
        accountTable.getColumns().add(typeCol);
        accountTable.getColumns().add(numberCol);
        accountTable.getColumns().add(balanceCol);
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountTable.setPlaceholder(new Label("No accounts to show"));
        accountTable.setPrefHeight(180);
        accountTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> showTransactions(selected));

        // transactions table
        TableColumn<Transaction, String> whenCol = new TableColumn<>("Date and time");
        whenCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTimestamp().withNano(0).toString().replace('T', ' ')));
        TableColumn<Transaction, String> kindCol = new TableColumn<>("Type");
        kindCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().toString()));
        TableColumn<Transaction, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(c -> new SimpleStringProperty(money(c.getValue().getAmount())));
        TableColumn<Transaction, String> afterCol = new TableColumn<>("Balance after");
        afterCol.setCellValueFactory(c -> new SimpleStringProperty(money(c.getValue().getBalanceAfter())));
        TableColumn<Transaction, String> noteCol = new TableColumn<>("Note");
        noteCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNote() == null ? "" : c.getValue().getNote()));
        transactionTable.getColumns().add(whenCol);
        transactionTable.getColumns().add(kindCol);
        transactionTable.getColumns().add(amountCol);
        transactionTable.getColumns().add(afterCol);
        transactionTable.getColumns().add(noteCol);
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        transactionTable.setPlaceholder(new Label("Select an account to see its transactions"));
        VBox.setVgrow(transactionTable, Priority.ALWAYS);

        searchField.setPromptText("Search accounts by number or type");
        searchField.textProperty().addListener((obs, old, text) -> reload(selectedNumber()));

        Label accountsTitle = sectionTitle("My accounts");
        Label historyTitle = sectionTitle("Transaction history");
        VBox center = new VBox(8, accountsTitle, searchField, accountTable, historyTitle, transactionTable);
        center.setPadding(new Insets(16, 20, 8, 20));
        return center;
    }

    private VBox buildActions() {
        amountField.setPromptText("Amount (BWP)");
        amountField.setPrefWidth(140);
        Button depositButton = new Button("Deposit");
        Button withdrawButton = new Button("Withdraw");
        Button salaryButton = new Button("Credit salary");
        HBox money = new HBox(10, amountField, depositButton, withdrawButton, salaryButton);
        money.setAlignment(Pos.CENTER_LEFT);

        Button openButton = new Button("Open account");
        Button closeButton = new Button("Close account");
        Button interestButton = new Button("Run monthly interest");
        HBox manage = new HBox(10, openButton, closeButton, interestButton);
        manage.setAlignment(Pos.CENTER_LEFT);

        depositButton.setOnAction(e -> transact(Action.DEPOSIT));
        withdrawButton.setOnAction(e -> transact(Action.WITHDRAW));
        salaryButton.setOnAction(e -> transact(Action.SALARY));
        openButton.setOnAction(e -> openAccount());
        closeButton.setOnAction(e -> closeAccount());
        interestButton.setOnAction(e -> attempt(() -> {
            int count = controller.payMonthlyInterest(customer);
            reload(selectedNumber());
            showSuccess("Interest paid into " + count + " account(s).");
        }));

        statusLabel.setWrapText(true);
        VBox actions = new VBox(10, money, manage, statusLabel);
        actions.setPadding(new Insets(8, 20, 16, 20));
        return actions;
    }

    // ----- user actions (each one only forwards to the controller and shows the result) -----

    private enum Action { DEPOSIT, WITHDRAW, SALARY }

    private void transact(Action action) {
        attempt(() -> {
            Account account = requireSelection();
            double amount = readAmount();
            switch (action) {
                case DEPOSIT -> controller.deposit(customer, account.getAccountNumber(), amount);
                case WITHDRAW -> controller.withdraw(customer, account.getAccountNumber(), amount);
                case SALARY -> controller.creditSalary(customer, account.getAccountNumber(), amount);
            }
            amountField.clear();
            reload(account.getAccountNumber());
            showSuccess("Done. New balance: " + money(account.getBalance()));
        });
    }

    private void openAccount() {
        ChoiceDialog<String> typeDialog = new ChoiceDialog<>("Savings", "Savings", "Investment", "Cheque");
        typeDialog.setTitle("Open account");
        typeDialog.setHeaderText("Choose the type of account to open");
        Optional<String> type = typeDialog.showAndWait();
        if (type.isEmpty()) {
            return;
        }
        TextInputDialog depositDialog = new TextInputDialog("0");
        depositDialog.setTitle("Open account");
        depositDialog.setHeaderText("Opening deposit (BWP). An investment account needs at least 500.");
        Optional<String> text = depositDialog.showAndWait();
        if (text.isEmpty()) {
            return;
        }
        attempt(() -> {
            double deposit = parseAmount(text.get());
            Account opened = controller.openAccount(customer, type.get(), deposit);
            reload(opened.getAccountNumber());
            showSuccess(type.get() + " account " + opened.getAccountNumber() + " opened.");
        });
    }

    private void closeAccount() {
        attempt(() -> {
            Account account = requireSelection();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Close account " + account.getAccountNumber() + "?", ButtonType.YES, ButtonType.NO);
            if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                return;
            }
            double paidOut = controller.closeAccount(customer, account.getAccountNumber());
            reload(null);
            showSuccess(paidOut > 0
                    ? "Account closed. " + money(paidOut) + " paid out."
                    : "Account closed.");
        });
    }

    // ----- helpers for displaying things -----

    /** Runs an action and shows any message from the controller instead of crashing. */
    private void attempt(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        }
    }

    private Account requireSelection() {
        Account account = accountTable.getSelectionModel().getSelectedItem();
        if (account == null) {
            throw new IllegalStateException("Select an account first.");
        }
        return account;
    }

    private double readAmount() {
        return parseAmount(amountField.getText());
    }

    /** Turns typed text into a number (a format check only; the rules are in the controller). */
    private double parseAmount(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Enter a valid amount, for example 250.00.");
        }
    }

    private String selectedNumber() {
        Account selected = accountTable.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.getAccountNumber();
    }

    /** Reloads the accounts table (respecting the search text) and re-selects an account. */
    private void reload(String numberToSelect) {
        accountTable.setItems(FXCollections.observableArrayList(
                controller.searchAccounts(customer, searchField.getText())));
        accountTable.refresh();
        if (numberToSelect != null) {
            for (Account a : accountTable.getItems()) {
                if (a.getAccountNumber().equals(numberToSelect)) {
                    accountTable.getSelectionModel().select(a);
                    break;
                }
            }
        }
        showTransactions(accountTable.getSelectionModel().getSelectedItem());
    }

    private void showTransactions(Account account) {
        if (account == null) {
            transactionTable.setItems(FXCollections.observableArrayList());
        } else {
            transactionTable.setItems(FXCollections.observableArrayList(controller.getTransactions(account)));
        }
    }

    private static String money(double value) {
        return String.format("BWP %,.2f", value);
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1f4e79;");
        return label;
    }

    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #b00020;");
        statusLabel.setText(message);
    }

    private void showSuccess(String message) {
        statusLabel.setStyle("-fx-text-fill: #1b6e2d;");
        statusLabel.setText(message);
    }
}
