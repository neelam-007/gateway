package com.l7tech.client.gui;

import com.l7tech.gui.util.FontUtil;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.client.gui.util.IconManager;
import com.l7tech.client.gui.dialogs.SsgPropertyDialog;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

/**
 * Panel listing known SSGs and allowing create/edit/delete.
 * User: mike
 * Date: May 29, 2003
 * Time: 3:22:24 PM
 */
class SsgListPanel extends JPanel {
    private final Logger log = Logger.getLogger(SsgListPanel.class.getName());
    private final SsgFinder ssgFinder;
    private final int bindPort;
    private final boolean savePass;
    private SsgTableModel ssgTableModel;
    private JTable ssgTable;
    private Action actionNewSsg;
    private Action actionEditSsg;
    private Action actionSetDefaultSsg;
    private Action actionDeleteSsg;
    private Action changePasswordAction;

    /**
     * Create an Ssg list panel.
     *
     * @param ssgManager       SSG manager to use for making lists of ssgs or creating new ones
     * @param bindPort         port that the proxy will listen on on localhost
     * @param toolbarHasChpass true to include "Change password" button on toolbar
     * @param savePass         true to set "Save password to disk" by default on newly created Ssgs
     */
    SsgListPanel(SsgManager ssgManager, int bindPort, boolean toolbarHasChpass, boolean savePass) {
        this.bindPort = bindPort;
        this.ssgFinder = ssgManager; // keep write privs only long enough to hand them off to the table model'
        this.savePass = savePass;
        init(ssgManager, toolbarHasChpass);
    }

    private void init(SsgManager ssgManager, boolean toolbarHasChpass) {
        setLayout(new GridBagLayout());

        final JPanel ssgListPanel = new JPanel(new BorderLayout());
        ssgListPanel.setPreferredSize(new Dimension(220, 90));

        ssgTableModel = new SsgTableModel(ssgManager);
        ssgTable = new JTable(ssgTableModel) {
            public TableCellRenderer getCellRenderer(int row, int column) {
                final TableCellRenderer ce = super.getCellRenderer(row, column);
                return new TableCellRenderer() {
                    public Component getTableCellRendererComponent(JTable table, Object value,
                                                                   boolean isSelected, boolean hasFocus,
                                                                   int row, int column) {
                        Component c = ce.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        Ssg ssg = ssgTableModel.getSsgAtRow(row);
                        if (ssg.isDefaultSsg()) {
                            FontUtil.emboldenFont(c);
                        }
                        return c;
                    }
                };
            }
        };
        ssgTable.getColumnModel().getColumn(0).setHeaderValue("Gateway");
        ssgTable.getColumnModel().getColumn(1).setHeaderValue("Proxy");
        ssgTable.getColumnModel().getColumn(2).setHeaderValue("Type");
        ssgTable.getColumnModel().getColumn(3).setHeaderValue("User Name");
        ssgTable.getSelectionModel().setSelectionInterval(0, 0);
        ssgTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ssgTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateEnableDisableState();
            }
        });
        ssgTable.getTableHeader().setReorderingAllowed(false);
        ssgTable.getTableHeader().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (ssgTableModel.getRowCount() < 3) // not enought items in list to bother worth resorting
                    return;
                int columnIndex = ssgTable.getColumnModel().getColumnIndexAtX(e.getX());
                int column = ssgTable.convertColumnIndexToModel(columnIndex);
                if (e.getClickCount() == 1 && column != -1) {
                    Ssg selectedSsg = getSelectedSsg();
                    boolean reverse = false;
                    if (ssgTableModel.getSortColumn() == column)
                        reverse = !ssgTableModel.getSortingReverse();
                    ssgTableModel.setSortOrder(column, reverse);
                    selectSsg(selectedSsg);
                }
            }
        });
        ssgTable.setColumnSelectionAllowed(false);
        ssgTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    getActionEditSsg().actionPerformed(new ActionEvent(this, 1, "properties"));
            }
        });

        add(makeToolbar(toolbarHasChpass),
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                   GridBagConstraints.NORTH,
                                   GridBagConstraints.BOTH,
                                   new Insets(0, 0, 0, 0),
                                   0, 0));
        add(ssgListPanel,
            new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                                   GridBagConstraints.SOUTH,
                                   GridBagConstraints.BOTH,
                                   new Insets(0, 0, 0, 0),
                                   0, 0));

        final JScrollPane ssgListPane = new JScrollPane(ssgTable);
        ssgListPanel.add(ssgListPane, BorderLayout.CENTER);
        updateEnableDisableState();

    }

    private JToolBar makeToolbar(boolean toolbarHasChpass) {
        final JToolBar bar = new JToolBar(JToolBar.HORIZONTAL);
        bar.setFloatable(false);
        bar.setRollover(true);
        bar.add(new JButton(getActionNewSsg()));
        bar.add(new JButton(getActionEditSsg()));
        bar.add(new JButton(getActionSetDefaultSsg()));
        bar.add(new JButton(getActionDeleteSsg()));
        if (toolbarHasChpass)
            bar.add(new JButton(getChangePasswordAction()));
        return bar;
    }

    private void updateEnableDisableState() {
        final Ssg ssg = getSelectedSsg();
        boolean haveSsg = ssg != null;
        getActionDeleteSsg().setEnabled(haveSsg);
        getActionEditSsg().setEnabled(haveSsg);
        getActionSetDefaultSsg().setEnabled(haveSsg);
        getChangePasswordAction().setEnabled(haveSsg && ssg.isPasswordChangeServiceSupported());
    }

    /**
     * Get the currently selected SSG, or null if no SSG is selected.
     *
     * @return The selected SSG or null.
     */
    Ssg getSelectedSsg() {
        return ssgTableModel.getSsgAtRow(ssgTable.getSelectedRow());
    }

    /**
     * Set the selection bar to cover the specified Ssg, if it's in the table.
     * Otherwise the selection will be left alone.
     * @param ssg
     */
    void selectSsg(Ssg ssg) {
        int row = ssgTableModel.getRow(ssg);
        if (row >= 0)
            ssgTable.getSelectionModel().setSelectionInterval(row, row);
    }

    Action getActionDeleteSsg() {
        if (actionDeleteSsg == null)
            actionDeleteSsg = new DeleteSsgAction(this, ssgFinder);
        return actionDeleteSsg;
    }

    Action getActionEditSsg() {
        if (actionEditSsg == null) {
            actionEditSsg = new AbstractAction("Properties", IconManager.getEdit()) {
                public void actionPerformed(final ActionEvent e) {
                    final Ssg ssg = getSelectedSsg();
                    log.info("Editing ssg " + ssg);
                    if (ssg != null) {
                        final SsgPropertyDialog ssgPropertyDialog = SsgPropertyDialog.makeSsgPropertyDialog(ssg, ssgTableModel.getSsgFinder(), bindPort);
                        final boolean result = ssgPropertyDialog.runDialog();
                        if (result) {
                            if (ssg.isDefaultSsg())
                                ssgTableModel.setDefaultSsg(ssg);
                            ssgTableModel.editedSsg(ssg);
                            selectSsg(ssg);
                        }
                        // Regardless of whether this dialog was OK'ed or canceled, turn off all anti-popup-spamming
                        // flags on all registered SSGs (Bug #1325)
                        int rows = ssgTableModel.getRowCount();
                        for (int i = 0; i < rows; ++i) {
                            Ssg s = ssgTableModel.getSsgAtRow(i);
                            if (s != null)
                                s.getRuntime().promptForUsernameAndPassword(true);
                        }
                    }
                }
            };
            actionEditSsg.putValue(Action.SHORT_DESCRIPTION, "View or change properties associated with this Gateway");
            actionEditSsg.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
        }
        return actionEditSsg;
    }

    Action getActionNewSsg() {
        if (actionNewSsg == null)
            actionNewSsg = new NewSsgAction(this, ssgTableModel, ssgFinder, bindPort, savePass);
        return actionNewSsg;
    }

    Action getActionSetDefaultSsg() {
        if (actionSetDefaultSsg == null) {
            actionSetDefaultSsg = new AbstractAction("Set Default", IconManager.getDefault()) {
                public void actionPerformed(final ActionEvent e) {
                    final Ssg ssg = getSelectedSsg();
                    if (ssg != null) {
                        log.info("Setting default Gateway to " + ssg);
                        ssgTableModel.setDefaultSsg(ssg);
                        ssgTableModel.editedSsg(ssg);
                        selectSsg(ssg);
                    }
                }
            };
            actionSetDefaultSsg.putValue(Action.SHORT_DESCRIPTION, "Set this Gateway as the default");
            actionSetDefaultSsg.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
        }
        return actionSetDefaultSsg;
    }

    /**
     * The action of changing one's password and revoking one's client certificate on an SSG.
     */
    Action getChangePasswordAction() {
        if (changePasswordAction == null) {
            changePasswordAction = new ChangePasswordAction(this);
            changePasswordAction.putValue(Action.SHORT_DESCRIPTION,
                "Request the selected Gateway to change this account's password and revoke any client certificate");
            changePasswordAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
        }
        return changePasswordAction;
    }

    /**
     * Get the number of registered SSGs.
     * @return the number of registered SSGs.
     */
    int getNumSsgs() {
        return ssgTableModel.getRowCount();
    }

    /**
     * Select the row containing the default Ssg.  If there is no default Ssg, selects the first Ssg
     * on the list, if any.
     */
    void selectDefaultSsg() {
        int rowCount = ssgTableModel.getRowCount();
        if (rowCount < 1)
            return;
        for (int i = 0; i < rowCount; ++i) {
            Ssg ssg = ssgTableModel.getSsgAtRow(i);
            if (ssg != null && ssg.isDefaultSsg()) {
                ssgTable.getSelectionModel().setSelectionInterval(i, i);
                return;
            }
        }
        ssgTable.getSelectionModel().setSelectionInterval(0, 0);
    }

    /** Remove the specified Ssg from the SsgManager (and hence from ssgs.xml) and also from the table model. */
    void removeSsg(Ssg ssg) {
        ssgTableModel.removeSsg(ssg);
    }

}
