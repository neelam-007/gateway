package com.l7tech.console.panels;

import com.l7tech.policy.exporter.CustomAssertionReference;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;

/**
 * This panel allows an administrator to take the appropriate action when a policy
 * being imported contains custom assertions whose type is not installed on the
 * target system.
 *
 * Because custom assertions cannot be installed through the ssm, the
 * supported actions are simply "ignore" or "delete".
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 28, 2004<br/>
 * $Id$<br/>
 */
public class ResolveForeignCustomAssertionPanel extends WizardStepPanel {
    private CustomAssertionReference foreignRef;
    private JPanel mainPanel;
    private JTextField assNameTxtField;
    private JRadioButton removeRadio;
    private JRadioButton ignoreRadio;

    public ResolveForeignCustomAssertionPanel(WizardStepPanel next, CustomAssertionReference foreignRef) {
        super(next);
        this.foreignRef = foreignRef;
        initialize();
    }

    public String getDescription() {
        return getStepLabel();
    }

    public String getStepLabel() {
        return "Unknown assertion type " + foreignRef.getCustomAssertionName();
    }

    public boolean canFinish() {
        if (hasNextPanel()) return false;
        return true;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        // show details (not much there)
        assNameTxtField.setText(foreignRef.getCustomAssertionName());

        // group the radios
        ButtonGroup actionGroup = new ButtonGroup();
        actionGroup.add(removeRadio);
        actionGroup.add(ignoreRadio);
        removeRadio.setSelected(true);
    }

    public boolean onNextButton() {
        // collect actions details and store in the reference for resolution
        if (removeRadio.isSelected()) {
            foreignRef.setLocalizeDelete();
        } else if (ignoreRadio.isSelected()) {
            foreignRef.setLocalizeIgnore();
        }
        return true;
    }

}
