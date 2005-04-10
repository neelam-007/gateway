/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.HttpFormPost;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;

/**
 * @author emil
 * @version Mar 22, 2005
 */
public class HttpFormPostDialogTest {
    public static void main(String[] args)
      throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        HttpFormPost assertion = new HttpFormPost();
        HttpFormPost.FieldInfo fi1 = new HttpFormPost.FieldInfo("foo", "text/plain");
        HttpFormPost.FieldInfo fi2 = new HttpFormPost.FieldInfo("bar", "text/xml");
        HttpFormPost.FieldInfo fi3 = new HttpFormPost.FieldInfo("baz", "application/octet-stream");

        assertion.setFieldInfos(new HttpFormPost.FieldInfo[] {fi1, fi2, fi3} );
        HttpFormPostDialog rd = new HttpFormPostDialog(null, assertion);
        rd.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        rd.pack();
        Utilities.centerOnScreen(rd);
        rd.setVisible(true);

    }
}