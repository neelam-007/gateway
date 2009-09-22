/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.gui.widgets.OkCancelDialog;

import javax.swing.*;
import java.awt.*;

import org.junit.Ignore;

/**
 * @author alex
 */
@Ignore
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
