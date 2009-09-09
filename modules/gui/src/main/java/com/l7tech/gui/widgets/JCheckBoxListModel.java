package com.l7tech.gui.widgets;

import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;

/**
 * A ListModel that holds a number of JCheckBox elements as its list entries.
 */
public class  JCheckBoxListModel extends AbstractListModel {
    public static final String CLIENT_PROPERTY_ENTRY_CODE = "JCheckBoxListModel.entryCode";
    private final List<JCheckBox> entries;
    private int armedEntry = -1;

    public JCheckBoxListModel(List<JCheckBox> entries) {
        this.entries = new ArrayList<JCheckBox>(entries);
    }

    protected List<JCheckBox> getEntries() {
        //noinspection ReturnOfCollectionOrArrayField
        return entries;
    }

    @Override
    public int getSize() {
        return entries.size();
    }

    @Override
    public Object getElementAt(int index) {
        return getEntryAt(index);
    }

    public JCheckBox getEntryAt(int index) {
        return entries.get(index);
    }

    public void swapEntries(int index1, int index2) {
        JCheckBox value1 = entries.get(index1);
        JCheckBox value2 = entries.get(index2);
        entries.set(index2, value1);
        entries.set(index1, value2);
        fireContentsChanged(this, index1, index2);
    }

    /**
     * Set the "armed" state for the checkbox at the specified index.
     * Any currently-armed checkbox will be disarmed.
     * <p/>
     * The "armed" state is normally shown on mousedown to show that a checkbox is toggling.
     *
     * @param index index of list entry to arm
     */
    public void arm(int index) {
        disarm();
        if (index < 0) return;
        ButtonModel entryModel = getEntryAt(index).getModel();
        entryModel.setArmed(true);
        entryModel.setRollover(true);
        armedEntry = index;
        fireContentsChanged(this, armedEntry, armedEntry);
    }

    /**
     * Clear the "armed" state from any checkbox that was armed by a call to {@link #arm}.
     */
    public void disarm() {
        if (armedEntry >= 0) {
            getEntryAt(armedEntry).getModel().setArmed(false);
            fireContentsChanged(this, armedEntry, armedEntry);
            armedEntry = -1;
        }
    }

    /**
     * Toggle the checkbox at the specified index.
     * @param index the index to toggle.  Must be between 0 and getSize() - 1 inclusive.
     */
    public void toggle(int index) {
        if (armedEntry >= 0 && armedEntry != index) disarm();
        JCheckBox entry = getEntryAt(index);
        ButtonModel entryModel = entry.getModel();
        entryModel.setArmed(false);
        entryModel.setRollover(false);
        entry.setSelected(!entry.isSelected());
        fireContentsChanged(this, index, index);
    }

    /**
     * @return true if at least one check box is checked.
     */
    public boolean isAnyEntryChecked() {
        for (JCheckBox entry : entries) {
            if (entry.isSelected()) return true;
        }
        return false;
    }

    /**
     * Get the code name for the specified entry.
     *
     * @param entry  one of the checkbox list entries.  Required.
     * @return the code name for this entry, ie "SSL_RSA_WITH_3DES_EDE_CBC_SHA".
     */
    protected static String getEntryCode(JCheckBox entry) {
        Object code = entry.getClientProperty(CLIENT_PROPERTY_ENTRY_CODE);
        return code != null ? code.toString() : entry.getText();
    }

    protected String buildEntryCodeString() {
        StringBuilder ret = new StringBuilder(128);
        boolean isFirst = true;
        for (JCheckBox entry : entries) {
            if (entry.isSelected()) {
                if (!isFirst) ret.append(',');
                ret.append(getEntryCode(entry));
                isFirst = false;
            }
        }
        return ret.toString();
    }

    /**
     * Configure the specified JList to use this as its list model.
     * <p/>
     * This will set the list model, the cell renderer, and the selection model.
     *
     * @param jList the JList to configure.  Required.
     */
    public void attachToJList(final JList jList) {
        final JCheckBoxListModel jCheckBoxListModel = this;
        jList.setModel(this);
        jList.setSelectionModel(new JCheckBoxListSelectionModel(this));
        jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jList.setCellRenderer(new ComponentListCellRenderer());
        jList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                jCheckBoxListModel.disarm();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int selectedIndex = jList.locationToIndex(e.getPoint());
                if (selectedIndex < 0) return;
                jCheckBoxListModel.disarm();
                jCheckBoxListModel.arm(selectedIndex);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int selectedIndex = jList.locationToIndex(e.getPoint());
                if (selectedIndex < 0) return;
                jCheckBoxListModel.toggle(selectedIndex);
            }
        });
        // Change unmodified space from 'addToSelection' to 'toggleCheckBox' (ie, same as our above single-click handler)
        jList.getInputMap().put(KeyStroke.getKeyStroke(' '), "toggleCheckBox");
        //noinspection CloneableClassInSecureContext
        jList.getActionMap().put("toggleCheckBox", new AbstractAction("toggleCheckBox") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = jList.getSelectedIndex();
                jCheckBoxListModel.toggle(selectedIndex);
            }
        });
    }
}
