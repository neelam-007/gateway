/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.util;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

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
        pack();
        show();
    }

    public static void main(String[] args) {
        TestPauseListener me = new TestPauseListener();
    }

    public void textEntryPaused( JTextComponent component, long msecs ) {
        _label.setText( "paused" );
    }

    public void textEntryResumed(JTextComponent component) {
        _label.setText( "resumed" );
    }

    private JTextField _textField = new JTextField(80);
    private JLabel _label = new JLabel("initial value");
}
