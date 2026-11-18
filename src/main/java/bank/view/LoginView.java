package bank.view;

import bank.controller.LoginController;
import bank.model.Customer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Boundary class: shows the login screen and collects the username and password.
 * It contains no business logic; every decision is made by LoginController.
 */
public class LoginView extends VBox {

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label messageLabel = new Label();

    public LoginView(LoginController controller, Navigator navigator) {
        setAlignment(Pos.CENTER);
        setSpacing(18);
        setPadding(new Insets(40));
        setStyle("-fx-background-color: #eef6fc;");

        Label title = new Label("Banking System");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1f4e79;");
        Label subtitle = new Label("Log in to your accounts");
        subtitle.setStyle("-fx-text-fill: #555555;");

        usernameField.setPromptText("Username");
        passwordField.setPromptText("Password");
        usernameField.setPrefWidth(240);
        passwordField.setPrefWidth(240);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.setAlignment(Pos.CENTER);
        form.addRow(0, new Label("Username"), usernameField);
        form.addRow(1, new Label("Password"), passwordField);

        Button loginButton = new Button("Log in");
        loginButton.setDefaultButton(true);
        loginButton.setPrefWidth(110);
        loginButton.setStyle("-fx-background-color: #1f4e79; -fx-text-fill: white;");
        Button registerButton = new Button("Register");
        registerButton.setPrefWidth(110);
        HBox buttons = new HBox(12, loginButton, registerButton);
        buttons.setAlignment(Pos.CENTER);

        messageLabel.setStyle("-fx-text-fill: #b00020;");
        messageLabel.setWrapText(true);

        loginButton.setOnAction(e -> {
            try {
                Customer customer = controller.login(usernameField.getText(), passwordField.getText());
                passwordField.clear();
                messageLabel.setText("");
                navigator.showAccounts(customer);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                messageLabel.setText(ex.getMessage());
                passwordField.clear();
            }
        });
        registerButton.setOnAction(e -> navigator.showRegister());

        getChildren().addAll(title, subtitle, form, buttons, messageLabel);
    }
}
