/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.util;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author alex
 * @version $Revision$
 */
public class TestPauseListener extends JFrame implements PauseListener {
    public TestPauseListener() {
        super( "Test Pause Listener" );
        super.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        TextComponentPauseListenerManager.registerPauseListener(
                _textField, this, 1000 );

        Container cp = getContentPane();
        cp.setLayout(new GridBagLayout());
        cp.add( _textField );
        cp.add( _label );
        cp.add(statusLabel,new GridBagConstraints(0,1,2,1,0,0,GridBagConstraints.NORTH,0,new Insets(0,0,0,0),0,0));
        pack();
        show();
    }

    public static void main(String[] args) {
        TestPauseListener me = new TestPauseListener();
    }

    public void textEntryPaused( JTextComponent component, long msecs ) {
        _label.setText( "paused" );
        final String value = component.getText();
        if (value != null && value.length() > 0 && !value.equals(oldvalue)) {
            InputStream is = null;
            try {
                URL url = new URL(value);
                is = url.openStream();
                statusLabel.setText("OK");
            } catch ( IOException e ) {
                statusLabel.setText(e.toString());
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        oldvalue = value;
    }

    public void textEntryResumed(JTextComponent component) {
        _label.setText( "resumed" );
    }

    private String oldvalue = null;
    private JTextField _textField = new JTextField(80);
    private JLabel _label = new JLabel("initial value");
    private JLabel statusLabel = new JLabel(" ");
}
