/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.gui.dialogs.NewSsgDialog;
import com.l7tech.proxy.gui.dialogs.SsgPropertyDialog;
import com.l7tech.proxy.gui.util.IconManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

/**
 * Action to create a new Gateway registration.
 */
class NewSsgAction extends AbstractAction {
    private static final Logger logger = Logger.getLogger(NewSsgAction.class.getName());
    private final SsgListPanel ssgListPanel;
    private final ClientProxy clientProxy;
    private final SsgTableModel ssgTableModel;

    public NewSsgAction(SsgListPanel ssgListPanel, SsgTableModel ssgTableModel, ClientProxy clientProxy) {
        super("New", IconManager.getAdd());
        this.ssgListPanel = ssgListPanel;
        this.clientProxy = clientProxy;
        this.ssgTableModel = ssgTableModel;
        putValue(Action.SHORT_DESCRIPTION, "Register a new Gateway with this Bridge");
        putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
    }

    public void actionPerformed(final ActionEvent e) {
        final Ssg newSsg = ssgTableModel.createSsg();
        logger.info("Creating new Gateway registration " + newSsg);
        newSsg.getRuntime().getSsgKeyStoreManager().deleteStores();
        if (ssgTableModel.getRowCount() < 1)
            newSsg.setDefaultSsg(true);

        NewSsgDialog newSsgDialog = new NewSsgDialog(newSsg, clientProxy.getSsgFinder(), Gui.getInstance().getFrame(),
                                                     "Create Gateway Registration", true);
        newSsgDialog.pack();
        Utilities.centerOnScreen(newSsgDialog);
        newSsgDialog.show();
        if (newSsgDialog.getSsg() == null)
            return;

        final SsgPropertyDialog ssgPropertyDialog = SsgPropertyDialog.makeSsgPropertyDialog(clientProxy, newSsg);
        final boolean result = ssgPropertyDialog.runDialog();
        if (result) {
            ssgTableModel.addSsg(newSsg);
            ssgListPanel.selectSsg(newSsg);
        }
    }
}
