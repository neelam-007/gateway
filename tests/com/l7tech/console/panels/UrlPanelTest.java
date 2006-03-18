/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import javax.swing.*;

/**
 * @author alex
 */
public class UrlPanelTest {
    public static void main(String[] args) {
        OkCancelDialog dialog = new OkCancelDialog(new JFrame(), "Enter a damned Regex", true, new RegexPanel("Regex:", "you suck"));
        dialog.pack();
        dialog.setVisible(true);
        System.out.println(dialog.getValue());
    }
}
