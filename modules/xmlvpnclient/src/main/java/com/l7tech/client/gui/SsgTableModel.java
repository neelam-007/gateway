package com.l7tech.client.gui;

import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provide a ListModel view of the current SSG list.
 * User: mike
 * Date: Jun 3, 2003
 * Time: 2:48:51 PM
 */
class SsgTableModel extends AbstractTableModel implements SsgListener {
    private static final Logger log = Logger.getLogger(SsgTableModel.class.getName());
    private final String SSG_TYPE_FEDERATED = "Federated";
    private final String SSG_TYPE_TRUSTED = "Trusted";
    private final String SSG_TYPE_GENERIC = "Generic service";

    private SsgManager ssgManager;
    private SortedSet model = null;
    private Object[] modelArray = null;
    private Comparator comparator = null;
    private int sortColumn = 1;
    private boolean sortReverse = false;

    SsgTableModel(SsgManager ssgManager) {
        super();
        if (ssgManager == null)
            throw new IllegalArgumentException("No SsgManager provided");
        this.ssgManager = ssgManager;
    }

    public boolean isCellEditable(int row, int column) {
        return false;
    }

    private void setComparator(Comparator comparator) {
        if (this.comparator != comparator) {
            this.comparator = comparator;
            updateModel();
        }
    }

    public SsgFinder getSsgFinder() {
        return ssgManager;
    }

    /** Comparator for sorting the SSG list for display. */
    private static abstract class SsgComparator implements Comparator {
        boolean reverse;
        private SsgComparator(boolean reverse) { this.reverse = reverse; }

        // Returns compare() result if at least one of o1 or o2 is null; returns 2 if both are non-null.
        protected int compareNull(Object o1, Object o2) {
            // safety / paranoia: handle one or both is null (null comparing as less than non-null)
            if (o1 == null && o2 == null)
                return 0;
            if (o1 == null)
                return -1;
            if (o2 == null)
                return 1;
            return 2;
        }

        public int compare(Object o1, Object o2) {
            if (reverse)
                return doCompare(o2, o1);
            else
                return doCompare(o1, o2);
        }

        public int doCompare(Object o1, Object o2) {
            int nullCheck = compareNull(o1, o2);
            if (nullCheck != 2)
                return nullCheck;

            // safety / paranoia: handle one or both is not an Ssg (Ssg comparing as greater than non-Ssg)
            if (!(o1 instanceof Ssg) || !(o2 instanceof Ssg))
                return 0;
            else if (!(o1 instanceof Ssg))
                return -1;
            else if (!(o2 instanceof Ssg))
                return 1;

            Ssg ssg1 = (Ssg) o1;
            Ssg ssg2 = (Ssg) o2;
            int result = compare(ssg1, ssg2);
            return result == 0 ? ssg1.compareTo(ssg2) : result; // always use ssg ID as backup comparator
        }

        protected int compareStringsThatMightBeNull(String s1, String s2) {
            int nullCheck = compareNull(s1, s2);
            if (nullCheck != 2)
                return nullCheck;
            return s1.compareToIgnoreCase(s2);
        }

        abstract public int compare(Ssg ssg1, Ssg ssg2);
    }

    int getSortColumn() {
        return sortColumn;
    }

    boolean getSortingReverse() {
        return sortReverse;
    }

    /** Sort the table by the specified column. */
    void setSortOrder(int sortColumn, final boolean reverse) {
        this.sortColumn = sortColumn;
        this.sortReverse = reverse;
        if (sortColumn == 3) {
            setComparator(new SsgComparator(reverse) {
                public int compare(Ssg ssg1, Ssg ssg2) {
                    return compareStringsThatMightBeNull(ssg1.getUsername(), ssg2.getUsername());
                }
            });
        } else if (sortColumn == 2) {
            setComparator(new SsgComparator(reverse) {
                public int compare(Ssg ssg1, Ssg ssg2) {
                    String ssgaType = SSG_TYPE_TRUSTED;
                    String ssgbType = SSG_TYPE_TRUSTED;
                    if (ssg1.isGeneric()) {
                        ssgaType = SSG_TYPE_GENERIC;
                    } else if (ssg1.isFederatedGateway()) {
                        ssgaType = SSG_TYPE_FEDERATED;
                    }
                    if (ssg2.isGeneric()) {
                        ssgbType = SSG_TYPE_GENERIC;
                    } else if (ssg2.isFederatedGateway()) {
                        ssgbType = SSG_TYPE_FEDERATED;
                    }
                    return compareStringsThatMightBeNull(ssgaType, ssgbType);
                }
            });
        } else if (sortColumn == 0) {
            setComparator(new SsgComparator(reverse) {
                public int compare(Ssg ssg1, Ssg ssg2) {
                    return compareStringsThatMightBeNull(ssg1.getSsgAddress(), ssg2.getSsgAddress());
                }
            });
        } else { // SORT_BY_ID
            this.sortColumn = 1;
            setComparator(new SsgComparator(reverse) {
                public int compare(Ssg ssg1, Ssg ssg2) {
                    return ssg1.compareTo(ssg2);
                }
            });
        }
    }

    private void updateModel() {
        int cursize = getRowCount();
        model = null;
        if (cursize == getRowCount())
            fireTableRowsUpdated(0, getRowCount());
        else
            fireTableDataChanged();
    }

    private SortedSet getModel() {
        if (model == null) {
            if (comparator == null)
                model = new TreeSet();
            else
                model = new TreeSet(comparator);
            model.addAll(ssgManager.getSsgList());
            modelArray = null;
        }
        return model;
    }

    private Object[] getModelArray() {
        if (modelArray == null) {
            modelArray = getModel().toArray();
        }
        return modelArray;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        synchronized (ssgManager) {
            if (rowIndex < 0 || rowIndex >= getModel().size())
                return null;
            Ssg ssg = (Ssg) getModelArray()[rowIndex];
            switch (columnIndex) {
                case 0:
                    return ssg;
                case 1:
                    return ssg.getLocalEndpoint();
                case 2:
                    if (ssg.isGeneric()) {
                        return SSG_TYPE_GENERIC;
                    } else if(ssg.isFederatedGateway()) {
                        return SSG_TYPE_FEDERATED;
                    } else {
                        return SSG_TYPE_TRUSTED;
                    }
                case 3:
                    ssg.addSsgListener(this); // make sure we're subscribed for updates
                    return ssg.getUsername() == null ? "" : ssg.getUsername();
                default:
                    return null;
            }
        }
    }

    public int getRowCount() {
        return getModel().size();
    }

    public int getColumnCount() {
        return 4;
    }

    /** Get the SSG at the specified row, or null. */
    Ssg getSsgAtRow(final int rowNumber) {
        return (Ssg) getValueAt(rowNumber, 0);
    }

    void addSsg(final Ssg ssg) {
        if (ssgManager.add(ssg)) {
            saveSsgList();
            updateModel();
        }
        ssgManager.onSsgUpdated(ssg);
    }

    /** Remove the specified Ssg from the SsgManager (and hence from ssgs.xml) and also from the table model. */
    void removeSsg(final Ssg ssg) {
        try {
            ssgManager.remove(ssg);
            saveSsgList();
            updateModel();
        } catch (SsgNotFoundException e) {
            // who cares
        }
        ssgManager.onSsgUpdated(ssg);
    }

    void setDefaultSsg(Ssg ssg) {
        Ssg oldDefault = null;
        try {
            oldDefault = ssgManager.getDefaultSsg();
        } catch (SsgNotFoundException e) {
            // ok, that's fine
        }
        try {
            ssgManager.setDefaultSsg(ssg);
        } catch (SsgNotFoundException e) {
            log.log(Level.SEVERE, e.getMessage(), e); // shouldn't ever happen
        }
        ssgManager.onSsgUpdated(ssg);
        if (oldDefault != null)
            fireSsgUpdated(oldDefault);
        fireSsgUpdated(ssg);
    }

    /**
     * Return the row containing the given ssg, or -1 if it isn't in the table.
     * @param ssg
     */
    int getRow(Ssg ssg) {
        if (ssg == null)
            return -1;

        for (int i = 0; i < getModelArray().length; i++) {
            Ssg s = (Ssg) getModelArray()[i];
            if (s.equals(ssg))
                return i;
        }

        return -1;
    }

    /**
     * Called by SSG property dialog to notify us that the specified SSG has been edited.
     * TODO: Figure out if this is still needed now that Ssg supports the SsgListener interface. 
     */
    void editedSsg(Ssg ssg) {
        saveSsgList();
        ssgManager.onSsgUpdated(ssg);
        fireSsgUpdated(ssg);
    }

    Ssg createSsg() {
        return ssgManager.createSsg();
    }

   /**
    * Save the SSG list to disk, overwriting the old file.
    */
    private void saveSsgList() {
        try {
            ssgManager.save();
        } catch (IOException e1) {
            JOptionPane.showMessageDialog(
                Gui.getInstance().getFrame(),
                "The system reported an error while saving the Gateway list.\n" +
                "The reported error was: " + e1,
                "Unable to save changes",
                JOptionPane.OK_OPTION,
                null);
        }
    }

    /**
     * This event is fired when a policy is attached to an Ssg with a PolicyAttachmentKey, either new
     * or updated.
     *
     * @param evt
     */
    public void policyAttached(SsgEvent evt) {
        // no action required
    }

    /**
     * This event is fired when field data in an Ssg such as name, local endpoing etc. are changed.
     */
    public void dataChanged(SsgEvent evt) {
        if (evt.getSource() instanceof Ssg)
            fireSsgUpdated((Ssg) evt.getSource());
    }

    public void sslReset(SsgEvent evt) {
        // Don't care
    }

    /**
     * Fire an event indicating that row containing the specified SSG should be updated.
     * Takes no action if the given SSG is not in the table.
     * @param ssg
     */
    public void fireSsgUpdated(Ssg ssg) {
        int row = getRow(ssg);
        if (row >= 0)
            fireTableRowsUpdated(row, row);
    }
}

