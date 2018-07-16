package com.l7tech.console.panels.solutionkit;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.DateUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * This panel contains a table that contains solution kits.
 */
public class SolutionKitTablePanel extends JPanel {
    static enum SolutionKitDisplayingType {PARENT_COLLAPSED, PARENT_EXPENDED, CHILD, NEITHER_PARENT_NOR_CHILD}

    private static final Logger logger = Logger.getLogger(SolutionKitTablePanel.class.getName());
    private final SolutionKitAdmin solutionKitAdmin;

    private JPanel mainPanel;
    private JTable solutionKitsTable;

    private SimpleTableModel<SolutionKitHeader> solutionKitsModel;
    private List<SolutionKitHeader> solutionKitsDisplayed;
    private List<SolutionKitHeader> parentSolutionKits;

    /**
     * Create panel.
     */
    public SolutionKitTablePanel() {
        super();
        solutionKitAdmin = Registry.getDefault().getSolutionKitAdmin();
        initialize();
    }

    /**
     * Refresh the solution kit table by set solution kits to display in the table.
     */
    public void refreshSolutionKitTable() throws FindException {
        // Now the table sort order becomes complicated, since parent and children are probably mixed together.
        // The table sorting relies on the element order of solutionKitsDisplayed.
        // Thus, remove any row sorter from the table model and all elements of solutionKitsDisplayed will be sorted in some manner.

        Collection<SolutionKitHeader> allSolutionKits = solutionKitAdmin.findHeaders();

        // determine possible parents and actual parent goids
        List<SolutionKitHeader> possibleParents = new ArrayList<>(allSolutionKits.size());
        Set<Goid> parentGoids = new HashSet<>(allSolutionKits.size());
        for (SolutionKitHeader solutionKit : allSolutionKits) {
            if (solutionKit.getParentGoid() == null) {
                possibleParents.add(solutionKit);
            } else {
                parentGoids.add(solutionKit.getParentGoid());
            }
        }

        solutionKitsDisplayed = possibleParents;  // Initially we do not display children in the table.
        Collections.sort(solutionKitsDisplayed);
        solutionKitsModel.setRows(solutionKitsDisplayed);

        // build list of actual parents - skip any that don't have children
        parentSolutionKits = new ArrayList<>(possibleParents.size());
        for (SolutionKitHeader possibleParent : possibleParents) {
            if (parentGoids.contains(possibleParent.getGoid())) {
                parentSolutionKits.add(possibleParent);
            }
        }
    }

    /**
     * Returns the number of selected rows.
     *
     * @return the number of selected rows, 0 if no rows are selected
     */
    public int getSelectedRowCount() {
        return solutionKitsTable.getSelectedRowCount();
    }

    /**
     * Get the selected solution kit header in the table.
     *
     * @return the selected solution kit header. Null if none is selected.
     */
    public SolutionKitHeader getFirstSelectedSolutionKit() {
        int rowIndex = solutionKitsTable.getSelectedRow();
        if (rowIndex == -1 || rowIndex >= solutionKitsTable.getRowCount()) {
            return null;
        }

        return solutionKitsModel.getRowObject(rowIndex);
    }

    public Collection<SolutionKitHeader> getSelectedSolutionKits() {
        final List<SolutionKitHeader> selected = new ArrayList<>();
        final int[] rows = solutionKitsTable.getSelectedRows();
        for (final int row : rows) {
            final int modelRow = solutionKitsTable.convertRowIndexToModel(row);
            final SolutionKitHeader rowObject = solutionKitsModel.getRowObject(modelRow);
            if (rowObject != null) {
                selected.add(rowObject);
            }
        }
        return selected;
    }

    /**
     * Set the button to click when a double-click is performed on the table.
     *
     * @param button the button
     */
    public void setDoubleClickAction(@NotNull JButton button) {
        Utilities.setDoubleClickAction(solutionKitsTable, button);
    }

    /**
     * Add a given list selection listener to the table.
     *
     * @param listener the listener
     */
    public void addListSelectionListener(@NotNull ListSelectionListener listener) {
        solutionKitsTable.getSelectionModel().addListSelectionListener(listener);
    }

    public void addEnterKeyBinding(final AbstractAction enterKeyAction) {
        solutionKitsTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "PRESS_ENTER");
        solutionKitsTable.getActionMap().put("PRESS_ENTER", enterKeyAction);
    }

    private void initialize() {
        solutionKitsModel = TableUtil.configureTable(solutionKitsTable,
            column("", 30, 30, 30, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    final SolutionKitDisplayingType displayingType = findDisplayingType(solutionKitHeader);
                    switch (displayingType) {
                        case PARENT_COLLAPSED:
                            return "   +";
                        case PARENT_EXPENDED:
                            return "   --";
                        case CHILD:
                            return "    |-";
                        case NEITHER_PARENT_NOR_CHILD:
                            return "";
                        default:
                            throw new IllegalArgumentException("Invalid solution kit displaying type");
                    }
                }
            }),
            column("Name", 50, 270, 5000, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    String displayName = solutionKitHeader.getName();

                    // If it is a child, then add an indentation at the front of the display name.
                    if (SolutionKitDisplayingType.CHILD == findDisplayingType(solutionKitHeader)) {
                        displayName = "     " + displayName;
                    }

                    return displayName;
                }
            }),
            column("Version", 50, 30, 500, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return solutionKitHeader.getSolutionKitVersion();
                }
            }),
            column("Instance Modifier", 50, 100, 5000, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return solutionKitHeader.getInstanceModifier();
                }
            }),
            column("Description", 50, 300, 5000, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return solutionKitHeader.getDescription();
                }
            }),
            column("Last Updated", 50, 100, 1000, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return DateUtils.getZuluFormattedString(new Date(solutionKitHeader.getLastUpdateTime()));
                }
            })
        );

        solutionKitsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        solutionKitsTable.getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (solutionKitsTable.getSelectedColumn() == 0) {
                    final SolutionKitHeader solutionKitHeader = getFirstSelectedSolutionKit();
                    if (solutionKitHeader == null) return;

                    final SolutionKitDisplayingType displayingType = findDisplayingType(solutionKitHeader);
                    switch (displayingType) {
                        case PARENT_COLLAPSED:
                            insertChildren(solutionKitHeader);
                            break;
                        case PARENT_EXPENDED:
                            removeChildren(solutionKitHeader);
                            break;
                        case CHILD:
                        case NEITHER_PARENT_NOR_CHILD:
                            // do nothing for both cases: CHILD and NEITHER_PARENT_NOR_CHILD!
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid solution kit displaying type");
                    }
                }
            }
        });

        //Set up tool tips for the table cells.
        final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if(comp instanceof JComponent) {
                    ((JComponent)comp).setToolTipText(value == null? "N/A": String.valueOf(value));
                }
                return comp;
            }
        };
        solutionKitsTable.getColumnModel().getColumn(1).setCellRenderer(renderer);
        solutionKitsTable.getColumnModel().getColumn(2).setCellRenderer(renderer);
        solutionKitsTable.getColumnModel().getColumn(3).setCellRenderer(renderer);
        solutionKitsTable.getColumnModel().getColumn(4).setCellRenderer(renderer);
        solutionKitsTable.getColumnModel().getColumn(5).setCellRenderer(renderer);

        setLayout(new BorderLayout());
        add(mainPanel);
    }

    /**
     * Insert all children of the parent solution kit into solutionKitsDisplayed.
     * That is, expand the parent.
     *
     * @param parent a solution kit has at least one child.
     */
    void insertChildren(SolutionKitHeader parent) {
        try {
            final List<SolutionKitHeader> allChildren = (List<SolutionKitHeader>) solutionKitAdmin.findHeaders(parent.getGoid());
            Collections.sort(allChildren);

            int index = solutionKitsDisplayed.indexOf(parent);
            if (index < 0) return;

            for (SolutionKitHeader kitHeader: allChildren) {
                solutionKitsDisplayed.add(++index, kitHeader);
            }

            solutionKitsModel.setRows(solutionKitsDisplayed);
        } catch (FindException e) {
            logger.warning("Cannot find children solution kits by a given parent GOID, " + parent.getStrId());
        }
    }

    /**
     * Remove all children under the parent solution kit from solutionKitsDisplayed
     * That is, collapse the parent.
     *
     * @param parent a solution kit has at least one child.
     */
    private void removeChildren(SolutionKitHeader parent) {
        int index = solutionKitsDisplayed.indexOf(parent);
        if (index < 0 || index == solutionKitsDisplayed.size() - 1) return;

        final Goid parentGoid = parent.getGoid();
        SolutionKitHeader child = solutionKitsDisplayed.get(++index);

        // Remove child from the list, solutionKitsDisplayed, which will be updated.
        while (child != null && parentGoid.equals(child.getParentGoid())) {
            solutionKitsDisplayed.remove(child);

            if (index == solutionKitsDisplayed.size()) break;
            child = solutionKitsDisplayed.get(index);
        }

        solutionKitsModel.setRows(solutionKitsDisplayed);
    }

    /**
     * Find what displaying type is for a given solution kit.
     *
     * Four different displaying texts depending on four solution kit types:
     * (1) a parent collapsed, (2) a parent expended, (3) a child, and (4) neither a parent nor a child
     *
     * @param solutionKitHeader: a solution kit header, which will be checked for display type.
     * @return one of SolutionKitDisplayingType
     */
    public SolutionKitDisplayingType findDisplayingType(@NotNull final SolutionKitHeader solutionKitHeader) {
        if (parentSolutionKits.contains(solutionKitHeader)) {
            final int index = solutionKitsDisplayed.indexOf(solutionKitHeader);

            // A parent solution kit but collapsed, since the solution kit is at the last position in the table view.
            if (index == solutionKitsDisplayed.size() - 1) {
                return SolutionKitDisplayingType.PARENT_COLLAPSED;
            } else {
                SolutionKitHeader potentialFirstChild = solutionKitsDisplayed.get(index + 1);
                // A parent solution kit expended, since there is a child below it in the table view.
                if (solutionKitHeader.getGoid().equals(potentialFirstChild.getParentGoid())) {
                    return SolutionKitDisplayingType.PARENT_EXPENDED;
                }
                // A parent solution kit but collapsed, since there are no any children below it in the table view.
                else {
                    return SolutionKitDisplayingType.PARENT_COLLAPSED;
                }
            }
        }
        // A child solution kit displayed in the table.
        else if (solutionKitHeader.getParentGoid() != null) {
            return SolutionKitDisplayingType.CHILD;
        }
        // A solution kit is neither a parent nor a child.
        else {
            return SolutionKitDisplayingType.NEITHER_PARENT_NOR_CHILD;
        }
    }
}