package vgu.sales.ui;

import java.math.BigDecimal;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Preview launcher for inspecting UI screens and components without a database.
 * Public: run it from the IDE or with exec:java on the test classpath.
 */
public final class UiPreview {

    private UiPreview() {
    }

    /**
     * Entry point to launch the preview windows on the Event Dispatch Thread.
     *
     * @param args command-line arguments (ignored)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(UiPreview::show);
    }

    /**
     * Opens MainWindow with a fake admin session and a StripedTable demo frame.
     * Private: only main() schedules it on the Event Dispatch Thread.
     */
    private static void show() {
        MainWindow window = new MainWindow();
        window.showSession("Preview Admin", "ADMIN");
        window.setStatus("UiPreview: fake data, no database");
        window.setVisible(true);

        JFrame demo = new JFrame("StripedTable demo");
        demo.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        ReadOnlyTableModel<String[]> model = new ReadOnlyTableModel<String[]>() {
            @Override
            protected String[] columnNames() {
                return new String[] {"Code", "Product", "Price"};
            }

            @Override
            protected Object valueOf(String[] row, int column) {
                return row[column];
            }
        };

        model.setRows(List.of(
                new String[] {"P001", "USB-C Cable", UiFormat.money(new BigDecimal("150000"))},
                new String[] {"P002", "Desk Lamp", UiFormat.money(new BigDecimal("450000"))},
                new String[] {"P003", "Laptop Stand", UiFormat.money(new BigDecimal("1290000"))}
        ));

        demo.add(new JScrollPane(new StripedTable(model)));
        demo.setSize(500, 220);
        demo.setLocation(60, 60);
        demo.setVisible(true);
    }
}
