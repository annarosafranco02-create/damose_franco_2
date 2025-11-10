package it.damose.ui;

import it.damose.controller.UserController;

import javax.swing.*;
import java.awt.*;

/**
 * Finestra di dialogo modale per Login, Registrazione e modalità Ospite.
 */
public class LoginWindow extends JDialog {

    private UserController userController;

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblStatus;

    // Questo è il "risultato" della finestra
    private String loggedInUsername = null;
    private boolean proceed = false; // true se l'utente ha fatto una scelta

    public LoginWindow(JFrame parent, UserController controller) {
        super(parent, "Accesso - Rome Transit Tracker", true); // true = Modale
        this.userController = controller;

        setSize(400, 250);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // Chiudere la 'X' è come annullare
        setLayout(new BorderLayout(10, 10));

        initLayout();
    }

    private void initLayout() {
        // Pannello per i campi di input
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        txtUsername = new JTextField(20);
        formPanel.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        txtPassword = new JPasswordField(20);
        formPanel.add(txtPassword, gbc);

        // Pannello per i bottoni
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnLogin = new JButton("Login");
        JButton btnRegister = new JButton("Registrati");
        JButton btnGuest = new JButton("Entra come Ospite");

        buttonPanel.add(btnLogin);
        buttonPanel.add(btnRegister);
        buttonPanel.add(btnGuest);

        // Etichetta di stato
        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setForeground(Color.RED);

        // Aggiungi i pannelli alla finestra
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(lblStatus, BorderLayout.NORTH);

        // Action Listeners
        btnLogin.addActionListener(e -> doLogin());
        btnRegister.addActionListener(e -> doRegister());
        btnGuest.addActionListener(e -> doGuest());

        // Permetti di premere "Invio" per il login
        txtPassword.addActionListener(e -> doLogin());
    }

    private void doLogin() {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());

        if (userController.login(username, password)) {
            // Login riuscito
            this.loggedInUsername = username.toLowerCase();
            this.proceed = true;
            dispose(); // Chiude la finestra di dialogo
        } else {
            lblStatus.setText("Username o password errati.");
        }
    }

    private void doRegister() {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());

        int result = userController.register(username, password);
        if (result == 0) {
            lblStatus.setForeground(Color.BLUE);
            lblStatus.setText("Registrazione riuscita! Ora puoi fare il login.");
        } else if (result == 1) {
            lblStatus.setForeground(Color.RED);
            lblStatus.setText("Username già esistente.");
        } else {
            lblStatus.setForeground(Color.RED);
            lblStatus.setText("Username o password non validi.");
        }
    }

    private void doGuest() {
        // Ospite: loggedInUsername rimane null
        this.loggedInUsername = null;
        this.proceed = true;
        dispose(); // Chiude la finestra di dialogo
    }

    /**
     * Metodo pubblico per ottenere il risultato dopo la chiusura della finestra.
     * @return L'username se il login ha avuto successo, null se è Ospite.
     */
    public String getLoggedInUsername() {
        return loggedInUsername;
    }

    /**
     * Metodo per sapere se l'utente ha fatto una scelta o ha chiuso la finestra
     */
    public boolean didProceed() {
        return proceed;
    }
}