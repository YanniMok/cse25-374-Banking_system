package bank;

import bank.controller.AccountController;
import bank.controller.LoginController;
import bank.controller.RegistrationController;
import bank.dao.CustomerDAO;
import bank.dao.DatabaseManager;
import bank.dao.JdbcCustomerDAO;
import bank.dao.SampleData;
import bank.model.Customer;
import bank.view.AccountView;
import bank.view.LoginView;
import bank.view.Navigator;
import bank.view.RegisterView;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Starts the JavaFX application. It sets up the SQLite database, then creates the data access object and the controllers once,
 * and switches between the screens (views) when asked through the Navigator interface.
 * Run it with:  mvn javafx:run
 */
public class BankingApp extends Application implements Navigator {

    private Stage stage;
    private LoginController loginController;
    private RegistrationController registrationController;
    private AccountController accountController;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;

        DatabaseManager database = DatabaseManager.forDefaultFile(); // bank.db in the project folder
        database.initialize();                                       // creates the tables if needed
        CustomerDAO dao = new JdbcCustomerDAO(database);
        SampleData.seedIfEmpty(dao);                                  // adds 10 sample customers on first run
        loginController = new LoginController(dao);
        registrationController = new RegistrationController(dao);
        accountController = new AccountController(dao);

        stage.setTitle("Banking System");
        showLogin();
        stage.show();
    }

    @Override
    public void showLogin() {
        display(new LoginView(loginController, this), 460, 440);
    }

    @Override
    public void showRegister() {
        display(new RegisterView(registrationController, this), 520, 560);
    }

    @Override
    public void showAccounts(Customer customer) {
        display(new AccountView(accountController, loginController, customer, this), 900, 680);
    }

    private void display(Parent root, double width, double height) {
        stage.setScene(new Scene(root, width, height));
        stage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
