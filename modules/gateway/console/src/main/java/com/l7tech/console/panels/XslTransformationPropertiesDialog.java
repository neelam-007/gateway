/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.assertion.AssertionResourceType;
import com.l7tech.policy.assertion.xml.XslTransformation;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.logging.Logger;


/**
 * A dialog to view / configure the properties of an {@link XslTransformation xslt assertion}.
 */
public class XslTransformationPropertiesDialog extends JDialog {
    private static final Logger log = Logger.getLogger(XslTransformationPropertiesDialog.class.getName());

    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JSpinner whichMimePartSpinner;
    private JLabel whichMimePartLabel;
    private JPanel borderPanel;
    private JPanel innerPanel;
    private JComboBox cbXslLocation;
    private TargetMessagePanel targetMessagePanel;
    private JPanel messageVariablePrefixTextFieldPanel;
    private TargetVariablePanel messageVariablePrefixTextField;

    private XslTransformation assertion;
    private final XslTransformationSpecifyPanel specifyPanel;
    private final RegexWhiteListPanel fetchPanel;
    private final MonitorUrlPanel specifyUrlPanel;
    private boolean wasoked = false;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.XslTransformationPropertiesDialog");

    // Combo box strings that also serve as mode identifiers
    private final String MODE_SPECIFY_XSL = resources.getString("specifyRadio.label");
    private final String MODE_SPECIFY_URL = resources.getString("fetchUrlRadio.label");
    private final String MODE_FETCH_PI_URL = resources.getString("fetchRadio.label");

    private final String BORDER_TITLE_PREFIX = resources.getString("xslLocationPrefix.text");

    ResourceBundle getResources() {
        return resources;
    }

    public XslTransformationPropertiesDialog(Frame owner, boolean modal, final boolean readOnly, XslTransformation assertion) {
        super(owner, resources.getString("window.title"), modal);

        if (assertion == null) throw new IllegalArgumentException("Xslt Transformation == null");

        this.assertion = assertion;

        Utilities.setEscKeyStrokeDisposes(this);

        targetMessagePanel.setTitle(null);
        targetMessagePanel.setBorder(null);
        targetMessagePanel.setModel(assertion);
        targetMessagePanel.setAllowNonMessageVariables(true);

        whichMimePartLabel.setLabelFor(whichMimePartSpinner);

        specifyPanel = new XslTransformationSpecifyPanel(this, assertion);
        fetchPanel = new RegexWhiteListPanel(this, assertion, getResources());
        specifyUrlPanel = new MonitorUrlPanel(assertion, getResources());

        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.add(specifyPanel);
        innerPanel.add(fetchPanel);
        innerPanel.add(specifyUrlPanel);

        String[] MODES = new String[]{
            MODE_SPECIFY_XSL,
            MODE_SPECIFY_URL,
            MODE_FETCH_PI_URL,
        };

        cbXslLocation.setModel(new DefaultComboBoxModel(MODES));
        cbXslLocation.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                updateModeComponents();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[] {
            fetchPanel.getAddButton(), fetchPanel.getEditButton(),
            fetchPanel.getRemoveButton(), specifyPanel.getFileButton(),
            specifyPanel.getUrlButton()
        });

        whichMimePartSpinner.setModel(new SpinnerNumberModel(0, 0, 9999, 1));
        InputValidator inputValidator = new InputValidator(this, resources.getString("window.title"));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(whichMimePartSpinner, "MIME part"));
        //noinspection UnnecessaryBoxing
        whichMimePartSpinner.setValue(new Integer(assertion.getWhichMimePart()));

        messageVariablePrefixTextField = new TargetVariablePanel();
        messageVariablePrefixTextFieldPanel.setLayout(new BorderLayout());
        messageVariablePrefixTextFieldPanel.add(messageVariablePrefixTextField, BorderLayout.CENTER);
        messageVariablePrefixTextField.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                okButton.setEnabled(messageVariablePrefixTextField.isEntryValid()&& !readOnly);
            }
        });
        messageVariablePrefixTextField.setVariable(assertion.getMsgVarPrefix());
        messageVariablePrefixTextField.setAssertion(assertion);
        messageVariablePrefixTextField.setSuffixes(new String[] {XslTransformation.VARIABLE_NAME});

        AssertionResourceInfo ri = assertion.getResourceInfo();
        AssertionResourceType rit = ri.getType();
        if (AssertionResourceType.MESSAGE_URL.equals(rit)) {
            cbXslLocation.setSelectedItem(MODE_FETCH_PI_URL);
        } else if (AssertionResourceType.SINGLE_URL.equals(rit)) {
            cbXslLocation.setSelectedItem(MODE_SPECIFY_URL);
        } else {
            if (!AssertionResourceType.STATIC.equals(rit)) log.warning("Unknown AssertionResourceType, assuming static: " + rit);
            cbXslLocation.setSelectedItem(MODE_SPECIFY_XSL);
        }
        updateModeComponents();

        // create callbacks
        okButton.setEnabled( !readOnly );
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        add(mainPanel);

        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xmlpad
    }


    private String getCurrentFetchMode() {
        return (String)cbXslLocation.getSelectedItem();
    }

    private void updateModeComponents() {
        String mode = getCurrentFetchMode();
        if (MODE_FETCH_PI_URL.equals(mode)) {
            fetchPanel.setVisible(true);
            specifyPanel.setVisible(false);
            specifyUrlPanel.setVisible(false);
        } else if (MODE_SPECIFY_URL.equals(mode)) {
            specifyUrlPanel.setVisible(true);
            specifyPanel.setVisible(false);
            fetchPanel.setVisible(false);
        } else {
            // Assume specify XSL
            if (!MODE_SPECIFY_XSL.equals(mode)) log.warning("Unexpected fetch mode, assuming specify: " + mode);
            specifyPanel.setVisible(true);
            fetchPanel.setVisible(false);
            specifyUrlPanel.setVisible(false);
        }
        Border border = borderPanel.getBorder();
        if (border instanceof TitledBorder) {
            TitledBorder tb = (TitledBorder)border;
            tb.setTitle(BORDER_TITLE_PREFIX + " " + mode);
        }
        innerPanel.revalidate();

        refreshDialog();
    }

    /**
     * Resize the dialog due to some components getting extended.
     */
    private void refreshDialog() {
        if (getSize().width < mainPanel.getMinimumSize().width) {
            setSize(mainPanel.getMinimumSize().width + 10, getSize().height);
        }
    }

    private void ok() {
        // validate the contents of the xml control
        String err;

        String mode = getCurrentFetchMode();
        if (MODE_FETCH_PI_URL.equals(mode)) {
            err = fetchPanel.check();
            if (err == null) fetchPanel.updateModel(assertion);
        } else if (MODE_SPECIFY_URL.equals(mode)) {
            err = specifyUrlPanel.check();
            if (err == null) specifyUrlPanel.updateModel(assertion);
        } else {
            // Assume specify XSL
            if (!MODE_SPECIFY_XSL.equals(mode)) log.warning("Unexpected fetch mode, assuming specify: " + mode);
            err = specifyPanel.check();
            if (err == null) specifyPanel.updateModel(assertion);
        }

        if (err == null)
            err = targetMessagePanel.check();

        if (err != null) {
            displayError(err, null);
            return;
        }

        targetMessagePanel.updateModel(assertion);

        assertion.setWhichMimePart(((Number)whichMimePartSpinner.getValue()).intValue());
        assertion.setMsgVarPrefix(messageVariablePrefixTextField.getVariable());

        wasoked = true;
        dispose();
    }

    private void cancel() {
        wasoked = false;
        dispose();
    }

    void displayError(String msg, String title) {
        if (title == null) title = resources.getString("error.window.title");
        JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        XslTransformationPropertiesDialog dlg = new XslTransformationPropertiesDialog(null, true, false, new XslTransformation());
        dlg.pack();
        dlg.setVisible(true);
        System.exit(0);
    }

    public boolean wasOKed() {
        return wasoked;
    }
}
