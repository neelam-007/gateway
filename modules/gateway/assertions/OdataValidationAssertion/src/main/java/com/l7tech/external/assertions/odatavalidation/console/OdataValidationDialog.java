package com.l7tech.external.assertions.odatavalidation.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.util.Pair;
import org.apache.commons.lang.BooleanUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Set;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 6/18/14
 */
public class OdataValidationDialog extends AssertionPropertiesOkCancelSupport<OdataValidationAssertion> {
    public enum ProtectionActions { ALLOW_METADATA, ALLOW_RAW_VALUE, ALLOW_OPEN_TYPE_ENTITY }

    private JPanel contentPanel;
    private JTextField metadataSourceTextField;
    private JCheckBox metadataCheckBox;
    private JCheckBox rawValueCheckBox;
    private JCheckBox openTypeEntityCheckBox;
    private JCheckBox getMethodCheckBox;
    private JCheckBox postMethodCheckBox;
    private JCheckBox putMethodCheckBox;
    private JCheckBox deleteMethodCheckBox;
    private JCheckBox mergeMethodCheckBox;
    private JCheckBox patchMethodCheckBox;
    private TargetVariablePanel targetVariablePanel1;
    private JLabel metadataSourceLabel;

    public OdataValidationDialog(final Frame parent, final OdataValidationAssertion assertion) {
        super(assertion.getClass(), parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
    }

    protected void initComponents() {
        super.initComponents();
        //TODO implement specific dialog initialization
    }

    /**
     * Configure the view with the data from the specified assertion bean.
     * This call should immediately configure all the editor widgets, before returning.
     *
     * @param assertion the assertion bean that provides that data that should be copied into the view.  Must not be null.
     */
    @Override
    public void setData(OdataValidationAssertion assertion) {
        metadataSourceTextField.setText(assertion.getOdataMetadataSource());
        Map<String, Object> availableActions = assertion.getAllActions();
        metadataCheckBox.setSelected(BooleanUtils.toBoolean((Boolean) availableActions.get(ProtectionActions.ALLOW_METADATA)));
        rawValueCheckBox.setSelected(BooleanUtils.toBoolean((Boolean) availableActions.get(ProtectionActions.ALLOW_RAW_VALUE)));
        openTypeEntityCheckBox.setSelected(BooleanUtils.toBoolean((Boolean) availableActions.get(ProtectionActions.ALLOW_OPEN_TYPE_ENTITY)));

        getMethodCheckBox.setSelected(assertion.isReadOperation());
        postMethodCheckBox.setSelected(assertion.isCreateOperation());
        putMethodCheckBox.setSelected(assertion.isUpdateOperation());
        patchMethodCheckBox.setSelected(assertion.isPartialUpdateOperation());
        mergeMethodCheckBox.setSelected(assertion.isMergeOperation());
        targetVariablePanel1.setDefaultVariableOrPrefix(assertion.getVariablePrefix());

    }

    /**
     * Copy the data out of the view into an assertion bean instance.
     * The provided bean should be filled and returned, if possible, but implementors may create and return
     * a new bean instead, if they must.
     *
     * @param assertion a bean to which the data from the view can be copied, if possible.  Must not be null.
     * @return a possibly-new assertion bean populated with data from the view.  Not necessarily the same bean that was passed in.
     * Never null.
     * @throws com.l7tech.console.panels.AssertionPropertiesOkCancelSupport.ValidationException if the data cannot be collected because of a validation error.
     */
    @Override
    public OdataValidationAssertion getData(OdataValidationAssertion assertion) throws ValidationException {
        assertion.setOdataMetadataSource(metadataSourceTextField.getText());
        assertion.setReadOperation(getMethodCheckBox.isSelected());
        assertion.setCreateOperation(postMethodCheckBox.isSelected());
        assertion.setUpdateOperation(putMethodCheckBox.isSelected());
        assertion.setPartialUpdateOperation(patchMethodCheckBox.isSelected());
        assertion.setMergeOperation(mergeMethodCheckBox.isSelected());
        assertion.setDeleteOperation(deleteMethodCheckBox.isSelected());
        assertion.setVariablePrefix(targetVariablePanel1.getVariable());
        return assertion;
    }

    /**
     * Create a panel to edit the properties of the assertion bean.  This panel does not include any
     * Ok or Cancel buttons.
     *
     * @return a panel that can be used to edit the assertion properties.  Never null.
     */
    @Override
    protected JPanel createPropertyPanel() {
        return contentPanel;
    }
}
