package com.l7tech.external.assertions.saml2attributequery.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.external.assertions.saml2attributequery.Saml2AttributeQueryAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14-Jan-2009
 * Time: 10:04:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class Saml2AttributeQueryAttributeListPanel extends WizardStepPanel {
    private ButtonGroup radioButtonGroup;
    private JRadioButton whiteListRadioButton;
    private JRadioButton blackListRadioButton;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JList attributeList;
    private JPanel mainPanel;

    public Saml2AttributeQueryAttributeListPanel(WizardStepPanel nextStep, boolean readonly) {
        super(nextStep, readonly);

        initialize();
    }

    private void initialize() {
        radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(whiteListRadioButton);
        radioButtonGroup.add(blackListRadioButton);

        attributeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                String newAttributeName = JOptionPane.showInputDialog(
                        Saml2AttributeQueryAttributeListPanel.this,
                        "Attribute Name",
                        "Add Attribute",
                        JOptionPane.PLAIN_MESSAGE
                );

                if(newAttributeName != null) {
                    ((DefaultListModel)attributeList.getModel()).addElement(newAttributeName);
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int i = attributeList.getSelectedIndex();
                if(i == -1) {
                    return;
                }

                String value = (String)attributeList.getSelectedValue();

                value = JOptionPane.showInputDialog(
                        Saml2AttributeQueryAttributeListPanel.this,
                        "Attribute Name",
                        "Edit Attribute",
                        JOptionPane.PLAIN_MESSAGE
                );

                if(value != null) {
                    DefaultListModel model = (DefaultListModel)attributeList.getModel();
                    model.setElementAt(value, i);
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                int i = attributeList.getSelectedIndex();
                if(i == -1) {
                    return;
                }

                ((DefaultListModel)attributeList.getModel()).removeElementAt(i);
            }
        });

        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        if(!(settings instanceof Saml2AttributeQueryAssertion)) {
            throw new IllegalArgumentException();
        }

        Saml2AttributeQueryAssertion assertion = (Saml2AttributeQueryAssertion)settings;

        if(assertion.isWhiteList()) {
            whiteListRadioButton.setSelected(true);
        } else {
            blackListRadioButton.setSelected(true);
        }

        DefaultListModel model = new DefaultListModel();
        for(String attributeName : assertion.getRestrictedAttributeList()) {
            model.addElement(attributeName);
        }
        attributeList.setModel(model);
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        if(!(settings instanceof Saml2AttributeQueryAssertion)) {
            throw new IllegalArgumentException();
        }

        Saml2AttributeQueryAssertion assertion = (Saml2AttributeQueryAssertion)settings;

        if(whiteListRadioButton.isSelected()) {
            assertion.setWhiteList(true);
        } else if(blackListRadioButton.isSelected()) {
            assertion.setWhiteList(false);
        }

        ListModel model = attributeList.getModel();
        List<String> attributes = assertion.getRestrictedAttributeList();
        if(attributes == null) {
            attributes = new ArrayList<String>(model.getSize());
            assertion.setRestrictedAttributeList(attributes);
        } else {
            attributes.clear();
        }
        
        for(int i = 0;i < model.getSize();i++) {
            attributes.add((String)model.getElementAt(i));
        }
    }

    public String getStepLabel() {
        return "Attribute Restrictions";
    }
}
