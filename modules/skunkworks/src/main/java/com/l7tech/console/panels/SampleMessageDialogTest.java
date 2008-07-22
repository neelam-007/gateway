/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.service.SampleMessage;

import javax.swing.*;
import java.awt.*;

/**
 * @author emil
 * @version Mar 22, 2005
 */
public class SampleMessageDialogTest {
    public static void main(String[] args)
      throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        SampleMessage sm = new SampleMessage(1234, "foo", "getQuote", "<xml/>");
        SampleMessageDialog smd = new SampleMessageDialog((Dialog)null, sm, true);
        smd.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        smd.pack();
        Utilities.centerOnScreen(smd);
        Utilities.setEscKeyStrokeDisposes(smd);
        smd.setVisible(true);
    }
}