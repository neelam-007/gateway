package com.l7tech.console.panels.solutionkit;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * This panel contains a table that contains solution kits.
 */
public class SolutionKitTablePanel extends JPanel {
    private static enum SolutionKitDisplayingType {PARENT_COLLAPSED, PARENT_EXPENDED, CHILD, NEITHER_PARENT_NOR_CHILD}

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

        solutionKitsDisplayed = (List<SolutionKitHeader>) solutionKitAdmin.findAllExcludingChildren(); // Initially we do not display all children in the table.
        Collections.sort(solutionKitsDisplayed);
        solutionKitsModel.setRows(solutionKitsDisplayed);

        parentSolutionKits = (List<SolutionKitHeader>) solutionKitAdmin.findParentSolutionKits();
    }

    /**
     * Check if a solution kit in the table is selected.
     *
     * @return true if a solution kit selected. false otherwise.
     */
    public boolean isSolutionKitSelected() {
        int rowIndex = solutionKitsTable.getSelectedRow();
        return (rowIndex != -1);
    }

    /**
     * Get the selected solution kit header in the table.
     *
     * @return the selected solution kit header. Null if none is selected.
     */
    public SolutionKitHeader getSelectedSolutionKit() {
        int rowIndex = solutionKitsTable.getSelectedRow();
        if (rowIndex == -1 || rowIndex >= solutionKitsTable.getRowCount()) {
            return null;
        }

        return solutionKitsModel.getRowObject(rowIndex);
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
                            return "   |-";
                        case NEITHER_PARENT_NOR_CHILD:
                            return "";
                        default:
                            throw new IllegalArgumentException("Invalid solution kit displaying type");
                    }
                }
            }),
            column("Name", 50, 400, 5000, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return solutionKitHeader.getName();
                }
            }),
            column("Version", 50, 60, 500, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return solutionKitHeader.getSolutionKitVersion();
                }
            }),
            column("Instance Modifier", 50, 240, 5000, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return solutionKitHeader.getInstanceModifier();
                }
            }),
            column("Description", 50, 500, 5000, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return solutionKitHeader.getDescription();
                }
            }),
            column("Last Updated", 50, 400, 1000, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return new Date(solutionKitHeader.getLastUpdateTime()).toString();
                }
            })
        );

        solutionKitsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        solutionKitsTable.getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (solutionKitsTable.getSelectedColumn() == 0) {
                    final SolutionKitHeader solutionKitHeader = getSelectedSolutionKit();
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
     * Insert all children of the parent solution kit into solutionKitsDisplayed
     * @param parent a solution kit has at least one child.
     */
    private void insertChildren(SolutionKitHeader parent) {
        try {
            final List<SolutionKitHeader> allChildren = (List<SolutionKitHeader>) solutionKitAdmin.findAllChildrenByParentGoid(parent.getGoid());
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
    private SolutionKitDisplayingType findDisplayingType(@NotNull final SolutionKitHeader solutionKitHeader) {
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