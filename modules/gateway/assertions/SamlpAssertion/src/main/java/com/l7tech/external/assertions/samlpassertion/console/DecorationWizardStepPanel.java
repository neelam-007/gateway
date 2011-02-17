package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.TargetMessagePanel;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Nov 7, 2008
 * Time: 3:10:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class DecorationWizardStepPanel extends SamlpWizardStepPanel {
    private JPanel mainPanel;
    private JCheckBox decorateMessage;
    private JCheckBox includeSignature;
    private JPanel targetMessagePanelHolder;
    private TargetMessagePanel targetMessagePanel = new TargetMessagePanel();

    public DecorationWizardStepPanel(WizardStepPanel next, Assertion prevAssertion) {
        super(next, AssertionMode.RESPONSE, prevAssertion);
        initialize();
    }

    private void initialize() {
        targetMessagePanelHolder.add( targetMessagePanel );
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    public String getStepLabel() {
        return "Target Message Decoration";
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        super.readSettings(settings);
        SamlProtocolAssertion assertion = (SamlProtocolAssertion) settings;
        targetMessagePanel.setModel(assertion,getPreviousAssertion());
    }
}
