package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.RequestSizeLimit;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 29, 2005
 * Time: 3:29:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class RequestSizeLimitDialogTest {
    public static void main(String[] args) {
        JFrame parentFrame = new JFrame();
        parentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        parentFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        RequestSizeLimit assertion = new RequestSizeLimit();
        assertion.setLimit(2048);
        assertion.setEntireMessage(false);
        System.out.println("[BEFORE] Limit = " + assertion.getLimit());
        System.out.println("[BEFORE] Apply to whole message = " + assertion.isEntireMessage());


        RequestSizeLimitDialog dlg = new RequestSizeLimitDialog(parentFrame, assertion, true, false);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);

        System.out.println("[AFTER] Limit = " + assertion.getLimit());
        System.out.println("[AFTER] Apply to whole message = " + assertion.isEntireMessage());

        System.exit(0);
    }
}

