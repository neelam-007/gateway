package com.l7tech.proxy.gui;

import com.l7tech.common.gui.util.FontUtil;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.datamodel.*;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.gui.dialogs.PasswordDialog;
import com.l7tech.proxy.gui.dialogs.SsgPropertyDialog;
import com.l7tech.proxy.gui.util.IconManager;
import com.l7tech.proxy.processor.MessageProcessor;
import com.l7tech.proxy.util.SslUtils;

import javax.net.ssl.SSLException;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.KeyStoreException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;

/**
 * Panel listing known SSGs and allowing create/edit/delete.
 * User: mike
 * Date: May 29, 2003
 * Time: 3:22:24 PM
 */
public class SsgListPanel extends JPanel {
    private final Logger log = Logger.getLogger(SsgListPanel.class.getName());
    private SsgTableModel ssgTableModel;
    private JTable ssgTable;
    private Action actionNewSsg;
    private Action actionEditSsg;
    private Action actionSetDefaultSsg;
    private Action actionDeleteSsg;
    private ClientProxy clientProxy;
    private Action actionEmptyCookieCache;
    private Action actionChangePasswordAndRevokeClientCertificate;

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
        ssgTable.getColumnModel().getColumn(3).setHeaderValue("Username");
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
    public Ssg getSelectedSsg() {
        return ssgTableModel.getSsgAtRow(ssgTable.getSelectedRow());
    }

    /**
     * Set the selection bar to cover the specified Ssg, if it's in the table.
     * Otherwise the selection will be left alone.
     * @param ssg
     */
    private void selectSsg(Ssg ssg) {
        int row = ssgTableModel.getRow(ssg);
        if (row >= 0)
            ssgTable.getSelectionModel().setSelectionInterval(row, row);
    }

    public Action getActionDeleteSsg() {
        if (actionDeleteSsg == null) {
            actionDeleteSsg = new AbstractAction("Remove", IconManager.getRemove()) {
                public void actionPerformed(final ActionEvent e) {
                    final Ssg ssg = getSelectedSsg();
                    log.info("Removing Gateway " + ssg);
                    if (ssg == null)
                        return;

                    // check if there is any Federated Gateway using this as a Trusted Gateway.
                    Collection ssgs = clientProxy.getSsgFinder().getSsgList();
                    Collection trusting = new ArrayList();
                    for (Iterator i = ssgs.iterator(); i.hasNext();) {
                        Ssg s = (Ssg)i.next();
                        if (s.getTrustedGateway() == ssg)
                            trusting.add(s);
                    }
                    if (!trusting.isEmpty()) {
                        StringBuffer sb = new StringBuffer();
                        int added = 0;
                        for (Iterator i = trusting.iterator(); i.hasNext();) {
                            Ssg t = (Ssg)i.next();
                            String un = t.getUsername() == null ? "" : " (" + t.getUsername() + ")";
                            sb.append("   " + t.getLocalEndpoint() + ": " + t.getSsgAddress() + un + "\n");
                            added++;
                            if (added >= 10) {
                                sb.append("   ...and " + (trusting.size() - added) + " more\n");
                                break;
                            }
                        }
                        String msg = "The following Federated Gateways are using this as a Trusted Gateway:\n\n" +
                                sb.toString() + "\nThese Federated Gateways must be removed before this Trusted Gateway can be removed.";
                        Gui.errorMessage(msg);
                        return;
                    }

                    Object[] options = { "Remove", "Cancel" };
                    int result = JOptionPane.showOptionDialog(null,
                                                              "Are you sure you want to remove the " +
                                                              "registration for the Gateway " + ssg + "?\n" +
                                                              "This action cannot be undone.",
                                                              "Delete Gateway?",
                                                              0, JOptionPane.WARNING_MESSAGE,
                                                              null, options, options[1]);
                    if (result == 0) {
                        try {
                            if (ssg.getTrustedGateway() == null && SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
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
                                SsgKeyStoreManager.deleteStores(ssg);
                                // FALLTHROUGH -- continue with newly-emptied keystore
                            } catch (OperationCanceledException e2) {
                                return; // cancel the remove as well
                            }
                        }

                        ssgTableModel.removeSsg(ssg);
                        ssg.setTrustedGateway(null); // break federation prior to removing key stores
                        SsgKeyStoreManager.deleteStores(ssg);
                    }
                }
            };
            actionDeleteSsg.putValue(Action.SHORT_DESCRIPTION, "Remove this Gateway registration");
            actionDeleteSsg.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
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
                        final SsgPropertyDialog ssgPropertyDialog = SsgPropertyDialog.makeSsgPropertyDialog(clientProxy, ssg, false);
                        final boolean result = ssgPropertyDialog.runDialog();
                        if (result) {
                            if (ssg.isDefaultSsg())
                                ssgTableModel.setDefaultSsg(ssg);
                            ssgTableModel.editedSsg(ssg);
                            selectSsg(ssg);
                        }
                    }
                }
            };
            actionEditSsg.putValue(Action.SHORT_DESCRIPTION, "View or change properties associated with this Gateway");
            actionEditSsg.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
        }
        return actionEditSsg;
    }

    public Action getActionNewSsg() {
        if (actionNewSsg == null) {
            actionNewSsg = new AbstractAction("New", IconManager.getAdd()) {
                public void actionPerformed(final ActionEvent e) {
                    final Ssg newSsg = ssgTableModel.createSsg();
                    log.info("Creating new Gateway registration " + newSsg);
                    if (ssgTableModel.getRowCount() < 1)
                        newSsg.setDefaultSsg(true);
                    final SsgPropertyDialog ssgPropertyDialog = SsgPropertyDialog.makeSsgPropertyDialog(clientProxy, newSsg, true);
                    final boolean result = ssgPropertyDialog.runDialog();
                    if (result) {
                        ssgTableModel.addSsg(newSsg);
                        selectSsg(newSsg);
                    }
                }
            };
            actionNewSsg.putValue(Action.SHORT_DESCRIPTION, "Register a new Gateway with this Agent");
            actionNewSsg.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
        }
        return actionNewSsg;
    }

    public Action getActionSetDefaultSsg() {
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
    public Action getActionEmptyCookieCache() {
        if (actionEmptyCookieCache == null) {
            actionEmptyCookieCache = new AbstractAction("Empty Cookie Cache", IconManager.getRemove()) {
                public void actionPerformed(final ActionEvent e) {

                    for (int i = 0; i < ssgTableModel.getRowCount(); i++) {
                        Ssg ssg = ssgTableModel.getSsgAtRow(i);
                        ssg.clearSessionCookies();
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
    public Action getActionChangePasswordAndRevokeClientCertificate() {
        if (actionChangePasswordAndRevokeClientCertificate == null) {
            actionChangePasswordAndRevokeClientCertificate = new AbstractAction("Change Password/Revoke Certificate", IconManager.getCert()) {
                public void actionPerformed(final ActionEvent e) {
                    final Ssg ssg = getSelectedSsg();
                    if (ssg != null) {
                        if (ssg.getTrustedGateway() != null) {
                            Gui.errorMessage("This is a Federated Gateway.  You must perform this\n" +
                                             "action on the corresponding Trusted Gateway instead." );
                            return;
                        }

                        boolean retry = true;
                        boolean prompted = false;
                        char[] newpass = null;
                        while (retry) {
                            retry = false;
                            try {
                                if (!prompted) {
                                    Object[] certoptions;
                                    String title;
                                    String message;
                                    if (SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
                                        certoptions = new Object[] {"Revoke Certificate", "Cancel"};
                                        title = "Gateway " + ssg + ": Change Password and Revoke Client Certificate";
                                        message = "You are about to send a request to\n" +
                                                "the Gateway \"" + ssg + "\" for it\n" +
                                                "to change your password for this account\n" +
                                                "and revoke your current Client Certificate.\n" +
                                                "Are you sure you wish to proceed?\n\n" +
                                                "This action cannot be undone.";
                                    } else {
                                        certoptions = new Object[] {"Change Password", "Cancel"};
                                        title = "Gateway " + ssg + ": Change Password";
                                        message = "You are about to send a request to\n" +
                                                "the Gateway \"" + ssg + "\" for it\n" +
                                                "to change your password for this account.\n" +
                                                "Are you sure you wish to proceed?\n\n" +
                                                "This action cannot be undone.";
                                    }
                                    int res2 = JOptionPane.showOptionDialog(null, message, title,
                                                                            0, JOptionPane.WARNING_MESSAGE,
                                                                            null, certoptions, certoptions[1]);
                                    if (res2 != 0)
                                        return;

                                    prompted = true;
                                }

                                PasswordAuthentication pw = Managers.getCredentialManager().getCredentials(ssg);
                                if (newpass == null)
                                    newpass = PasswordDialog.getPassword(Gui.getInstance().getFrame(), "Enter desired new password");
                                if (newpass == null)
                                    return;

                                CurrentRequest.setCurrentSsg(ssg);
                                SslUtils.changePasswordAndRevokeClientCertificate(ssg,
                                                                                  pw.getUserName(),
                                                                                  pw.getPassword(),
                                                                                  newpass);

                                // Succeeded, so update password and client cert
                                ssg.cmPassword(newpass);
                                SsgKeyStoreManager.deleteClientCert(ssg);

                            } catch (KeyStoreCorruptException e1) {
                                try {
                                    Managers.getCredentialManager().notifyKeyStoreCorrupt(ssg);
                                    SsgKeyStoreManager.deleteStores(ssg);
                                    retry = true;
                                    // FALLTHROUGH -- retry with newly-emptied keystore
                                } catch (OperationCanceledException e2) {
                                    return; // cancel the password change as well
                                }
                            } catch (OperationCanceledException e1) {
                                return;
                            } catch (SSLException sslException) {
                                PasswordAuthentication credentials = null;
                                try {
                                    credentials = Managers.getCredentialManager().getCredentials(ssg);
                                    MessageProcessor.handleSslException(ssg, credentials, sslException);
                                    retry = true;
                                    // FALLTHROUGH -- retry with newly-(re?)learned server certificate
                                } catch (OperationCanceledException e1) {
                                    return;
                                } catch (BadCredentialsException e1) {
                                    log.log(Level.WARNING, e1.getMessage(), e1);
                                    Gui.errorMessage("Unable to change your password -- the Gateway reports that your current " +
                                                     "password or client certificate is not valid.");
                                    return;
                                } catch (Exception e1) {
                                    log.log(Level.WARNING, e1.getMessage(), e1);
                                    Gui.errorMessage("Unable to change your password", "Unable to negotiate an SSL connection " +
                                                                                       "with the Gateway.", e1);
                                    return;
                                }
                            } catch (IOException e1) {
                                log.log(Level.WARNING, e1.getMessage(), e1);
                                Gui.errorMessage("Unable to change your password", "The Gateway was unable to change " +
                                                                                   "your password.", e1);
                                return;
                            } catch (BadCredentialsException e1) {
                                log.log(Level.WARNING, e1.getMessage(), e1);
                                Gui.errorMessage("Unable to change your password -- the Gateway reports that your current " +
                                                 "password or client certificate is not valid.");
                                return;
                            } catch (KeyStoreException e1) {
                                log.log(Level.WARNING, e1.getMessage(), e1);
                                Gui.errorMessage("Unable to remove your existing client certificate",
                                                 "There was an error while attempting to remove our local copy of your " +
                                                 "newly-revoked client certificate.", e1);
                                return;
                            } finally {
                                CurrentRequest.clearCurrentRequest();
                            }
                        }

                        JOptionPane.showMessageDialog(Gui.getInstance().getFrame(),
                                                      "Your password has been changed successfully.",
                                                      "Password change succeeded",
                                                      JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            };
            actionChangePasswordAndRevokeClientCertificate.putValue(Action.SHORT_DESCRIPTION,
                "Request the selected Gateway to change this account's password and revoke any client certificate");
            actionChangePasswordAndRevokeClientCertificate.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
        }
        return actionChangePasswordAndRevokeClientCertificate;
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
