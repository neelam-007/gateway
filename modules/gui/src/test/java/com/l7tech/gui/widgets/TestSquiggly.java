/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.gui.widgets;

import javax.swing.JFrame;
import java.awt.Color;

import org.junit.Ignore;

/**
 * @author alex
 * @version $Revision$
 */
@Ignore
public class TestSquiggly extends JFrame {
    public TestSquiggly() {
        super( "Test squiggly" );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        SquigglyTextField text = new SquigglyTextField(80);
//        text.setRange(5, SquigglyTextField.ALL);
        text.setRange(5,10);
        text.setColor( Color.RED);
        text.setSquiggly();
//        text.setStraight();
        getContentPane().add(text);
        pack();
        setVisible(true);
    }

    public static void main(String[] args) {
        TestSquiggly me = new TestSquiggly();
    }
}
