/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.client.gui.dialogs;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.client.gui.dialogs.NewSsgDialog;

import javax.swing.*;

/**
 * @author mike
 */
public class NewSsgDialogTest {
    private static void setLnf() throws Exception {
        UIManager.LookAndFeelInfo[] feels = UIManager.getInstalledLookAndFeels();
        for ( int i = 0; i < feels.length; i++ ) {
            UIManager.LookAndFeelInfo feel = feels[i];
            if ( feel.getName().indexOf( "indows" ) >= 0 ) {
                UIManager.setLookAndFeel( feel.getClassName() );
                break;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        setLnf();
        NewSsgDialog dlg = new NewSsgDialog(new Ssg(1), null/*new SsgManagerStub()*/, new JFrame("Application"), "Blah!", true);
        dlg.pack();
        dlg.setVisible(true);
        System.exit(0);
    }

}
