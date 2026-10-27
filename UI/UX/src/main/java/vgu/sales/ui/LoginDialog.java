package vgu.sales.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import vgu.sales.controller.AuthService;
import vgu.sales.controller.AuthenticationException;
import vgu.sales.controller.model.Staff;

/**
 * LoginDialog.
 * A modal dialog for staff login that authenticates through AuthService.
 * Public: App opens it at start-up and after logout.
 */
public class LoginDialog extends JDialog {
    /** Private: used internally for authentication. */
    private final AuthService auth;
    /** Private: only this dialog reads the field; callers use getUsername(). */
    private final JTextField txtUsername = new JTextField(18);
    /** Private: only this dialog reads the password internally. */
    private final JPasswordField pwdPassword = new JPasswordField(18);
    /** Private: only this dialog handles the login action. */
    private final JButton btnLogin = new JButton("Login");
    /** Private: only this dialog handles the cancel action. */
    private final JButton btnCancel = new JButton("Cancel");
    /** Private: only this dialog handles opening the sign-up dialog. */
    private final JButton btnSignUp = new JButton("Create account...");
    /** Private: only this dialog updates the status/error message. */
    private final JLabel lblError = new JLabel(" ");
    /** Private: callers read it through getStaff(). */
    private Staff staff;

    /**
     * Constructor: displays the dialog UI, authenticating through AuthService.
     * Public: used by App and MainWindow to initiate staff login.
     *
     * @param owner parent Frame; dialog is centered over this
     * @param auth  authentication service; must not be null
     */
    public LoginDialog(Frame owner, AuthService auth) {
        super(owner, "Staff Login", true);
        if (auth == null) {
            throw new IllegalArgumentException("auth must not be null");
        }
        this.auth = auth;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Username label + field
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        panel.add(new JLabel("Username:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(txtUsername, c);

        // Row 1: Password label + field
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        panel.add(new JLabel("Password:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(pwdPassword, c);

        // Row 2: Error label (spans 2 columns)
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.weightx = 1.0;
        lblError.setForeground(new Color(180, 0, 0));
        panel.add(lblError, c);
        c.gridwidth = 1;

        // Row 3: Button panel (spans 2 columns)
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightButtons.add(btnLogin);
        rightButtons.add(Box.createHorizontalStrut(8));
        rightButtons.add(btnCancel);

        JPanel buttonsPanel = new JPanel(new BorderLayout(8, 0));
        buttonsPanel.add(btnSignUp, BorderLayout.WEST);
        buttonsPanel.add(rightButtons, BorderLayout.EAST);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.weightx = 1.0;
        panel.add(buttonsPanel, c);
        c.gridwidth = 1;

        btnLogin.addActionListener(e -> onLogin());
        btnCancel.addActionListener(e -> onCancel());
        btnSignUp.addActionListener(e -> onSignUp());
        getRootPane().setDefaultButton(btnLogin);
        setContentPane(panel);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /**
     * Returns whether the dialog was confirmed with successful login.
     * Public: callers outside this class inspect login success.
     *
     * @return true if login succeeded, false otherwise
     */
    public boolean isConfirmed() {
        return staff != null;
    }

    /**
     * Returns the authenticated staff member if login was successful.
     * Public: allows callers to retrieve the authenticated staff profile.
     *
     * @return optional containing the authenticated Staff, or empty if cancelled
     */
    public Optional<Staff> getStaff() {
        return Optional.ofNullable(staff);
    }

    /**
     * Returns the trimmed username from the username field.
     * Public: allows callers to read username after dialog closes.
     *
     * @return trimmed username string
     */
    public String getUsername() {
        return txtUsername.getText().trim();
    }

    /**
     * Clears the password field text for security.
     * Public: used after authentication attempts or account creation.
     */
    public void clearPassword() {
        pwdPassword.setText("");
    }

    /**
     * Shows an error message in the status label (red foreground) and repacks dialog.
     * Public: callers can display custom error messages.
     *
     * @param message error text to show
     */
    public void showError(String message) {
        lblError.setForeground(new Color(180, 0, 0));
        lblError.setText(message);
        pack();
    }

    /**
     * Shows an informational message in the status label (green foreground) and repacks dialog.
     * Public: callers can display informational messages.
     *
     * @param message info text to show
     */
    public void showInfo(String message) {
        lblError.setForeground(new Color(0, 110, 0));
        lblError.setText(message);
        pack();
    }

    /**
     * Authenticates credentials using AuthService and hides dialog on success.
     * Private: called only by Login button handler.
     */
    private void onLogin() {
        char[] password = pwdPassword.getPassword();
        try {
            staff = auth.login(getUsername(), password);
            clearPassword();
            setVisible(false);
        } catch (AuthenticationException | IllegalArgumentException ex) {
            clearPassword();
            showError(ex.getMessage());
            pwdPassword.requestFocusInWindow();
        } catch (RuntimeException ex) {
            showError("Cannot reach the database: " + ex.getMessage());
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Opens the sign-up dialog to register a new staff account.
     * Private: called only by the Create Account button handler.
     */
    private void onSignUp() {
        SignUpDialog dialog = new SignUpDialog(this, auth);
        dialog.setVisible(true);
        dialog.getCreatedStaff().ifPresent(created -> {
            txtUsername.setText(created.getUsername());
            clearPassword();
            showInfo("Account created. Sign in with your new password.");
            pwdPassword.requestFocusInWindow();
        });
    }

    /**
     * Handles cancel action: clears staff and disposes dialog.
     * Private: called only by Cancel button handler.
     */
    private void onCancel() {
        staff = null;
        dispose();
    }
}
