package vgu.sales.ui;

import java.util.Optional;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import vgu.sales.controller.AuthService;
import vgu.sales.controller.ControllerFactory;
import vgu.sales.controller.model.Staff;

/**
 * App entry point for Online Sales Management system.
 * Launches MainWindow, handles login/logout, sets up UI look and feel.
 */
public final class App {
    private App() { }

    /**
     * Application main entry (for JVM).
     * Launches GUI on EDT and starts login sequence.
     * @param args CLI args, ignored
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::start);
    }

    /**
     * Sets system look and feel, creates window, binds logout, starts login dialog.
     * Services come from ControllerFactory; UI never creates DAOs.
     */
    private static void start() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Using the default look and feel: " + e.getMessage());
        }
        AuthService auth = ControllerFactory.authService();
        MainWindow window = new MainWindow();
        window.setLogoutAction(() -> {
            auth.logout();
            showLogin(window, auth);
        });
        window.setVisible(true);
        showLogin(window, auth);
    }

    /**
     * Shows login dialog and handles authentication session.
     * On cancel, closes the application. On success, sets session user details.
     * @param window MainWindow instance for session context
     * @param auth AuthService instance for authentication operations
     */
    private static void showLogin(MainWindow window, AuthService auth) {
        window.showSession(null, null);
        window.setStatus("Please sign in.");
        LoginDialog dialog = new LoginDialog(window, auth);
        dialog.setVisible(true); // modal, blocks
        Optional<Staff> staff = dialog.getStaff();
        dialog.dispose();
        if (staff.isEmpty()) {
            window.dispose();
            System.exit(0);
            return;
        }
        Staff signedIn = staff.get();
        window.showSession(signedIn.getFullName(), signedIn.getRole().name());
        window.setStatus("Signed in as " + signedIn.getUsername() + " (" + signedIn.describeRole() + ")");
    }
}
