package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;
import com.l7tech.proxy.util.ClientLogger;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.io.IOException;

/**
 * Provide a ListModel view of the current SSG list.
 * User: mike
 * Date: Jun 3, 2003
 * Time: 2:48:51 PM
 */
public class SsgTableModel extends AbstractTableModel {
    private static final ClientLogger log = ClientLogger.getInstance(SsgTableModel.class);
    private SsgManager ssgManager;

    public SsgTableModel(SsgManager ssgManager) {
        super();
        if (ssgManager == null)
            throw new IllegalArgumentException("No SsgManager provided");
        this.ssgManager = ssgManager;
    }

    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        synchronized (ssgManager) {
            if (rowIndex < 0 || rowIndex >= ssgManager.getSsgList().size())
                return null;
            Ssg ssg = (Ssg)  ssgManager.getSsgList().get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return ssg;
                case 1:
                    return ssg.getLocalEndpoint();
                case 2:
                    return ssg.getUsername() == null ? "" : ssg.getUsername();
                default:
                    return null;
            }
        }
    }

    public int getRowCount() {
        return ssgManager.getSsgList().size();
    }

    public int getColumnCount() {
        return 3;
    }

    /** Get the SSG at the specified row, or null. */
    public Ssg getSsgAtRow(final int rowNumber) {
        synchronized (ssgManager) {
            if (rowNumber < 0 || rowNumber >= ssgManager.getSsgList().size())
                return null;
            return (Ssg)  ssgManager.getSsgList().get(rowNumber);
        }
    }

    public void addSsg(final Ssg ssg) {
        if (ssgManager.add(ssg)) {
            saveSsgList();
            this.fireTableDataChanged();
        }
        ssgManager.onSsgUpdated(ssg);
    }

    public void removeSsg(final Ssg ssg) {
        try {
            ssgManager.remove(ssg);
            saveSsgList();
            this.fireTableDataChanged();
        } catch (SsgNotFoundException e) {
            // who cares
        }
        ssgManager.onSsgUpdated(ssg);
    }

    public void setDefaultSsg(Ssg ssg) {
        try {
            ssgManager.setDefaultSsg(ssg);
        } catch (SsgNotFoundException e) {
            log.error(e); // shouldn't ever happen
        }
        ssgManager.onSsgUpdated(ssg);
    }

    public void editedSsg() {
        saveSsgList();
        ssgManager.onSsgUpdated(null);
        this.fireTableDataChanged();
    }

    public Ssg createSsg() {
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
}

