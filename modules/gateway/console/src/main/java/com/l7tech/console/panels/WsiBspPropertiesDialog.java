package com.l7tech.console.panels;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import com.l7tech.policy.assertion.WsiBspAssertion;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;

/**
 * Property editing assertion for WS-I BSP.
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsiBspPropertiesDialog extends LegacyAssertionPropertyDialog {

    //- PUBLIC

    public WsiBspPropertiesDialog(WsiBspAssertion assertion, Frame owner, boolean modal, boolean readOnly) throws HeadlessException {
        super(owner, assertion, modal);
        this.wsiBspAssertion = assertion;

        checkRequestMessagesCheckBox.setSelected(wsiBspAssertion.isCheckRequestMessages());
        checkResponseMessagesCheckBox.setSelected(wsiBspAssertion.isCheckResponseMessages());

        requestButtonGroup = new ButtonGroup();
        requestButtonGroup.add(reqAuditRadioButton);
        requestButtonGroup.add(reqAuditFailRadioButton);
        requestButtonGroup.add(reqFailRadioButton);
        reqAuditRadioButton.setSelected(true);
        if(wsiBspAssertion.isAuditRequestNonCompliance()
        && wsiBspAssertion.isFailOnNonCompliantRequest()) {
            reqAuditFailRadioButton.setSelected(true);
        }
        else if(wsiBspAssertion.isFailOnNonCompliantRequest()) {
            reqFailRadioButton.setSelected(true);
        }

        responseButtonGroup = new ButtonGroup();
        responseButtonGroup.add(resAuditRadioButton);
        responseButtonGroup.add(resAuditFailRadioButton);
        responseButtonGroup.add(resFailRadioButton);
        resAuditRadioButton.setSelected(true);
        if(wsiBspAssertion.isAuditResponseNonCompliance()
        && wsiBspAssertion.isFailOnNonCompliantResponse()) {
            resAuditFailRadioButton.setSelected(true);
        }
        else if(wsiBspAssertion.isFailOnNonCompliantResponse()) {
            resFailRadioButton.setSelected(true);
        }
        getContentPane().add(mainPanel);

        RunOnChangeListener rocl = new RunOnChangeListener(new Runnable(){
            @Override
            public void run() {
                updateControls();
            }
        });
        checkRequestMessagesCheckBox.addChangeListener(rocl);
        checkResponseMessagesCheckBox.addChangeListener(rocl);

        okButton.setEnabled( !readOnly );
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean requestAudit = reqAuditRadioButton.isSelected() || reqAuditFailRadioButton.isSelected();
                boolean responseAudit = resAuditRadioButton.isSelected() || resAuditFailRadioButton.isSelected();
                boolean requestFail = reqAuditFailRadioButton.isSelected() || reqFailRadioButton.isSelected();
                boolean responseFail = resAuditFailRadioButton.isSelected() || resFailRadioButton.isSelected();

                wsiBspAssertion.setCheckRequestMessages(checkRequestMessagesCheckBox.isSelected());
                wsiBspAssertion.setCheckResponseMessages(checkResponseMessagesCheckBox.isSelected());
                wsiBspAssertion.setAuditRequestNonCompliance(requestAudit);
                wsiBspAssertion.setFailOnNonCompliantRequest(requestFail);
                wsiBspAssertion.setAuditResponseNonCompliance(responseAudit);
                wsiBspAssertion.setFailOnNonCompliantResponse(responseFail);

                assertionChanged = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                wsiBspAssertion = null;
                dispose();
            }
        });

        updateControls();
    }

    public boolean isAssertionChanged() {
        return assertionChanged;
    }

    //- PRIVATE

    private WsiBspAssertion wsiBspAssertion;
    private boolean assertionChanged = false;

    private final ButtonGroup requestButtonGroup;
    private final ButtonGroup responseButtonGroup;
    private JPanel reqRadioPanel;
    private JPanel resRadioPanel;
    private JCheckBox checkRequestMessagesCheckBox;
    private JCheckBox checkResponseMessagesCheckBox;
    private JRadioButton reqAuditRadioButton;
    private JRadioButton reqAuditFailRadioButton;
    private JRadioButton reqFailRadioButton;
    private JRadioButton resAuditRadioButton;
    private JRadioButton resAuditFailRadioButton;
    private JRadioButton resFailRadioButton;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;

    /**
     *
     */
    private void updateControls() {
        Utilities.setEnabled(reqRadioPanel, checkRequestMessagesCheckBox.isSelected());
        Utilities.setEnabled(resRadioPanel, checkResponseMessagesCheckBox.isSelected());
    }
}
