package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;

/**
 * The WSDL create overview panel. This is a support class for the
 * <i>WsdlCreateOverview.form</i>
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class WsdlCreateOverviewPanel extends WizardStepPanel {
    private JPanel mainPanel;


    public WsdlCreateOverviewPanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * @return the wizard step description
     */
    public String getDescription() {
        return "Service";
    }

    /**
     * Test whether the step is finished and it is safe to proceed to the next
     * one.
     * If the step is valid, the "Next" (or "Finish") button will be enabled.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean isValid() {
        return true;
    }

    /**
     * Test whether the step is finished and it is safe to finish the wizard.
     *
     * @return true if the panel is valid, false otherwis
     */

    public boolean canFinish() {
        return false;
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Overview";
    }

    {
// do not edit this generated initializer!!! do not add your code here!!!
        $$$setupUI$$$();
    }

    /** generated code, do not edit or call this method manually !!! */
    private void $$$setupUI$$$() {
        JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(14, 4, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _3;
        _3 = new JLabel();
        _3.setText("Overview");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _4;
        _4 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(12, 2, 1, 1, 0, 2, 1, 6, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _5;
        _5 = new JLabel();
        _5.setText("You will be asked to provide:");
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _6;
        _6 = new JLabel();
        _6.setText("This WSDL creation wizard will guide you through the steps required to create the Web Service.");
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, 8, 1, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _7;
        _7 = new JLabel();
        _7.setText("- service definition");
        _2.add(_7, new com.intellij.uiDesigner.core.GridConstraints(6, 2, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _8;
        _8 = new JLabel();
        _8.setText("- operations, operations messages and message parts involved");
        _2.add(_8, new com.intellij.uiDesigner.core.GridConstraints(8, 2, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _9;
        _9 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_9, new com.intellij.uiDesigner.core.GridConstraints(7, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        JLabel _10;
        _10 = new JLabel();
        _10.setText("");
        _2.add(_10, new com.intellij.uiDesigner.core.GridConstraints(11, 2, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _11;
        _11 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_11, new com.intellij.uiDesigner.core.GridConstraints(9, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        com.intellij.uiDesigner.core.Spacer _12;
        _12 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_12, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        com.intellij.uiDesigner.core.Spacer _13;
        _13 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_13, new com.intellij.uiDesigner.core.GridConstraints(3, 2, 1, 1, 0, 2, 1, 0, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10)));
        com.intellij.uiDesigner.core.Spacer _14;
        _14 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_14, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, 0, 1, 6, 1, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _15;
        _15 = new JLabel();
        _15.setText("- service endpoint address and SOAPAction operation attributes");
        _2.add(_15, new com.intellij.uiDesigner.core.GridConstraints(10, 2, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
    }

}
