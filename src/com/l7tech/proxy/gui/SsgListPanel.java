package com.l7tech.proxy.gui;

import com.l7tech.common.gui.util.FontUtil;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.gui.dialogs.SsgPropertyDialog;
import com.l7tech.proxy.gui.util.IconManager;

import javax.swing.*;
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
    private final ClientProxy clientProxy;
    private SsgTableModel ssgTableModel;
    private JTable ssgTable;
    private Action actionNewSsg;
    private Action actionEditSsg;
    private Action actionSetDefaultSsg;
    private Action actionDeleteSsg;
    private Action actionEmptyCookieCache;
    private Action changePasswordAction;

    SsgListPanel(ClientProxy clientProxy, SsgManager ssgManager) {
        this.clientProxy = clientProxy;
        init(ssgManager);
    }

    private void init(SsgManager ssgManager) {
        setLayout(new GridBagLayout());

        final JPanel ssgListPanel = new JPanel(new BorderLayout());
        ssgListPanel.setPreferredSize(new Dimension(220, 90));

        final JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        add(toolBar,
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
        ssgTable.getTableHeader().setReorderingAllowed(false);
        ssgTable.getTableHeader().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (ssgTableModel.getRowCount() < 3)
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


        final JScrollPane ssgListPane = new JScrollPane(ssgTable);
        ssgListPanel.add(ssgListPane, BorderLayout.CENTER);

        toolBar.add(new JButton(getActionNewSsg()));
        toolBar.add(new JButton(getActionEditSsg()));
        toolBar.add(new JButton(getActionSetDefaultSsg()));
        toolBar.add(new JButton(getActionDeleteSsg()));
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
            actionDeleteSsg = new DeleteSsgAction(this, clientProxy);
        return actionDeleteSsg;
    }

    Action getActionEditSsg() {
        if (actionEditSsg == null) {
            actionEditSsg = new AbstractAction("Properties", IconManager.getEdit()) {
                public void actionPerformed(final ActionEvent e) {
                    final Ssg ssg = getSelectedSsg();
                    log.info("Editing ssg " + ssg);
                    if (ssg != null) {
                        final SsgPropertyDialog ssgPropertyDialog = SsgPropertyDialog.makeSsgPropertyDialog(clientProxy, ssg);
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
            actionNewSsg = new NewSsgAction(this, ssgTableModel, clientProxy);
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
     * The action of deleting all HTTP cookies from cache.
     *
     * @return  Action  an reference to the the action object.
     */
    Action getActionEmptyCookieCache() {
        if (actionEmptyCookieCache == null) {
            actionEmptyCookieCache = new AbstractAction("Empty Cookie Cache", IconManager.getRemove()) {
                public void actionPerformed(final ActionEvent e) {

                    for (int i = 0; i < ssgTableModel.getRowCount(); i++) {
                        Ssg ssg = ssgTableModel.getSsgAtRow(i);
                        ssg.getRuntime().clearSessionCookies();
                    }
                }
            };
            actionEmptyCookieCache.putValue(Action.SHORT_DESCRIPTION, "Delete session cookies of all SSGs from the cache.");
            actionEmptyCookieCache.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
        }
        return actionEmptyCookieCache;
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
