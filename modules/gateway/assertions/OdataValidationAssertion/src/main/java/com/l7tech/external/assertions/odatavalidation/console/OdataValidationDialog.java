package com.l7tech.external.assertions.odatavalidation.console;

import com.japisoft.xmlpad.XMLContainer;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.XMLContainerFactory;
import com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.AssertionMetadata;
import org.apache.commons.lang.BooleanUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

/**
 * Copyright: CA Technologies, 2014
 * Date: 6/18/14
 *
 * @author ymoiseyenko
 */
public class OdataValidationDialog extends AssertionPropertiesOkCancelSupport<OdataValidationAssertion> {

    private OdataValidationAssertion assertion;
    private JPanel contentPanel;
//    private JTextField metadataSourceTextField;
    private JCheckBox metadataCheckBox;
    private JCheckBox rawValueCheckBox;
    private JCheckBox openTypeEntityCheckBox;
    private JCheckBox getMethodCheckBox;
    private JCheckBox postMethodCheckBox;
    private JCheckBox putMethodCheckBox;
    private JCheckBox deleteMethodCheckBox;
    private JCheckBox mergeMethodCheckBox;
    private JCheckBox patchMethodCheckBox;
    private TargetVariablePanel targetVariablePanel;
    private JLabel metadataSourceLabel;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField odataResourcetUrl;
    private JPanel xmlPanel;
    private JLabel resourceUrlLabel;
    private XMLContainer xmlContainer;


    private InputValidator inputValidator;

    public OdataValidationDialog(final Frame parent, final OdataValidationAssertion assertion) {
        super(assertion.getClass(), parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        this.assertion = assertion;
        initComponents();
    }

    protected void initComponents() {
        super.initComponents();
        //TODO implement specific dialog initialization
        targetVariablePanel.setDefaultVariableOrPrefix("odata");
        targetVariablePanel.setSuffixes(new ArrayList<String>(Arrays.asList("one", "two", "three")));
        setContentPane(contentPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);
        xmlContainer = XMLContainerFactory.createXmlContainer(true);
        xmlPanel.removeAll();
        xmlPanel.setLayout(new BorderLayout(0,5));
        xmlPanel.add(xmlContainer.getView(),BorderLayout.CENTER);

        inputValidator = new InputValidator(this,"Something - in property file");
        // inputValidator.constrainTextFieldToBeNonEmpty("Meta data source",xmlContainer,null);
        inputValidator.constrainTextFieldToBeNonEmpty(resourceUrlLabel.getText(), odataResourcetUrl, null);

        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();

            }
        });
    }

    private void doCancel() {

        this.dispose();
    }

    private void doOk() {

        getData(assertion);

        this.dispose();
    }

    /**
     * Configure the view with the data from the specified assertion bean.
     * This call should immediately configure all the editor widgets, before returning.
     *
     * @param assertion the assertion bean that provides that data that should be copied into the view. Must not be null.
     */
    @Override
    public void setData(OdataValidationAssertion assertion) {
        // metadataSourceTextField.setText(assertion.getOdataMetadataSource());
        odataResourcetUrl.setText(assertion.getResourceUrl());
        Set<OdataValidationAssertion.ProtectionActions> availableActions = assertion.getAllActions();
        metadataCheckBox.setSelected(BooleanUtils.toBoolean((Boolean) availableActions.contains(OdataValidationAssertion.ProtectionActions.ALLOW_METADATA)));
        rawValueCheckBox.setSelected(BooleanUtils.toBoolean((Boolean) availableActions.contains(OdataValidationAssertion.ProtectionActions.ALLOW_RAW_VALUE)));
        openTypeEntityCheckBox.setSelected(BooleanUtils.toBoolean((Boolean) availableActions.contains(OdataValidationAssertion.ProtectionActions.ALLOW_OPEN_TYPE_ENTITY)));
        getMethodCheckBox.setSelected(assertion.isReadOperation());
        postMethodCheckBox.setSelected(assertion.isCreateOperation());
        putMethodCheckBox.setSelected(assertion.isUpdateOperation());
        patchMethodCheckBox.setSelected(assertion.isPartialUpdateOperation());
        mergeMethodCheckBox.setSelected(assertion.isMergeOperation());
        targetVariablePanel.setVariable(assertion.getVariablePrefix());
        if ( assertion.getOdataMetadataSource() != null)
          xmlContainer.getUIAccessibility().getEditor().setText(assertion.getOdataMetadataSource());
    }

    /**
     * Copy the data out of the view into an assertion bean instance.
     *
     * @param assertion a bean to which the data from the view can be copied, if possible. Must not be null.
     * @return a possibly-new assertion bean populated with data from the view.  Not necessarily the same bean that was passed in. Never null.
     * @throws com.l7tech.console.panels.AssertionPropertiesOkCancelSupport.ValidationException if the data cannot be collected because of a validation error.
     */
    @Override
    public OdataValidationAssertion getData(OdataValidationAssertion assertion) throws ValidationException {
        // assertion.setOdataMetadataSource(metadataSourceTextField.getText());
        assertion.setResourceUrl(odataResourcetUrl.getText());
        if ( metadataCheckBox.isSelected() )
            assertion.addAction(OdataValidationAssertion.ProtectionActions.ALLOW_METADATA);
        if ( rawValueCheckBox.isSelected() )
            assertion.addAction(OdataValidationAssertion.ProtectionActions.ALLOW_RAW_VALUE);
        if ( openTypeEntityCheckBox.isSelected() )
            assertion.addAction(OdataValidationAssertion.ProtectionActions.ALLOW_OPEN_TYPE_ENTITY);
        assertion.setReadOperation(getMethodCheckBox.isSelected());
        assertion.setCreateOperation(postMethodCheckBox.isSelected());
        assertion.setUpdateOperation(putMethodCheckBox.isSelected());
        assertion.setPartialUpdateOperation(patchMethodCheckBox.isSelected());
        assertion.setMergeOperation(mergeMethodCheckBox.isSelected());
        assertion.setDeleteOperation(deleteMethodCheckBox.isSelected());
        assertion.setVariablePrefix(targetVariablePanel.getVariable());
        assertion.setOdataMetadataSource(xmlContainer.getUIAccessibility().getEditor().getText());
        setConfirmed(true);
        return assertion;
    }

    /**
     * Create a panel to edit the properties of the assertion bean.  This panel does not include any
     * Ok or Cancel buttons.
     *
     * @return a panel that can be used to edit the assertion properties. Never null.
     */
    @Override
    protected JPanel createPropertyPanel() {
        return contentPanel;
    }


    /*
     * Validating Field:
     *
     * odataServiceRootURL - non-empty, URL or contains a context-variable reference
     *
     * metadataSourceTexField - non-empty, context-variable naming a string/message?
     *
     * targetVariablePanel1 - non-empty
     *
     * HTTP Method check boxes - at least one set
     *
     * hide data check boxes (metadata, rawdata) -
     *
     */
}
