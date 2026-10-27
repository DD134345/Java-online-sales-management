package vgu.sales.ui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * Abstract base class for read-only table models. All screen tables should extend this.
 * <p>
 * Purpose: ensures all grid data is immutable from the UI (per screens.md),
 * forces column and value mapping definitions in subclasses, and provides
 * a consistent way to update the table data.
 * <p>
 * Declared abstract so each entity screen must specify column names and cell mapping.
 */
public abstract class ReadOnlyTableModel<T> extends AbstractTableModel {
    /**
     * Holds the current list of rows displayed in the table.
     * <p>
     * Private: subclasses can only modify via setRows(), ensuring updates trigger
     * table change events and keeping data encapsulated.
     */
    private final List<T> rows = new ArrayList<>();

    /**
     * @return the list of column headers for this table model.
     * <p>
     * Protected and abstract: must be implemented by subclass to define headers,
     * and intended only for use within the inheritance hierarchy.
     */
    protected abstract String[] columnNames();

    /**
     * Maps a row object to its value for the given column.
     *
     * @param row    The entity instance
     * @param column Which column
     * @return The value to display in the cell
     * <p>
     * Protected and abstract: must be implemented by subclass, and intended for use only by this class and its subclasses.
     */
    protected abstract Object valueOf(T row, int column);

    /**
     * Replaces all table data.
     * @param newRows The new list of rows to display. If null, shows empty.
     * <p>
     * Public: called by screens/panels on reload. Not final as subclasses may extend behavior.
     */
    public void setRows(List<T> newRows) {
        rows.clear();
        if (newRows != null) rows.addAll(newRows);
        fireTableDataChanged();
    }

    /**
     * Returns the row entity at the specified index.
     * @param rowIndex Row index
     * @return Corresponding row object
     * <p>
     * Public: screens may need to obtain the model object for detailed view or actions.
     */
    public T getRowAt(int rowIndex) {
        return rows.get(rowIndex);
    }

    /**
     * @return How many rows in the table now.
     * <p>
     * Public override because AbstractTableModel requires this. No reason for narrower visibility.
     */
    @Override
    public int getRowCount() {
        return rows.size();
    }

    /**
     * @return How many columns (as defined by the subclass)
     * <p>
     * Public override; required by AbstractTableModel.
     */
    @Override
    public int getColumnCount() {
        return columnNames().length;
    }

    /**
     * Returns the header for the column.
     *
     * @param column Column index
     * @return Header string
     * <p>
     * Public override; required by swing JTable.
     */
    @Override
    public String getColumnName(int column) {
        return columnNames()[column];
    }

    /**
     * Returns the data value at the specified cell.
     *
     * @param rowIndex    Row index
     * @param columnIndex Column index
     * @return Cell value to display
     * <p>
     * Public override: required by JTable.
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return valueOf(rows.get(rowIndex), columnIndex);
    }

    /**
     * Cells are always read-only in this model.
     *
     * @return Always false
     * <p>
     * Public override (required by JTable); ensures Swing never tries to edit a cell.
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
