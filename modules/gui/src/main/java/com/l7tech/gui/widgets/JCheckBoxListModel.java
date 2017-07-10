package com.l7tech.gui.widgets;

import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A ListModel that holds a number of JCheckBox elements as its list entries.
 */
public class  JCheckBoxListModel
        extends AbstractListModel<JCheckBox>
        implements JCheckBoxListModelAware {
    public static final String CLIENT_PROPERTY_ENTRY_CODE = "JCheckBoxListModel.entryCode";

    /** A predicate that will match checkboxes that are currently checked. */
    public static final Functions.Unary<Boolean,JCheckBox> MATCH_CHECKED_PREDICATE = Functions.propertyTransform(JCheckBox.class, "selected");

    /** A predicate that will match checkboxes that are currently unchecked. */
    public static final Functions.Unary<Boolean,JCheckBox> MATCH_UNCHECKED_PREDICATE = Functions.negate(MATCH_CHECKED_PREDICATE);

    private List<JCheckBox> entries;
    private int armedEntry = -1;

    public JCheckBoxListModel(List<JCheckBox> entries) {
        this.entries = new ArrayList<>(entries);
    }

    protected void setEntries(List<JCheckBox> entries) {
        int oldSize = getSize();
        this.entries = new ArrayList<>(entries);
        fireContentsChanged(this, 0, Math.max(oldSize, entries.size()));
    }

    protected List<JCheckBox> getEntries() {
        return entries;
    }

    @Override
    public int getSize() {
        return entries.size();
    }

    @Override
    public JCheckBox getElementAt(int index) {
        return entries.get(index);
    }

    @Override
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
    @Override
    public void arm(int index) {
        disarm();
        if (index < 0) return;
        ButtonModel entryModel = getElementAt(index).getModel();
        entryModel.setArmed(true);
        entryModel.setRollover(true);
        armedEntry = index;
        fireContentsChanged(this, armedEntry, armedEntry);
    }

    /**
     * Clear the "armed" state from any checkbox that was armed by a call to {@link #arm}.
     */
    @Override
    public void disarm() {
        if (armedEntry >= 0) {
            getElementAt(armedEntry).getModel().setArmed(false);
            fireContentsChanged(this, armedEntry, armedEntry);
            armedEntry = -1;
        }
    }

    /**
     * Toggle the checkbox at the specified index.
     * @param index the index to toggle.  Must be between 0 and getSize() - 1 inclusive.
     */
    @Override
    public void toggle(int index) {
        if (armedEntry >= 0 && armedEntry != index) disarm();
        JCheckBox entry = getElementAt(index);
        if (entry.isEnabled()) {
            ButtonModel entryModel = entry.getModel();
            entryModel.setArmed(false);
            entryModel.setRollover(false);
            entry.setSelected(!entry.isSelected());
            fireContentsChanged(this, index, index);
        }
    }

    /**
     * Configure all checkbox states.
     *
     * @param checker a checker that will be given a JCheckBox instance to examine, along with its position in the list.
     *        The checker should return "true" if the checkbox should be checked, "false" if it should be unchecked,
     *        or null to leave the current state unchanged. The checker should <b>not</b> modify the JCheckBox itself.
     */
    public void visitEntries(Functions.Binary<Boolean,Integer,JCheckBox> checker) {
        disarm();
        int[] modifiedRange = visitEntriesForStateChange(this, checker);
        if (modifiedRange[0] <= modifiedRange[1]) {
            fireContentsChanged(this, modifiedRange[0], modifiedRange[1]);
        }
    }

    /**
     * Visits entries for state change in the JCheckBox list model.
     * @param model
     * @param checker a checker that will be given a JCheckBox instance to examine, along with its position in the list.
     *        The checker should return "true" if the checkbox should be checked, "false" if it should be unchecked,
     *        or null to leave the current state unchanged. The checker should <b>not</b> modify the JCheckBox itself.
     * @return range of modified entries (lowest and highest).
     */
    public static int[] visitEntriesForStateChange(final ListModel<JCheckBox> model,
                                    final Functions.Binary<Boolean, Integer, JCheckBox> checker) {
        int[] modifiedRange = { Integer.MAX_VALUE, Integer.MIN_VALUE };

        for (int index = 0; index < model.getSize(); index++) {
            final JCheckBox entry = model.getElementAt(index);
            final Boolean wantChecked = checker.call(index, entry);

            if (wantChecked != null && updateEntryState(entry, wantChecked)) {
                if (modifiedRange[0] > index) {
                    modifiedRange[0] = index;
                }
                modifiedRange[1] = index;
            }
        }

        return modifiedRange;
    }

    /**
     * Updates the entry state if required.
     * @param entry JCheckBox entry
     * @param wantChecked true to be checked.
     * @return true if the entry state is updated. Otherwise returns false.
     */
    private static boolean updateEntryState(JCheckBox entry, boolean wantChecked) {
        final boolean wasChecked = entry.isSelected();

        if (wasChecked != wantChecked) {
            ButtonModel entryModel = entry.getModel();

            entryModel.setArmed(false);
            entryModel.setRollover(false);
            entry.setSelected(wantChecked);

            return true;
        }

        return false;
    }

    /**
     * Enable/disable the checkboxes in the list.
     */
    public void setEnabled(final boolean enabled) {
        for (int index = 0; index < entries.size(); ++index) {
            JCheckBox entry = entries.get(index);
            entry.setEnabled(enabled);
        }
    }

    /**
     * Return all entries that satisfy the predicate.
     *
     * @param predicate  the predicate to invoke to determine whether an entry should be included in the returned list.  Required.
     * @return a list of all entries for which the predicate returned true.  May be empty but never null.
     */
    public List<JCheckBox> filterEntries(Functions.Unary<Boolean, JCheckBox> predicate) {
        List<JCheckBox> ret = new ArrayList<JCheckBox>();
        for (JCheckBox entry : entries) {
            if (predicate.call(entry))
                ret.add(entry);
        }
        return ret;
    }

    /**
     * Return all entries whose checkbox is currently checked.
     *
     * @return a list of all entries with checked checkboxes.  May be empty but never null.
     */
    public List<JCheckBox> getAllCheckedEntries() {
        return filterEntries(MATCH_CHECKED_PREDICATE);
    }

    /**
     * Return all entries whose checkbox is currently unchecked.
     *
     * @return a list of all entries with unchecked checkboxes.  May be empty but never null.
     */
    public List<JCheckBox> getAllUncheckedEntries() {
        return filterEntries(MATCH_UNCHECKED_PREDICATE);
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
     * Configure the specified JList to use this as its list model.
     * <p/>
     * This will set the list model, the cell renderer, and the selection model.
     *
     * @param jList the JList to configure.  Required.
     */
    public void attachToJList(final JList jList) {
        JCheckBoxListModel.attachToJList(jList, this, this);
    }

    /**
     * Configures the JList with the provided list model and its corresponding list model aware.
     * @param jList the JList to configure.
     * @param listModel Any JCheckBox list model
     * @param listModelAware Any JCheckBoxListModelAware instance
     */
    public static void attachToJList(@NotNull final JList jList,
                                     @NotNull final ListModel<JCheckBox> listModel,
                                     @NotNull final JCheckBoxListModelAware listModelAware) {
        jList.setModel(listModel);
        jList.setSelectionModel(new DefaultListSelectionModel() {
            public void setSelectionInterval(int index0, int index1) {
                super.setSelectionInterval(index0, index1);
                listModelAware.arm(index0);
            }
        });
        jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jList.setCellRenderer(new ComponentListCellRenderer());
        jList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                listModelAware.disarm();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int selectedIndex = jList.locationToIndex(e.getPoint());
                if (selectedIndex < 0) return;
                listModelAware.disarm();
                listModelAware.arm(selectedIndex);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int selectedIndex = jList.locationToIndex(e.getPoint());
                if (selectedIndex < 0) return;
                listModelAware.toggle(selectedIndex);
            }
        });

        // Change unmodified space from 'addToSelection' to 'toggleCheckBox' (ie, same as our above single-click handler)
        jList.getInputMap().put(KeyStroke.getKeyStroke(' '), "toggleCheckBox");

        //noinspection CloneableClassInSecureContext
        jList.getActionMap().put("toggleCheckBox", new AbstractAction("toggleCheckBox") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = jList.getSelectedIndex();
                listModelAware.toggle(selectedIndex);
            }
        });
    }
}
