/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.client.gui;

import com.l7tech.gui.util.Utilities;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.client.gui.dialogs.NewSsgDialog;
import com.l7tech.client.gui.dialogs.SsgPropertyDialog;
import com.l7tech.client.gui.util.IconManager;
import com.l7tech.proxy.Constants;

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
    private final SsgTableModel ssgTableModel;
    private final SsgFinder ssgFinder;
    private final int bindPort;
    private final boolean savePass;

    /**
     * Create an action that, when activated, launches the New Ssg and Ssg Properties dialogs.
     *
     * @param ssgListPanel    for selecting the newly-created Ssg
     * @param ssgTableModel   for actually creating a new Ssg instance
     * @param ssgFinder       SsgFinder for populating list of possible Trusted SSGs in New SSG dialog
     * @param bindPort        bind port for displaying URLs in Ssg property dialog
     * @param savePass        if true, "Save Password to disk" should be checked by default in newly-created Ssgs
     */
    public NewSsgAction(SsgListPanel ssgListPanel, SsgTableModel ssgTableModel, SsgFinder ssgFinder, int bindPort, boolean savePass) {
        super("New", IconManager.getAdd());
        this.ssgListPanel = ssgListPanel;
        this.ssgTableModel = ssgTableModel;
        this.ssgFinder = ssgFinder;
        this.bindPort = bindPort;
        this.savePass = savePass;
        putValue(Action.SHORT_DESCRIPTION, "Register a new Gateway with this " + Constants.APP_NAME);
        putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
    }

    public void actionPerformed(final ActionEvent e) {
        final Ssg newSsg = ssgTableModel.createSsg();
        if (savePass)
            newSsg.setSavePasswordToDisk(true);
        logger.info("Creating new Gateway Account " + newSsg);
        newSsg.getRuntime().getSsgKeyStoreManager().deleteStores();
        if (ssgTableModel.getRowCount() < 1)
            newSsg.setDefaultSsg(true);

        NewSsgDialog newSsgDialog = new NewSsgDialog(newSsg, ssgFinder, Gui.getInstance().getFrame(),
                                                     "Create Gateway Account", true);
        newSsgDialog.pack();
        Utilities.centerOnScreen(newSsgDialog);
        newSsgDialog.setVisible(true);
        if (newSsgDialog.getSsg() == null)
            return;

        final SsgPropertyDialog ssgPropertyDialog = SsgPropertyDialog.makeSsgPropertyDialog(newSsg, ssgTableModel.getSsgFinder(), bindPort);
        final boolean result = ssgPropertyDialog.runDialog();
        if (result) {
            ssgTableModel.addSsg(newSsg);
            ssgListPanel.selectSsg(newSsg);
        }
    }
}
