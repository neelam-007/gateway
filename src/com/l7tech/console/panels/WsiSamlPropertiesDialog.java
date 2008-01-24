package com.l7tech.console.panels;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;

import com.l7tech.policy.assertion.WsiSamlAssertion;
import com.l7tech.common.gui.util.RunOnChangeListener;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.PolicyListener;

/**
 * Property editing assertion for WS-I SAML Token Profile.
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsiSamlPropertiesDialog extends JDialog {

    //- PUBLIC

    public WsiSamlPropertiesDialog(WsiSamlAssertion assertion, Frame owner, boolean modal, boolean readOnly) throws HeadlessException {
        super(owner, "Configure WS-I SAML Token Profile properties", modal);
        this.wsiSamlAssertion = assertion;

        checkRequestMessagesCheckBox.setSelected(wsiSamlAssertion.isCheckRequestMessages());
        checkResponseMessagesCheckBox.setSelected(wsiSamlAssertion.isCheckResponseMessages());

        requestButtonGroup = new ButtonGroup();
        requestButtonGroup.add(reqAuditRadioButton);
        requestButtonGroup.add(reqAuditFailRadioButton);
        requestButtonGroup.add(reqFailRadioButton);
        reqAuditRadioButton.setSelected(true);
        if(wsiSamlAssertion.isAuditRequestNonCompliance()
        && wsiSamlAssertion.isFailOnNonCompliantRequest()) {
            reqAuditFailRadioButton.setSelected(true);
        }
        else if(wsiSamlAssertion.isFailOnNonCompliantRequest()) {
            reqFailRadioButton.setSelected(true);
        }

        responseButtonGroup = new ButtonGroup();
        responseButtonGroup.add(resAuditRadioButton);
        responseButtonGroup.add(resAuditFailRadioButton);
        responseButtonGroup.add(resFailRadioButton);
        resAuditRadioButton.setSelected(true);
        if(wsiSamlAssertion.isAuditResponseNonCompliance()
        && wsiSamlAssertion.isFailOnNonCompliantResponse()) {
            resAuditFailRadioButton.setSelected(true);
        }
        else if(wsiSamlAssertion.isFailOnNonCompliantResponse()) {
            resFailRadioButton.setSelected(true);
        }
        getContentPane().add(mainPanel);

        RunOnChangeListener rocl = new RunOnChangeListener(new Runnable(){
            public void run() {
                updateControls();
            }
        });
        checkRequestMessagesCheckBox.addChangeListener(rocl);
        checkResponseMessagesCheckBox.addChangeListener(rocl);

        okButton.setEnabled( !readOnly );
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean requestAudit = reqAuditRadioButton.isSelected() || reqAuditFailRadioButton.isSelected();
                boolean responseAudit = resAuditRadioButton.isSelected() || resAuditFailRadioButton.isSelected();
                boolean requestFail = reqAuditFailRadioButton.isSelected() || reqFailRadioButton.isSelected();
                boolean responseFail = resAuditFailRadioButton.isSelected() || resFailRadioButton.isSelected();

                wsiSamlAssertion.setCheckRequestMessages(checkRequestMessagesCheckBox.isSelected());
                wsiSamlAssertion.setCheckResponseMessages(checkResponseMessagesCheckBox.isSelected());
                wsiSamlAssertion.setAuditRequestNonCompliance(requestAudit);
                wsiSamlAssertion.setFailOnNonCompliantRequest(requestFail);
                wsiSamlAssertion.setAuditResponseNonCompliance(responseAudit);
                wsiSamlAssertion.setFailOnNonCompliantResponse(responseFail);

                assertionChanged = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wsiSamlAssertion = null;
                dispose();
            }
        });

        updateControls();
    }

    public boolean isAssertionChanged() {
        return assertionChanged;
    }

    //- PRIVATE

    private WsiSamlAssertion wsiSamlAssertion;
    private boolean assertionChanged = false;
    private PolicyListener listener;

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
