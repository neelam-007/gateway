package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.external.assertions.xacmlpdp.XacmlRequestBuilderAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 27-Mar-2009
 * Time: 9:24:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class XacmlRequestBuilderMultiAttrOptionsDialog extends JDialog {
    private JPanel mainPanel;
    private JCheckBox isXPathExpressionCheckBox;
    private JCheckBox isRelativeToXPathCheckBox;
    private JButton okButton;
    private JButton cancelButton;

    private boolean confirmed = false;

    public XacmlRequestBuilderMultiAttrOptionsDialog(Frame owner, String propertyName, XacmlRequestBuilderAssertion.MultipleAttributeConfig.Field field) {
        super(owner, propertyName + " Options", true);
        initComponents(field);
    }

    public XacmlRequestBuilderMultiAttrOptionsDialog(Dialog owner, String propertyName, XacmlRequestBuilderAssertion.MultipleAttributeConfig.Field field) {
        super(owner, propertyName + " Options", true);
        initComponents(field);
    }

    private void initComponents(XacmlRequestBuilderAssertion.MultipleAttributeConfig.Field field) {
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                confirmed = true;
                XacmlRequestBuilderMultiAttrOptionsDialog.this.dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                XacmlRequestBuilderMultiAttrOptionsDialog.this.dispose();
            }
        });

        isXPathExpressionCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                isRelativeToXPathCheckBox.setEnabled(isXPathExpressionCheckBox.isSelected());
                if (! isXPathExpressionCheckBox.isSelected())
                    isRelativeToXPathCheckBox.setSelected(false); // can't be relative if it's not XPath
            }
        });
        isRelativeToXPathCheckBox.setEnabled(false);

        isXPathExpressionCheckBox.setSelected(field.getIsXpath());
        if(field.getIsXpath()) {
            isRelativeToXPathCheckBox.setEnabled(true);
            isRelativeToXPathCheckBox.setSelected(field.getIsRelative());
        }

        setContentPane(mainPanel);
        pack();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean getXpathIsExpression() {
        return isXPathExpressionCheckBox.isSelected();
    }

    public boolean getRelativeToXpath() {
        return isRelativeToXPathCheckBox.isSelected();
    }
}
