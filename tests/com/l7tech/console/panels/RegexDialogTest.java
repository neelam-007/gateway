/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.Regex;

import javax.swing.*;

/**
 * @author emil
 * @version Mar 22, 2005
 */
public class RegexDialogTest {
    public static void main(String[] args)
      throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        Regex r = new Regex();
        r.setRegex("aaaa");
        r.setReplacement("bbbb");
        r.setEncoding("UTF-8");
        RegexDialog rd = new RegexDialog(null, r);
        rd.pack();
        rd.setVisible(true);

    }
}