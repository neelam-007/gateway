/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.client.gui.dialogs;

import com.l7tech.proxy.datamodel.Ssg;

import javax.swing.*;

import org.junit.Ignore;

/**
 * @author mike
 */
@Ignore
public class NewSsgDialogTest {
    private static void setLnf() throws Exception {
        UIManager.LookAndFeelInfo[] feels = UIManager.getInstalledLookAndFeels();
        for ( UIManager.LookAndFeelInfo feel : feels ) {
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
