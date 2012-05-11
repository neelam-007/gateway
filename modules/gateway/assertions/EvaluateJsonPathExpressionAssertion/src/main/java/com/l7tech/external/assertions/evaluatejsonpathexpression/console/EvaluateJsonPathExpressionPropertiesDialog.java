package com.l7tech.external.assertions.evaluatejsonpathexpression.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetMessagePanel;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.evaluatejsonpathexpression.EvaluateJsonPathExpressionAdmin;
import com.l7tech.external.assertions.evaluatejsonpathexpression.EvaluateJsonPathExpressionAssertion;
import com.l7tech.external.assertions.evaluatejsonpathexpression.JsonPathEvaluator;
import com.l7tech.external.assertions.evaluatejsonpathexpression.JsonPathExpressionResult;
import com.l7tech.gui.util.RunOnChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * <p>A dialog to present the configuration screen to the user for the {@link EvaluateJsonPathExpressionAssertion}</p>
 */
public class EvaluateJsonPathExpressionPropertiesDialog extends AssertionPropertiesOkCancelSupport<EvaluateJsonPathExpressionAssertion> {

    private static final String DIALOG_TITLE = "Evaluate JSON Path Expression Properties";
    private JPanel contentPane;
    private JTextField textFieldExpression;

    private JLabel labelExpression;
    private JTabbedPane tabbedPane;
    private JPanel sourceAndDestinationPane;
    private JPanel testPane;
    private JPanel destinationPanelHolder;

    private TargetVariablePanel targetVariablePrefix;
    private JPanel sourcePanelHolder;
    private JButton testButton;
    private JTextArea testOutputArea;
    private JTextArea testInputArea;
    private JComboBox cbEvaluator;
    private TargetMessagePanel sourcePanel = new TargetMessagePanel();

    public EvaluateJsonPathExpressionPropertiesDialog(final Window owner, final EvaluateJsonPathExpressionAssertion assertion) {
        super(EvaluateJsonPathExpressionAssertion.class, owner, DIALOG_TITLE, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData(final EvaluateJsonPathExpressionAssertion assertion) {
        sourcePanel.setModel(assertion, getPreviousAssertion());
        textFieldExpression.setText(assertion.getExpression());

        targetVariablePrefix.setDefaultVariableOrPrefix(EvaluateJsonPathExpressionAssertion.VARIABLE_PREFIX);
        targetVariablePrefix.setSuffixes( EvaluateJsonPathExpressionAssertion.getVariableSuffixes() );
        targetVariablePrefix.setVariable(assertion.getVariablePrefix());
        cbEvaluator.setSelectedItem(assertion.getEvaluator());
    }

    @Override
    public EvaluateJsonPathExpressionAssertion getData(final EvaluateJsonPathExpressionAssertion assertion) throws ValidationException {
        sourcePanel.updateModel(assertion);
        assertion.setExpression(textFieldExpression.getText().trim());
        assertion.setVariablePrefix(targetVariablePrefix.getVariable());
        assertion.setEvaluator(cbEvaluator.getSelectedItem().toString());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        sourcePanel.setAllowNonMessageVariables(false);
        sourcePanelHolder.setLayout(new BorderLayout());
        sourcePanelHolder.add(sourcePanel, BorderLayout.CENTER);
        RunOnChangeListener buttonStateUpdateListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                toggleButtonState();
            }
        });
        sourcePanel.addDocumentListener(buttonStateUpdateListener);
        targetVariablePrefix.addChangeListener(buttonStateUpdateListener);
        textFieldExpression.getDocument().addDocumentListener(buttonStateUpdateListener);
        testInputArea.getDocument().addDocumentListener(buttonStateUpdateListener);
        for(JsonPathEvaluator s : EvaluateJsonPathExpressionAssertion.getSupportedEvaluator()){
            cbEvaluator.addItem(s.name());
        }
        cbEvaluator.setEnabled(cbEvaluator.getItemCount() > 1);
        testButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    final EvaluateJsonPathExpressionAdmin admin = Registry.getDefault().getExtensionInterface(EvaluateJsonPathExpressionAdmin.class, null);
                    final JsonPathExpressionResult result = admin.testEvaluation(
                            JsonPathEvaluator.valueOf(cbEvaluator.getSelectedItem().toString()), 
                            testInputArea.getText().trim(), textFieldExpression.getText().trim());
                    StringBuilder sb = new StringBuilder("found = ").append(result.isFound()).append("\r\n")
                            .append("count = ").append(result.getCount()).append("\r\n\r\nResult(s):\r\n");
                    List<String> results = result.getResults();
                    for(int i = 0; i < results.size(); i++){
                        sb.append((i + 1)).append(": ").append(results.get(i)).append("\r\n");
                    }
                    sb = sb.delete(sb.length() - 2, sb.length());
                    testOutputArea.setText(sb.toString());
                } catch (EvaluateJsonPathExpressionAdmin.EvaluateJsonPathExpressionTestException e1) {
                    testOutputArea.setText(e1.getMessage());
                }
            }
        });
    }

    private void toggleButtonState() {
        getOkButton().setEnabled(
                sourcePanel.isValidTarget() &&
                        targetVariablePrefix.isEntryValid() &&
                        !textFieldExpression.getText().trim().isEmpty()
        );
        testButton.setEnabled(!textFieldExpression.getText().trim().isEmpty() && !testInputArea.getText().trim().isEmpty());
    }
}
