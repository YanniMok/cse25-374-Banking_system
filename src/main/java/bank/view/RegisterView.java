package bank.view;

import bank.controller.RegistrationController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Boundary class: shows the registration form and collects the customer's details.
 * Validation and saving are done by RegistrationController.
 */
public class RegisterView extends VBox {

    private final TextField firstNameField = new TextField();
    private final TextField surnameField = new TextField();
    private final TextField addressField = new TextField();
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final CheckBox employedBox = new CheckBox("I am employed");
    private final TextField employerNameField = new TextField();
    private final TextField employerAddressField = new TextField();
    private final Label messageLabel = new Label();

    public RegisterView(RegistrationController controller, Navigator navigator) {
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(30));
        setStyle("-fx-background-color: #eef6fc;");

        Label title = new Label("Register as a customer");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1f4e79;");

        // Employer details are only needed (and only enabled) for employed customers.
        employerNameField.setDisable(true);
        employerAddressField.setDisable(true);
        employedBox.selectedProperty().addListener((obs, was, isNow) -> {
            employerNameField.setDisable(!isNow);
            employerAddressField.setDisable(!isNow);
        });
        passwordField.setPromptText("At least 6 characters");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setAlignment(Pos.CENTER);
        form.addRow(0, new Label("First name"), firstNameField);
        form.addRow(1, new Label("Surname"), surnameField);
        form.addRow(2, new Label("Address"), addressField);
        form.addRow(3, new Label("Username"), usernameField);
        form.addRow(4, new Label("Password"), passwordField);
        form.add(employedBox, 1, 5);
        form.addRow(6, new Label("Employer name"), employerNameField);
        form.addRow(7, new Label("Employer address"), employerAddressField);

        Button registerButton = new Button("Register");
        registerButton.setDefaultButton(true);
        registerButton.setPrefWidth(110);
        registerButton.setStyle("-fx-background-color: #1f4e79; -fx-text-fill: white;");
        Button backButton = new Button("Back");
        backButton.setPrefWidth(110);
        HBox buttons = new HBox(12, registerButton, backButton);
        buttons.setAlignment(Pos.CENTER);

        messageLabel.setStyle("-fx-text-fill: #b00020;");
        messageLabel.setWrapText(true);

        registerButton.setOnAction(e -> {
            try {
                controller.register(firstNameField.getText(), surnameField.getText(),
                        addressField.getText(), usernameField.getText(), passwordField.getText(),
                        employedBox.isSelected(),
                        employerNameField.getText(), employerAddressField.getText());
                new Alert(Alert.AlertType.INFORMATION,
                        "Registration successful. You can now log in.").showAndWait();
                navigator.showLogin();
            } catch (IllegalArgumentException | IllegalStateException ex) {
                messageLabel.setText(ex.getMessage());
            }
        });
        backButton.setOnAction(e -> navigator.showLogin());

        getChildren().addAll(title, form, buttons, messageLabel);
    }
}
