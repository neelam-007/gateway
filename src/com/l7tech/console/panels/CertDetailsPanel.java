package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class CertDetailsPanel extends WizardStepPanel{
    private JPanel mainPanel;

     public CertDetailsPanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "View Certificate Details";
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        final JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, 0, 3, 3, 3, null, null, null));
        final JLabel _3;
        _3 = new JLabel();
        _3.setText("Certificate Details");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _4;
        _4 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 2, 1, 6, new Dimension(-1, 10), new Dimension(-1, 10), null));
        final com.intellij.uiDesigner.core.Spacer _5;
        _5 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 2, 1, 6, new Dimension(-1, 10), new Dimension(-1, 10), null));
        final JScrollPane _6;
        _6 = new JScrollPane();
        _1.add(_6, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 0, 3, 7, 7, null, null, null));
        final JTable _7;
        _7 = new JTable();
        _6.setViewportView(_7);
        final com.intellij.uiDesigner.core.Spacer _8;
        _8 = new com.intellij.uiDesigner.core.Spacer();
        _1.add(_8, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 2, 0, 2, 1, 6, new Dimension(-1, 10), new Dimension(-1, 10), null));
    }

}
