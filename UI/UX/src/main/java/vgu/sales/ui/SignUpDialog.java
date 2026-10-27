package vgu.sales.ui;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
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
import vgu.sales.controller.model.Staff;

/**
 * SignUpDialog
 * Modal dialog for staff self sign-up (SCR-16).
 * Public: opened by LoginDialog when creating a new staff account.
 */
public class SignUpDialog extends JDialog {

    /** Private: service reference used only inside this dialog. */
    private final AuthService auth;

    /** Private: input field encapsulated within this dialog. */
    private final JTextField txtUsername = new JTextField(20);

    /** Private: input field encapsulated within this dialog. */
    private final JTextField txtFullName = new JTextField(20);

    /** Private: input field encapsulated within this dialog. */
    private final JTextField txtEmail = new JTextField(20);

    /** Private: input field encapsulated within this dialog. */
    private final JTextField txtPhone = new JTextField(20);

    /** Private: password field encapsulated within this dialog. */
    private final JPasswordField pwdPassword = new JPasswordField(20);

    /** Private: password confirmation field encapsulated within this dialog. */
    private final JPasswordField pwdConfirm = new JPasswordField(20);

    /** Private: informational label encapsulated within this dialog. */
    private final JLabel lblInfo = new JLabel("New accounts get the Sales Staff role.");

    /** Private: error message label encapsulated within this dialog. */
    private final JLabel lblError = new JLabel(" ");

    /** Private: button encapsulated within this dialog to trigger account creation. */
    private final JButton btnCreate = new JButton("Create account");

    /** Private: button encapsulated within this dialog to cancel and close. */
    private final JButton btnCancel = new JButton("Cancel");

    /** Private: created staff state, accessible externally via getCreatedStaff(). */
    private Staff createdStaff;

    /**
     * Constructs a modal dialog for staff sign-up.
     * Public: called by LoginDialog.
     *
     * @param owner parent Dialog; dialog is centered over this
     * @param auth authentication service used to create account
     */
    public SignUpDialog(Dialog owner, AuthService auth) {
        super(owner, "Create Staff Account", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        if (auth == null) {
            throw new IllegalArgumentException("auth must not be null");
        }
        this.auth = auth;

        txtUsername.setToolTipText("3-50 letters, digits, dots or underscores");
        txtPhone.setToolTipText("10 digits starting with 0");
        pwdPassword.setToolTipText("At least 8 characters with a letter and a digit");

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Username
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        panel.add(new JLabel("Username:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(txtUsername, c);

        // Row 1: Full name
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.gridwidth = 1;
        panel.add(new JLabel("Full name:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(txtFullName, c);

        // Row 2: Email
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        c.gridwidth = 1;
        panel.add(new JLabel("Email:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(txtEmail, c);

        // Row 3: Phone
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0;
        c.gridwidth = 1;
        panel.add(new JLabel("Phone:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(txtPhone, c);

        // Row 4: Password
        c.gridx = 0;
        c.gridy = 4;
        c.weightx = 0;
        c.gridwidth = 1;
        panel.add(new JLabel("Password:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(pwdPassword, c);

        // Row 5: Confirm password
        c.gridx = 0;
        c.gridy = 5;
        c.weightx = 0;
        c.gridwidth = 1;
        panel.add(new JLabel("Confirm password:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        panel.add(pwdConfirm, c);

        // Row 6: Info label
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 2;
        c.weightx = 1.0;
        lblInfo.setForeground(Color.GRAY);
        panel.add(lblInfo, c);

        // Row 7: Error label
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 2;
        c.weightx = 1.0;
        lblError.setForeground(new Color(180, 0, 0));
        panel.add(lblError, c);

        // Row 8: Action buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonsPanel.add(btnCreate);
        buttonsPanel.add(Box.createHorizontalStrut(8));
        buttonsPanel.add(btnCancel);

        c.gridx = 0;
        c.gridy = 8;
        c.gridwidth = 2;
        c.weightx = 1.0;
        panel.add(buttonsPanel, c);

        btnCreate.addActionListener(e -> onCreate());
        btnCancel.addActionListener(e -> dispose());

        getRootPane().setDefaultButton(btnCreate);
        setContentPane(panel);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /**
     * Returns the created staff account if sign-up was successful.
     * Public: called by caller dialog to inspect registration result.
     *
     * @return optional containing the created Staff, or empty if cancelled/failed
     */
    public Optional<Staff> getCreatedStaff() {
        return Optional.ofNullable(createdStaff);
    }

    /**
     * Handles account creation validation and service call.
     * Private: triggered only by the create account button.
     */
    private void onCreate() {
        char[] password = pwdPassword.getPassword();
        char[] confirm = pwdConfirm.getPassword();
        try {
            if (!Arrays.equals(password, confirm)) {
                showError("Passwords do not match.");
                return;
            }
            createdStaff = auth.signUp(txtUsername.getText(), password, txtFullName.getText(),
                    txtEmail.getText(), txtPhone.getText());
            pwdPassword.setText("");
            pwdConfirm.setText("");
            dispose();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (RuntimeException ex) {
            showError("Cannot reach the database: " + ex.getMessage());
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
        }
    }

    /**
     * Updates the error label text and repacks the dialog to adjust layout.
     * Private: internal UI error display helper.
     *
     * @param message error text to show
     */
    private void showError(String message) {
        lblError.setText(message);
        pack();
    }
}
