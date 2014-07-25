package com.l7tech.external.assertions.odatavalidation.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.variable.Syntax;
import org.apache.commons.lang.BooleanUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import static com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion.ProtectionActions;

/**
 * Copyright: CA Technologies, 2014
 * Date: 6/18/14
 *
 * @author ymoiseyenko
 */
public class OdataValidationDialog extends AssertionPropertiesOkCancelSupport<OdataValidationAssertion> {
    private static final Collection<String> VARIABLE_SUFFIXES = Collections.unmodifiableCollection(Arrays.asList(
            OdataValidationAssertion.QUERY_COUNT,
            OdataValidationAssertion.QUERY_CUSTOMOPTIONS,
            OdataValidationAssertion.QUERY_EXPAND,
            OdataValidationAssertion.QUERY_FILTER,
            OdataValidationAssertion.QUERY_FORMAT,
            OdataValidationAssertion.QUERY_INLINECOUNT,
            OdataValidationAssertion.QUERY_ORDERBY,
            OdataValidationAssertion.QUERY_PATHSEGMENTS,
            OdataValidationAssertion.QUERY_SELECT,
            OdataValidationAssertion.QUERY_SKIP,
            OdataValidationAssertion.QUERY_TOP));

    private JPanel contentPanel;
    private JCheckBox metadataCheckBox;
    private JCheckBox rawValueCheckBox;
    private JCheckBox getMethodCheckBox;
    private JCheckBox postMethodCheckBox;
    private JCheckBox putMethodCheckBox;
    private JCheckBox deleteMethodCheckBox;
    private JCheckBox mergeMethodCheckBox;
    private JCheckBox patchMethodCheckBox;
    private TargetVariablePanel targetVariablePanel;
    private JTextField odataResourceUrl;
    private JLabel resourceUrlLabel;
    private JCheckBox validatePayload;
    private TargetVariablePanel metadataVariablePanel;
    private JLabel serviceMetadataLabel;

    private InputValidator inputValidator;

    public OdataValidationDialog(final Window owner, final OdataValidationAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    protected void initComponents() {
        super.initComponents();

        metadataVariablePanel.setValueWillBeWritten(true);
        metadataVariablePanel.setAcceptEmpty(false);
/*        metadataVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                enableDisableComponents();
            }
        });*/

        targetVariablePanel.setDefaultVariableOrPrefix(OdataValidationAssertion.DEFAULT_PREFIX);
        targetVariablePanel.setSuffixes(VARIABLE_SUFFIXES);
        targetVariablePanel.setAcceptEmpty(false);
        targetVariablePanel.setValueWillBeWritten(true);


        inputValidator = new InputValidator(this, getTitle());
        
        inputValidator.constrainTextFieldToBeNonEmpty(resourceUrlLabel.getText(), odataResourceUrl, null);
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if(!metadataVariablePanel.isEntryValid()) {
                   return serviceMetadataLabel.getText() + " entry is invalid!";
                }
                return null;
            }
        });



        inputValidator.attachToButton(getOkButton(), super.createOkAction());
    }

    private void enableDisableComponents() {
        getOkButton().setEnabled(metadataVariablePanel.isEntryValid() & targetVariablePanel.isEntryValid());
    }

    /**
     * Configure the view with the data from the specified assertion bean.
     * This call should immediately configure all the editor widgets, before returning.
     *
     * @param assertion the assertion bean that provides that data that should be copied into the view. Must not be null.
     */
    @Override
    public void setData(OdataValidationAssertion assertion) {
        EnumSet<ProtectionActions> availableActions = assertion.getActions();

        if (null != assertion.getActions()) {
            metadataCheckBox.setSelected(BooleanUtils.
                    toBoolean(availableActions.contains(ProtectionActions.ALLOW_METADATA)));
            rawValueCheckBox.setSelected(BooleanUtils.
                    toBoolean(availableActions.contains(ProtectionActions.ALLOW_RAW_VALUE)));
        }

        odataResourceUrl.setText(assertion.getResourceUrl());
        getMethodCheckBox.setSelected(assertion.isReadOperation());
        postMethodCheckBox.setSelected(assertion.isCreateOperation());
        putMethodCheckBox.setSelected(assertion.isUpdateOperation());
        patchMethodCheckBox.setSelected(assertion.isPartialUpdateOperation());
        mergeMethodCheckBox.setSelected(assertion.isMergeOperation());
        deleteMethodCheckBox.setSelected(assertion.isDeleteOperation());
        targetVariablePanel.setVariable(assertion.getVariablePrefix());
        validatePayload.setSelected(assertion.isValidatePayload());

        if (assertion.getOdataMetadataSource() != null) {
            metadataVariablePanel.setVariable(assertion.getOdataMetadataSource());
        }
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
        assertion.setResourceUrl(odataResourceUrl.getText());
        assertion.setVariablePrefix(targetVariablePanel.getVariable());
        assertion.setOdataMetadataSource(Syntax.SYNTAX_PREFIX + metadataVariablePanel.getVariable() + Syntax.SYNTAX_SUFFIX);
        assertion.setValidatePayload(validatePayload.isSelected());

        //set actions
        EnumSet<ProtectionActions> tempSet = EnumSet.noneOf(ProtectionActions.class);
        setAction(tempSet, metadataCheckBox, ProtectionActions.ALLOW_METADATA);
        setAction(tempSet, rawValueCheckBox, ProtectionActions.ALLOW_RAW_VALUE);
        assertion.setActions(tempSet);

        //set operations
        assertion.setReadOperation(getMethodCheckBox.isSelected());
        assertion.setCreateOperation(postMethodCheckBox.isSelected());
        assertion.setUpdateOperation(putMethodCheckBox.isSelected());
        assertion.setPartialUpdateOperation(patchMethodCheckBox.isSelected());
        assertion.setMergeOperation(mergeMethodCheckBox.isSelected());
        assertion.setDeleteOperation(deleteMethodCheckBox.isSelected());

        return assertion;
    }

    private void setAction(EnumSet<ProtectionActions> enumSet, JCheckBox control, ProtectionActions action) {
        if (control.isSelected()) {
            enumSet.add(action);
        }
    }

    @Override
    protected ActionListener createOkAction() {
        // returns a no-op action so we can add our own Ok listener
        return new RunOnChangeListener();
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
