/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.console;

import com.l7tech.external.assertions.comparison.ComparisonAssertion;

import javax.swing.*;

/**
 * @author alex
 */
public class ComparisonAssertionDialogTest {
    public static void main(String[] args) throws Exception {
        ComparisonAssertion ca = new ComparisonAssertion();
        JFrame owner = new JFrame();
        owner.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ComparisonPropertiesDialog dialog = new ComparisonPropertiesDialog(owner, true, ca);
        dialog.pack();
        dialog.setVisible(true);
    }
}
