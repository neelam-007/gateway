/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks;

import com.l7tech.common.gui.widgets.SquigglyTextField;

import javax.swing.JFrame;

/**
 * @author alex
 * @version $Revision$
 */
public class TestSquiggly extends JFrame {
    public TestSquiggly() {
        super( "Test squiggly" );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        SquigglyTextField text = new SquigglyTextField(80);
        getContentPane().add(text);
        pack();
        show();
    }

    public static void main(String[] args) {
        TestSquiggly me = new TestSquiggly();
    }
}
