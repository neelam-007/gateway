/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.util;

import com.l7tech.common.gui.widgets.SquigglyTextField;

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

/*
        Dimension size = _textField.getSize();
        size.height = size.height + 10;
        _textField.setSize(size);
*/

        TextComponentPauseListenerManager.registerPauseListener(
                _textField, this, 1000 );

        Container cp = getContentPane();
        final GridBagLayout layout = new GridBagLayout();
        layout.rowHeights = new int[] {30};
        cp.setLayout(layout);
        cp.add( _textField, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.VERTICAL,new Insets(0,0,0,0), 0, 0) );
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
                _textField.setNone();
                statusLabel.setText("OK");
            } catch ( IOException e ) {
                _textField.setSquiggly();
                _textField.setAll();
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
    private SquigglyTextField _textField = new SquigglyTextField(80);
    private JLabel _label = new JLabel("initial value");
    private JLabel statusLabel = new JLabel(" ");
}
