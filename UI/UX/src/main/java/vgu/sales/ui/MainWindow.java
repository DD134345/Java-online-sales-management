package vgu.sales.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Main application window for Online Sales Management.
 * Extends JFrame as the top-level window. Public: App and the test-only UiPreview create it.
 */
public class MainWindow extends JFrame {
    /** Admin role name, for UI decisions. */
    private static final String ROLE_ADMIN = "ADMIN";
    /** Session user label (top left). */
    private final JLabel lblSession = new JLabel("Not signed in");
    /** Status bar label (bottom). */
    private final JLabel lblStatus = new JLabel("Ready");
    /** New Order button (top right). */
    private final JButton btnNewOrder = new JButton("New Order");
    /** Main tabbed pane with placeholder panels. */
    private final JTabbedPane tabs = new JTabbedPane();
    /** Logout menu item (File menu). */
    private final JMenuItem mnuLogout = new JMenuItem("Logout");
    /** Exit menu item (File menu). */
    private final JMenuItem mnuExit = new JMenuItem("Exit");
    /** Product categories menu item (Management). */
    private final JMenuItem mnuCategories = new JMenuItem("Categories...");
    /** Staff management menu item (Management). */
    private final JMenuItem mnuStaff = new JMenuItem("Staff Accounts...");
    /** Action to run on logout, never null. */
    private Runnable logoutAction = () -> { };

    /**
     * Constructs main window with placeholders and sets initial state.
     * Public for UI bootstrapping.
     */
    public MainWindow() {
        super("Online Sales Management");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setJMenuBar(buildMenuBar());
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(lblStatus, BorderLayout.SOUTH);

        // Add tabs in required order with placeholders
        tabs.addTab("Products", placeholder("Product catalog arrives in slice S3"));
        tabs.addTab("Customers", placeholder("Customer management arrives in slice S4"));
        tabs.addTab("Orders", placeholder("Order history arrives in slice S6"));
        tabs.addTab("Statistics", placeholder("Sales statistics arrive in slice S7"));

        setSize(1100, 700);
        setLocationRelativeTo(null);
        showSession(null, null);
    }

    /**
     * Shows session user in header and enables/disables admin options.
     * Public so caller can update session info after login/logout.
     * @param displayName user's display name, or null if signed out
     * @param role user's role, e.g., "ADMIN", "SALES_STAFF", or null
     */
    public void showSession(String displayName, String role) {
        if (displayName == null) {
            lblSession.setText("Not signed in");
        } else {
            lblSession.setText("Signed in: " + displayName + " (" + role + ")");
        }
        boolean admin = ROLE_ADMIN.equals(role);
        mnuCategories.setEnabled(admin);
        mnuStaff.setEnabled(admin);
    }

    /**
     * Sets the status bar message. Public for easy updates.
     * @param message status to show, not null
     */
    public void setStatus(String message) {
        lblStatus.setText(message);
    }

    /**
     * Sets the logout action to run when Logout is clicked.
     * Public so caller can bind authentication logic. Null not allowed.
     * @param action Runnable to run on logout (never null)
     */
    public void setLogoutAction(Runnable action) {
        if (action == null) throw new IllegalArgumentException("logout action must not be null");
        this.logoutAction = action;
    }

    /**
     * Builds the menu bar with File and Management menus. Private: only called internally.
     * @return JMenuBar with correct structure and listeners
     */
    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu mgmtMenu = new JMenu("Management");

        fileMenu.add(mnuLogout);
        fileMenu.addSeparator();
        fileMenu.add(mnuExit);

        mgmtMenu.add(mnuCategories);
        mgmtMenu.add(mnuStaff);

        // Listeners
        mnuLogout.addActionListener(e -> logoutAction.run());
        mnuExit.addActionListener(e -> {
            dispose();
            System.exit(0);
        });
        mnuCategories.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Coming in slice S3");
        });
        mnuStaff.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Coming in slice S2");
        });

        bar.add(fileMenu);
        bar.add(mgmtMenu);
        return bar;
    }

    /**
     * Builds the app header panel (session info left, button right).
     * Private for internal layout only.
     * @return JPanel header
     */
    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        // Bold session label
        lblSession.setFont(lblSession.getFont().deriveFont(Font.BOLD));
        panel.add(lblSession, BorderLayout.WEST);
        // Disabled New Order button with tooltip
        btnNewOrder.setEnabled(false);
        btnNewOrder.setToolTipText("Checkout arrives in slice S5");
        panel.add(btnNewOrder, BorderLayout.EAST);
        return panel;
    }

    /**
     * Creates a plain placeholder panel with centered grey text, for future screens.
     * Static: utility for placeholder use.
     * @param text what to display
     * @return JPanel with centered label
     */
    private static JPanel placeholder(String text) {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel label = new JLabel(text);
        label.setForeground(Color.GRAY);
        panel.add(label, new GridBagConstraints(
            0, 0, 1, 1, 1.0, 1.0,
            GridBagConstraints.CENTER, GridBagConstraints.NONE,
            new Insets(10, 10, 10, 10), 0, 0));
        return panel;
    }
}
