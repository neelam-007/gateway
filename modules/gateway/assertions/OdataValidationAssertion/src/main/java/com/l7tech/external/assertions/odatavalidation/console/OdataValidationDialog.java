package com.l7tech.external.assertions.odatavalidation.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.variable.Syntax;
import org.apache.commons.lang.BooleanUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.MessageFormat;
import java.util.*;

import static com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion.OdataMethod;
import static com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion.ProtectionActions;

/**
 * Copyright: CA Technologies, 2014
 * Date: 6/18/14
 *
 * @author ymoiseyenko
 */
public class OdataValidationDialog extends AssertionPropertiesOkCancelSupport<OdataValidationAssertion> {
    private static final ResourceBundle resources = ResourceBundle.getBundle(OdataValidationDialog.class.getName());

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

    private static final int AUTOMATIC_METHOD_INDEX = 0;
    private static final String AUTOMATIC_METHOD_LABEL = "<Automatic>";

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
    private JCheckBox validatePayload;
    private TargetVariablePanel metadataVariablePanel;
    private JComboBox<String> httpMethodComboBox;

    private InputValidator inputValidator;

    public OdataValidationDialog(final Window owner, final OdataValidationAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    protected void initComponents() {
        super.initComponents();

        metadataVariablePanel.setValueWillBeWritten(true);
        metadataVariablePanel.setAcceptEmpty(false);
        targetVariablePanel.setAllowContextVariable(false);

        // HTTP method combo box
        DefaultComboBoxModel<String> httpMethodComboBoxModel = new DefaultComboBoxModel<>();

        httpMethodComboBoxModel.insertElementAt(AUTOMATIC_METHOD_LABEL, AUTOMATIC_METHOD_INDEX);

        for (OdataMethod method : OdataMethod.values()) {
            httpMethodComboBoxModel.addElement(method.toString());
        }

        httpMethodComboBox.setModel(httpMethodComboBoxModel);

        httpMethodComboBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                httpMethodComboBox.getEditor().selectAll();
            }
        });

        // prefix for target context variables
        targetVariablePanel.setSuffixes(VARIABLE_SUFFIXES);
        targetVariablePanel.setAcceptEmpty(false);
        targetVariablePanel.setValueWillBeWritten(true);

        // create validation rules
        inputValidator = new InputValidator(this, getTitle());

        inputValidator.constrainTextFieldToBeNonEmpty(resources.getString("resourceProperty"), odataResourceUrl, null);
        
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (!metadataVariablePanel.isEntryValid()) {
                   return resources.getString("serviceMetadataInvalidErrMsg");
                }

                return null;
            }
        });

        // validate variable prefix
        inputValidator.addRule(new InputValidator.ComponentValidationRule(targetVariablePanel) {
            @Override
            public String getValidationError() {
                if (!targetVariablePanel.isEntryValid()) {
                    return resources.getString("variablePrefixInvalidErrMsg");
                }
                else if ((Syntax.getReferencedNames(targetVariablePanel.getSuffix())).length > 0) {
                    return resources.getString("variablePrefixContainsContextVariableErrMsg");
                }

                return null;
            }
        });

        // validate HTTP method
        inputValidator.addRule(new InputValidator.ComponentValidationRule(httpMethodComboBox) {
            @Override
            public String getValidationError() {
                String methodSelection = httpMethodComboBox.getSelectedItem().toString();

                if (methodSelection.isEmpty()) {
                    return resources.getString("httpMethodEmptyErrMsg");
                }

                // see if a context variable is present
                if (Syntax.getReferencedNames(methodSelection).length > 0) {
                    if (!Syntax.isOnlyASingleVariableReferenced(methodSelection)) {
                        return resources.getString("httpMethodNotOnlyOneVariableErrMsg");
                    }
                } else if (!AUTOMATIC_METHOD_LABEL.equals(methodSelection)) { // if not a context variable or "<Automatic>"
                    try {
                        OdataMethod.valueOf(methodSelection); // valid method
                    } catch (IllegalArgumentException e) {
                        return MessageFormat.format(resources.getString("httpMethodInvalidErrMsg"), methodSelection);
                    }
                }

                return null;
            }
        });
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

        String method = assertion.getHttpMethod();

        if (null == method) {
            httpMethodComboBox.setSelectedIndex(AUTOMATIC_METHOD_INDEX);
        } else {
            try {
                OdataMethod.valueOf(method); // if it's a valid method, it will already be in the combo box
                httpMethodComboBox.setSelectedItem(method);
            } catch (IllegalArgumentException e) {
                httpMethodComboBox.addItem(method); // need to add the item to the combo box
                httpMethodComboBox.setSelectedItem(method); // select the inserted item
            }
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
        final String error = inputValidator.validate();

        if (null != error) {
            throw new ValidationException(error);
        }

        // set HTTP method
        if (AUTOMATIC_METHOD_LABEL.equals(httpMethodComboBox.getSelectedItem().toString())) {
            assertion.setHttpMethod(null);
        } else {
            assertion.setHttpMethod(httpMethodComboBox.getSelectedItem().toString());
        }
        
        assertion.setResourceUrl(odataResourceUrl.getText());
        assertion.setVariablePrefix(targetVariablePanel.getVariable());
        assertion.setOdataMetadataSource(Syntax.SYNTAX_PREFIX + metadataVariablePanel.getVariable() + Syntax.SYNTAX_SUFFIX);
        assertion.setValidatePayload(validatePayload.isSelected());

        // set actions
        EnumSet<ProtectionActions> tempSet = EnumSet.noneOf(ProtectionActions.class);
        setAction(tempSet, metadataCheckBox, ProtectionActions.ALLOW_METADATA);
        setAction(tempSet, rawValueCheckBox, ProtectionActions.ALLOW_RAW_VALUE);
        assertion.setActions(tempSet);

        // set operations
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
}
