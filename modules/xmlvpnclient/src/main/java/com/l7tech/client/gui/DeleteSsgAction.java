/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.client.gui;

import com.l7tech.client.gui.util.IconManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Client-side action which deletes the current Gateway registration.
 */
class DeleteSsgAction extends AbstractAction {
    private static final Logger log = Logger.getLogger(DeleteSsgAction.class.getName());

    private final SsgListPanel ssgListPanel;
    private final SsgFinder ssgFinder;

    /**
     * Create an action that will delete the currently selected SSG in the SsgListPanel.
     *
     * @param ssgListPanel  identifies which SSG to eventually delete
     * @param ssgFinder     used to check if some federated SSG uses this SSG as its trusted SSG
     */
    public DeleteSsgAction(SsgListPanel ssgListPanel, SsgFinder ssgFinder) {
        super("Delete Account", IconManager.getRemove());
        this.ssgListPanel = ssgListPanel;
        this.ssgFinder = ssgFinder;
        putValue(Action.SHORT_DESCRIPTION, "Delete this Gateway Account");
        putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
    }

    public void actionPerformed(final ActionEvent e) {
        final Ssg ssg = ssgListPanel.getSelectedSsg();
        log.info("Removing Gateway " + ssg);
        if (ssg == null)
            return;

        // check if there is any Federated Gateway using this as a Trusted Gateway.
        Collection ssgs = ssgFinder.getSsgList();
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
                sb.append("   ").append(t.getLocalEndpoint()).append(": ").append(t.getSsgAddress()).append(un).append("\n");
                added++;
                if (added >= 10) {
                    sb.append("   ...and ").append(trusting.size() - added).append(" more\n");
                    break;
                }
            }
            String msg = "The following Federated Gateways are using this as a Trusted Gateway:\n\n" +
                    sb.toString() + "\nThese Federated Gateways must be deleted before this Trusted Gateway can be deleted.";
            Gui.errorMessage(msg);
            return;
        }

        Object[] options = { "Delete", "Cancel" };
        int result = JOptionPane.showOptionDialog(null,
                                                  "Warning: Deleting the Gateway Account for " + ssg + "\n" +
                                                  "will delete any associated certificate and policies from the "+Gui.APP_NAME+".\n" +
                                                  "The action cannot be undone.",
                                                  "Delete Gateway Account",
                                                  0, JOptionPane.WARNING_MESSAGE,
                                                  null, options, options[1]);
        if (result == 0) {
            if (!ssg.isFederatedGateway() && ssg.getClientCertificate() != null) {
                Object[] certoptions = { "Destroy Certificate", "Cancel Account Deletion" };
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

            ssgListPanel.removeSsg(ssg);
            ssg.setTrustedGateway(null); // break federation prior to removing key stores
            ssg.setWsTrustSamlTokenStrategy(null); // break federation prior to removing key stores TODO fix hack
            ssg.getRuntime().getSsgKeyStoreManager().deleteStores();
        }
    }
}
