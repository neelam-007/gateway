/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * @author alex
 */
public class UrlPanelTest {
    public static void main(String[] args) {
        UrlPanel p = new UrlPanel("URL:", "you suck");
        p.setBackground(Color.RED);
        OkCancelDialog dialog = new OkCancelDialog(new JFrame(), "Enter a damned URL", true, p);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
        System.out.println(dialog.getValue());
    }
}
