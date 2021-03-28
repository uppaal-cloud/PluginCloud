package com.uppaal.cloud.ui;

import com.uppaal.cloud.util.UppaalCloudAPIClient;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class LoginView extends JPanel {
    private final UppaalCloudAPIClient apiClient;

    private final JTextField emailField;
    private final JTextField passwordField;
    private final JTextField errorField;

    private final Callback loggedInCallback;

    public LoginView(UppaalCloudAPIClient client, Callback callback) {
        super();
        this.loggedInCallback = callback;
        this.apiClient = client;
        // Create login layout
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(Component.CENTER_ALIGNMENT);
        setAlignmentY(Component.CENTER_ALIGNMENT);

        setPreferredSize(new Dimension(400, 180));
        setMaximumSize(new Dimension(400, 180));
        TitledBorder title = BorderFactory.createTitledBorder("Login");
        title.setTitleJustification(TitledBorder.CENTER);
        setBorder(title);

        JPanel usernameRow = new JPanel();
        usernameRow.add(new JLabel("Username: "));
        emailField = new JTextField("");
        emailField.setPreferredSize(new Dimension(128, emailField.getPreferredSize().height));
        usernameRow.add(emailField);
        add(usernameRow);

        JPanel passwordRow = new JPanel();
        passwordRow.add(new JLabel("Password: "));
        passwordField = new JPasswordField("");
        passwordField.setPreferredSize(new Dimension(128, passwordField.getPreferredSize().height));
        passwordRow.add(passwordField);
        add(passwordRow);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> login());
        add(loginButton);

        add(Box.createHorizontalStrut(10));
        add(new JLabel("Don't have an account yet?"));

        JLabel hyperlink = new JLabel("Click here to sign up");
        hyperlink.setForeground(Color.BLUE.darker());
        hyperlink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // The user clicks on the label
                try {
                    Desktop.getDesktop().browse(new URI(UppaalCloudAPIClient.API_URL+"/signup"));
                } catch (IOException | URISyntaxException e1) {
                    // Do nothing
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // the mouse has entered the label
                hyperlink.setText("<html><a href=''>Click here to sign up</a></html>");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // the mouse has exited the label
                hyperlink.setText("Click here to sign up");
            }
        });
        add(hyperlink);

        // Set a label for an error message
        errorField = new JTextField("Some error");
        errorField.setBorder(new LineBorder(Color.RED,2));
        errorField.setMaximumSize(new Dimension(400, 10));
        errorField.setVisible(false);
        add(errorField);
    }

    private void login() {
        String email = emailField.getText();
        String password = passwordField.getText();

        apiClient.setCredentials(email, password);
        try {
            apiClient.login();
            errorField.setVisible(false);
            // Call the main UI to hide the credentials panel and transition
            loggedInCallback.callback(UiAction.LOGGED_IN);
        } catch(Exception e) {
            // Failed to login
            displayError(e.getMessage());
        }
    }

    private void displayError(String error) {
        // Display an error message
        errorField.setText(error);
        errorField.setVisible(true);

        revalidate();
        repaint();
    }

    public void refreshView() {

    }
}
