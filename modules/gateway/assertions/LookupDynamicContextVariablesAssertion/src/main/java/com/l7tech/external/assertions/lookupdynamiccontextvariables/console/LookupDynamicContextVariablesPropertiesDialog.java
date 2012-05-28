package com.l7tech.external.assertions.lookupdynamiccontextvariables.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.lookupdynamiccontextvariables.LookupDynamicContextVariablesAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.variable.DataType;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;

/**
 * <p>The properties dialog for configuring the {@link LookupDynamicContextVariablesAssertion}</p>.
 *
 * @author KDiep
 */
public class LookupDynamicContextVariablesPropertiesDialog extends AssertionPropertiesOkCancelSupport<LookupDynamicContextVariablesAssertion> {

    private JPanel contentPane;
    private JTextField sourceVariable;
    private TargetVariablePanel targetVariablePanel;
    private JComboBox dataTypeDropDown;

    /**
     * Constructs a new dialog box.
     * @param parent the parent window/container.
     * @param assertion the {@link LookupDynamicContextVariablesAssertion} to hold the configured value.
     */
    public LookupDynamicContextVariablesPropertiesDialog(final Window parent, final LookupDynamicContextVariablesAssertion assertion) {
        super(LookupDynamicContextVariablesAssertion.class, parent, assertion, true);
        initComponents();
    }

    @Override
    public void setData(final LookupDynamicContextVariablesAssertion assertion) {
        sourceVariable.setText(assertion.getSourceVariable());
        sourceVariable.setCaretPosition(0);
        targetVariablePanel.setVariable(assertion.getTargetOutputVariable());
        targetVariablePanel.setAssertion(assertion, getPreviousAssertion());
        DataType target = assertion.getTargetDataType();
        if(target == null){
            target = DataType.STRING;
        }
        dataTypeDropDown.setSelectedItem(target);
    }

    @Override
    public LookupDynamicContextVariablesAssertion getData(final LookupDynamicContextVariablesAssertion assertion) throws ValidationException {
        assertion.setSourceVariable(sourceVariable.getText().trim());
        assertion.setTargetOutputVariable(targetVariablePanel.getVariable());
        assertion.setTargetDataType((DataType) dataTypeDropDown.getSelectedItem());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                toggleOkButtonState();
            }
        });
        sourceVariable.getDocument().addDocumentListener(changeListener);
        targetVariablePanel.addChangeListener(changeListener);

        dataTypeDropDown.setModel( new DefaultComboBoxModel(LookupDynamicContextVariablesAssertion.SUPPORTED_TYPES));
        dataTypeDropDown.setRenderer( new TextListCellRenderer<DataType>( new Functions.Unary<String,DataType>(){
            @Override
            public String call( final DataType dataType ) {
                return dataType.getName();
            }
        } ) );
    }

    private void toggleOkButtonState() {
        final boolean enabled = !sourceVariable.getText().isEmpty() && targetVariablePanel.isEntryValid();
        this.getOkButton().setEnabled(enabled);
    }

}
