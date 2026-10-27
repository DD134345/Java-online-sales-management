package vgu.sales.ui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 * JTable with alternating row colours (zebra striping) and single-row selection.
 * <p>
 * Open to subclassing by concrete screens, so public. Not final.
 * Use in all UI table grids for consistency.
 */
public class StripedTable extends JTable {
    /**
     * Light row background colour for even-numbered (striped) rows.
     * Private: only prepareRenderer of this class uses it.
     */
    private static final Color STRIPE = new Color(242, 245, 250);

    /**
     * Constructs a striped, sortable, single-selection table from the given TableModel.
     * <p>
     * Public so screens can instantiate.
     * @param model table model
     */
    public StripedTable(TableModel model) {
        super(model);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Single-row selection only (requirement)
        setFillsViewportHeight(true); // Fill height if viewport larger than content
        setAutoCreateRowSorter(true); // Enable sorting by column
        getTableHeader().setReorderingAllowed(false); // Fix column order for clarity
        setRowHeight(24); // Consistent row height
    }

    /**
     * Overrides base rendering to apply alternating stripes and selection color.
     * <p>
     * Public: JTable declares it public, and an override cannot narrow the access.
     * @param renderer the TableCellRenderer
     * @param row row index
     * @param column column index
     * @return cell component, with background set for stripe or selection
     */
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (!isRowSelected(row)) {
            c.setBackground(row % 2 == 0 ? getBackground() : STRIPE);
        } else {
            c.setBackground(getSelectionBackground());
        }
        return c;
    }
}
