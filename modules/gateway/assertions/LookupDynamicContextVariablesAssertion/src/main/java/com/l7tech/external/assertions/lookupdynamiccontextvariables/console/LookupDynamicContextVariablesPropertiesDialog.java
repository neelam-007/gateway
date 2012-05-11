package com.l7tech.external.assertions.lookupdynamiccontextvariables.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.lookupdynamiccontextvariables.LookupDynamicContextVariablesAssertion;
import com.l7tech.gui.util.RunOnChangeListener;

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
        targetVariablePanel.setVariable(assertion.getTargetOutputVariable());
    }

    @Override
    public LookupDynamicContextVariablesAssertion getData(final LookupDynamicContextVariablesAssertion assertion) throws ValidationException {
        assertion.setSourceVariable(sourceVariable.getText().trim());
        assertion.setTargetOutputVariable(targetVariablePanel.getVariable());
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
    }

    private void toggleOkButtonState() {
        final boolean enabled = !sourceVariable.getText().isEmpty() && targetVariablePanel.isEntryValid();
        this.getOkButton().setEnabled(enabled);
    }

}
