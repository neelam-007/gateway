package com.l7tech.external.assertions.odatavalidation.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion;
import com.l7tech.gui.util.InputValidator;
import org.apache.commons.lang.BooleanUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;
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
    private RSyntaxTextArea metadataSource;
    private JCheckBox validatePayload;
    
    private InputValidator inputValidator;

    public OdataValidationDialog(final Window owner, final OdataValidationAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    protected void initComponents() {
        super.initComponents();

        targetVariablePanel.setDefaultVariableOrPrefix(OdataValidationAssertion.DEFAULT_PREFIX);
        targetVariablePanel.setSuffixes(VARIABLE_SUFFIXES);
        targetVariablePanel.setAcceptEmpty(false);
        targetVariablePanel.setValueWillBeWritten(true);

        metadataSource.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);

        inputValidator = new InputValidator(this, getTitle());
        
        inputValidator.constrainTextFieldToBeNonEmpty(resourceUrlLabel.getText(), odataResourceUrl, null);
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
        metadataCheckBox.setSelected(BooleanUtils.
                toBoolean(availableActions.contains(ProtectionActions.ALLOW_METADATA)));
        rawValueCheckBox.setSelected(BooleanUtils.
                toBoolean(availableActions.contains(ProtectionActions.ALLOW_RAW_VALUE)));

        odataResourceUrl.setText(assertion.getResourceUrl());
        getMethodCheckBox.setSelected(assertion.isReadOperation());
        postMethodCheckBox.setSelected(assertion.isCreateOperation());
        putMethodCheckBox.setSelected(assertion.isUpdateOperation());
        patchMethodCheckBox.setSelected(assertion.isPartialUpdateOperation());
        mergeMethodCheckBox.setSelected(assertion.isMergeOperation());
        deleteMethodCheckBox.setSelected(assertion.isDeleteOperation());
        targetVariablePanel.setVariable(assertion.getVariablePrefix());
        validatePayload.setSelected(assertion.isValidatePayload());

        if (assertion.getOdataMetadataSource() != null)
            metadataSource.setText(assertion.getOdataMetadataSource());
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
        final String error = inputValidator.validate();

        if (null != error) {
            throw new ValidationException(error);
        }
        
        assertion.setResourceUrl(odataResourceUrl.getText());
        assertion.setVariablePrefix(targetVariablePanel.getVariable());
        assertion.setOdataMetadataSource(metadataSource.getText());
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
