package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.datamodel.SsgNotFoundException;

import javax.swing.*;
import java.io.IOException;

/**
 * Provide a ListModel view of the current SSG list.
 * User: mike
 * Date: Jun 3, 2003
 * Time: 2:48:51 PM
 */
public class SsgListModel extends AbstractListModel {

    public int getSize() {
        return Managers.getSsgManager().getSsgList().size();
    }

    public Object getElementAt(int index) {
        SsgManager ssgManager = Managers.getSsgManager();
        synchronized (ssgManager) {
            if (index < 0 || index >= ssgManager.getSsgList().size())
                return null;
            return Managers.getSsgManager().getSsgList().get(index);
        }
    }

    public void addSsg(Ssg ssg) {
        if (Managers.getSsgManager().add(ssg)) {
            saveSsgList();
            fireIntervalAdded(this, getSize(), getSize());
        }
    }

    public void removeSsg(Ssg ssg) {
        try {
            Managers.getSsgManager().remove(ssg);
            saveSsgList();
            fireContentsChanged(this, 0, getSize() + 1);
        } catch (SsgNotFoundException e) {
            // who cares
        }
    }

    public void editedSsg(Ssg ssg) {
        saveSsgList();
        fireContentsChanged(this, 0, getSize());
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

