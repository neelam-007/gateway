package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.datamodel.SsgNotFoundException;

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

    public int getSize() {
        return Managers.getSsgManager().getSsgList().size();
    }

    public Object getElementAt(final int index) {
        final SsgManager ssgManager = Managers.getSsgManager();
        synchronized (ssgManager) {
            if (index < 0 || index >= ssgManager.getSsgList().size())
                return null;
            return Managers.getSsgManager().getSsgList().get(index);
        }
    }

    public void addSsg(final Ssg ssg) {
        if (Managers.getSsgManager().add(ssg)) {
            saveSsgList();
            fireIntervalAdded(this, getSize(), getSize());
        }
    }

    public void removeSsg(final Ssg ssg) {
        try {
            Managers.getSsgManager().remove(ssg);
            saveSsgList();
            fireContentsChanged(this, 0, getSize() + 1);
        } catch (SsgNotFoundException e) {
            // who cares
        }
    }

    public void setDefaultSsg(Ssg ssg) {
        try {
            Managers.getSsgManager().setDefaultSsg(ssg);
        } catch (SsgNotFoundException e) {
            log.error(e); // shouldn't ever happen
        }
    }

    public void editedSsg() {
        saveSsgList();
        fireContentsChanged(this, 0, getSize());
    }

    public Ssg createSsg() {
        return Managers.getSsgManager().createSsg();
    }

   /**
    * Save the SSG list to disk, overwriting the old file.
    */
    private void saveSsgList() {
        try {
            Managers.getSsgManager().save();
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

