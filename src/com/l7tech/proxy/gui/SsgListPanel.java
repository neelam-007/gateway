package com.l7tech.proxy.gui;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgManager;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.gui.util.IconManager;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.util.ClientLogger;
import com.l7tech.common.gui.util.FontUtil;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Panel listing known SSGs and allowing create/edit/delete.
 * User: mike
 * Date: May 29, 2003
 * Time: 3:22:24 PM
 */
public class SsgListPanel extends JPanel {
    private final ClientLogger log = ClientLogger.getInstance(SsgListPanel.class);
    private SsgTableModel ssgTableModel;
    private JTable ssgTable;
    private Action actionNewSsg;
    private Action actionEditSsg;
    private Action actionSetDefaultSsg;
    private Action actionDeleteSsg;
    private ClientProxy clientProxy;

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
        ssgTable.getColumnModel().getColumn(2).setHeaderValue("Username");
        ssgTable.getSelectionModel().setSelectionInterval(0, 0);
        ssgTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ssgTable.getTableHeader().setReorderingAllowed(false);
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
    public Ssg getSelectedSsg() {
        return ssgTableModel.getSsgAtRow(ssgTable.getSelectedRow());
    }

    public Action getActionDeleteSsg() {
        if (actionDeleteSsg == null) {
            actionDeleteSsg = new AbstractAction("Delete", IconManager.getRemove()) {
                public void actionPerformed(final ActionEvent e) {
                    final Ssg ssg = getSelectedSsg();
                    log.info("Removing Gateway " + ssg);
                    if (ssg == null)
                        return;

                    Object[] options = { "Delete", "Cancel" };
                    int result = JOptionPane.showOptionDialog(null,
                                                              "Are you sure you want to remove the " +
                                                              "registration for the Gateway " + ssg + "?\n" +
                                                              "This action cannot be undone.",
                                                              "Delete Gateway?",
                                                              0, JOptionPane.WARNING_MESSAGE,
                                                              null, options, options[1]);
                    if (result == 0) {
                        try {
                            if (SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
                                Object[] certoptions = { "Destroy Certificate", "Cancel" };
                                int res2 = JOptionPane.showOptionDialog(null,
                                                                        "You have a Client Certificate assigned from this Gateway. \n" +
                                                                        "If you delete it, you will not be able to get another one \n" +
                                                                        "for your account until a Gateway administrator revokes your \n" +
                                                                        "old one and changes your password.  Are you sure you want to \n" +
                                                                        "delete your Client Certificate?\n\n" +
                                                                        "This action cannot be undone.",
                                                                        "Delete Client Certificate Forever?",
                                                                        0, JOptionPane.WARNING_MESSAGE,
                                                                        null, certoptions, certoptions[1]);
                                if (res2 != 0)
                                    return;
                            }
                        } catch (KeyStoreCorruptException e1) {
                            try {
                                Managers.getCredentialManager().notifyKeyStoreCorrupt(ssg);
                                SsgKeyStoreManager.deleteKeyStore(ssg);
                                // FALLTHROUGH -- continue with newly-emptied keystore
                            } catch (OperationCanceledException e2) {
                                return; // cancel the remove as well
                            }
                        }

                        ssgTableModel.removeSsg(ssg);
                        SsgKeyStoreManager.deleteKeyStore(ssg);
                    }
                }
            };
            actionDeleteSsg.putValue(Action.SHORT_DESCRIPTION, "Remove this Gateway registration");
        }
        return actionDeleteSsg;
    }

    public Action getActionEditSsg() {
        if (actionEditSsg == null) {
            actionEditSsg = new AbstractAction("Properties", IconManager.getEdit()) {
                public void actionPerformed(final ActionEvent e) {
                    final Ssg ssg = getSelectedSsg();
                    log.info("Editing ssg " + ssg);
                    if (ssg != null) {
                        if (SsgPropertyDialog.makeSsgPropertyDialog(clientProxy, ssg).runDialog()) {
                            if (ssg.isDefaultSsg())
                                ssgTableModel.setDefaultSsg(ssg);
                            ssgTableModel.editedSsg();
                        }
                    }
                }
            };
            actionEditSsg.putValue(Action.SHORT_DESCRIPTION, "View or change properties associated with this Gateway");
        }
        return actionEditSsg;
    }

    public Action getActionNewSsg() {
        if (actionNewSsg == null) {
            actionNewSsg = new AbstractAction("New", IconManager.getAdd()) {
                public void actionPerformed(final ActionEvent e) {
                    final Ssg newSsg = ssgTableModel.createSsg();
                    log.info("Creating new SSG " + newSsg);
                    if (ssgTableModel.getRowCount() < 1)
                        newSsg.setDefaultSsg(true);
                        if (SsgPropertyDialog.makeSsgPropertyDialog(clientProxy, newSsg).runDialog())
                            ssgTableModel.addSsg(newSsg);
                }
            };
            actionNewSsg.putValue(Action.SHORT_DESCRIPTION, "Register a new Gateway with this Agent");
        }
        return actionNewSsg;
    }

    public Action getActionSetDefaultSsg() {
        if (actionSetDefaultSsg == null) {
            actionSetDefaultSsg = new AbstractAction("Set Default", IconManager.getDefault()) {
                public void actionPerformed(final ActionEvent e) {
                    final Ssg ssg = getSelectedSsg();
                    log.info("Setting default ssg to " + ssg);
                    if (ssg != null)
                        ssgTableModel.setDefaultSsg(ssg);
                    ssgTableModel.editedSsg();
                }
            };
            actionSetDefaultSsg.putValue(Action.SHORT_DESCRIPTION, "Set this Gateway as the default");
        }
        return actionSetDefaultSsg;
    }

    /**
     * Get the number of registered SSGs.
     * @return the number of registered SSGs.
     */
    public int getNumSsgs() {
        return ssgTableModel.getRowCount();
    }

    /**
     * Select the row containing the default Ssg.  If there is no default Ssg, selects the first Ssg
     * on the list, if any.
     */
    public void selectDefaultSsg() {
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
}
