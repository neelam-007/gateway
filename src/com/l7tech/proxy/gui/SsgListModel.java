package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;

import javax.swing.*;
import java.io.IOException;

import org.apache.log4j.Category;

/**
 * Provide a ListModel view of the current SSG list.
 * User: mike
 * Date: Jun 3, 2003
 * Time: 2:48:51 PM
 */
public class SsgListModel extends AbstractListModel {
    private static final Category log = Category.getInstance(SsgListModel.class);
    private SsgManager ssgManager;

    public SsgListModel(SsgManager ssgManager) {
        if (ssgManager == null)
            throw new IllegalArgumentException("No SsgManager provided");
        this.ssgManager = ssgManager;
    }

    public int getSize() {
        return ssgManager.getSsgList().size();
    }

    public Object getElementAt(final int index) {
        synchronized (ssgManager) {
            if (index < 0 || index >= ssgManager.getSsgList().size())
                return null;
            return ssgManager.getSsgList().get(index);
        }
    }

    public void addSsg(final Ssg ssg) {
        if (ssgManager.add(ssg)) {
            saveSsgList();
            fireIntervalAdded(this, getSize(), getSize());
        }
        ssgManager.onSsgUpdated(ssg);
    }

    public void removeSsg(final Ssg ssg) {
        try {
            ssgManager.remove(ssg);
            saveSsgList();
            fireContentsChanged(this, 0, getSize() + 1);
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
        fireContentsChanged(this, 0, getSize());
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
                "The system reported an error while saving the SSG list.\n" +
                "The reported error was: " + e1,
                "Unable to save changes",
                JOptionPane.OK_OPTION,
                null);
        }
    }
}

