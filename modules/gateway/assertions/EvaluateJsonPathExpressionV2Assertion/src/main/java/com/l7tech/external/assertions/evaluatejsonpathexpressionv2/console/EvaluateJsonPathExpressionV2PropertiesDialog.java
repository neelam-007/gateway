package com.l7tech.external.assertions.evaluatejsonpathexpressionv2.console;


import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetMessagePanel;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.EvaluateJsonPathExpressionV2Admin;
import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.EvaluateJsonPathExpressionV2Assertion;
import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.Evaluator;
import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.JsonPathEvaluator;
import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.JsonPathExpressionResult;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * <p>A dialog to present the configuration screen to the user for the {@link EvaluateJsonPathExpressionV2Assertion}</p>
 */
public class EvaluateJsonPathExpressionV2PropertiesDialog extends AssertionPropertiesOkCancelSupport<EvaluateJsonPathExpressionV2Assertion> {

    private JPanel contentPane;
    private JTextField textFieldExpression;

    private TargetVariablePanel targetVariablePrefix;
    private JPanel sourcePanelHolder;
    private JButton testButton;
    private JTextArea testOutputArea;
    private JTextArea testInputArea;
    private JComboBox cbEvaluator;
    private TargetMessagePanel sourcePanel = new TargetMessagePanel();
    private static final Logger logger = Logger.getLogger(EvaluateJsonPathExpressionV2PropertiesDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle( EvaluateJsonPathExpressionV2PropertiesDialog.class.getName() );
    private static final String DIALOG_TITLE = resources.getString("dialog.title");

    public EvaluateJsonPathExpressionV2PropertiesDialog(final Window owner, final EvaluateJsonPathExpressionV2Assertion assertion) {
        super(EvaluateJsonPathExpressionV2Assertion.class, owner, DIALOG_TITLE, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData(final EvaluateJsonPathExpressionV2Assertion assertion) {
        sourcePanel.setModel(assertion, getPreviousAssertion());
        textFieldExpression.setText(assertion.getExpression());

        targetVariablePrefix.setDefaultVariableOrPrefix(EvaluateJsonPathExpressionV2Assertion.VARIABLE_PREFIX);
        targetVariablePrefix.setSuffixes( EvaluateJsonPathExpressionV2Assertion.getVariableSuffixes() );
        targetVariablePrefix.setVariable(assertion.getVariablePrefix());
        targetVariablePrefix.setAssertion(assertion, getPreviousAssertion());
        cbEvaluator.setSelectedItem(assertion.getEvaluator());
    }

    @Override
    public EvaluateJsonPathExpressionV2Assertion getData(final EvaluateJsonPathExpressionV2Assertion assertion) throws ValidationException {
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

        cbEvaluator.addItem(EvaluateJsonPathExpressionV2Assertion.DEFAULT_EVALUATOR);
        for(String evaluator : EvaluateJsonPathExpressionV2Assertion.getSupportedEvaluators()){
            cbEvaluator.addItem(evaluator);
        }

        testButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    if(Syntax.getReferencedNames(textFieldExpression.getText()).length > 0){
                        showErrorDialog();
                        return;
                    }

                    String evaluator = EvaluateJsonPathExpressionV2Assertion.DEFAULT_EVALUATOR.equals(cbEvaluator.getSelectedItem().toString()) ? getJsonSystemDefaultValue() : cbEvaluator.getSelectedItem().toString();
                    if (EvaluateJsonPathExpressionV2Assertion.getSupportedEvaluators().contains(evaluator)) {
                        final EvaluateJsonPathExpressionV2Admin admin = Registry.getDefault().getExtensionInterface(EvaluateJsonPathExpressionV2Admin.class, null);
                        final JsonPathExpressionResult result = admin.testEvaluation(evaluator, testInputArea.getText().trim(), textFieldExpression.getText().trim());

                        StringBuilder sb = new StringBuilder("found = ").append(result.isFound()).append("\r\n")
                                .append("count = ").append(result.getCount());
                        if (result.isFound()) {
                            List<String> results = result.getResults();
                            sb.append("\r\n\r\nResult(s):\r\n");
                            if (results == null || results.size() == 0) {
                                sb.append("[]");
                            } else {
                                for (int i = 0; i < results.size(); i++) {
                                    sb.append((i + 1)).append(": ").append(results.get(i)).append("\r\n");
                                }
                                sb = sb.delete(sb.length() - 2, sb.length());
                            }
                        }
                        testOutputArea.setText(sb.toString());
                    } else {
                        testOutputArea.setText("Invalid Evaluator : " + evaluator);
                    }
                } catch (EvaluateJsonPathExpressionV2Admin.EvaluateJsonPathExpressionTestException e1) {
                    testOutputArea.setText(e1.getMessage());
                }
            }
        });
    }

    private void showErrorDialog(){
        DialogDisplayer.showMessageDialog(this,
                resources.getString("error.test.context.variable"),
                "Error",
                JOptionPane.ERROR_MESSAGE, null);
    }

    private void toggleButtonState() {
        getOkButton().setEnabled(sourcePanel.isValidTarget() && targetVariablePrefix.isEntryValid() && !textFieldExpression.getText().trim().isEmpty());
        testButton.setEnabled(!textFieldExpression.getText().trim().isEmpty() && !testInputArea.getText().trim().isEmpty());
    }

    private String getJsonSystemDefaultValue(){
        try {
            ClusterProperty clusterProperty = Registry.getDefault().getClusterStatusAdmin().findPropertyByName(EvaluateJsonPathExpressionV2Assertion.PARAM_JSON_SYSTEM_DEFAULT_EVALUATOR);
            if (clusterProperty != null) {
                return clusterProperty.getValue();
            } else {
                logger.warning("null value retrieved for Cluster Wide Property: \"json.systemDefaultEvaluator\", therefore setting \"json.json.systemDefaultEvaluator\" with default value i.e JsonPath");
                return EvaluateJsonPathExpressionV2Assertion.JSONPATH_EVALUATOR;
            }
        } catch (FindException e) {
            logger.warning("could not retrieve Cluster Wide Property: \"json.systemDefaultEvaluator\", therefore setting \"json.json.systemDefaultEvaluator\" with default value i.e JsonPath" + e.getMessage());
            return EvaluateJsonPathExpressionV2Assertion.JSONPATH_EVALUATOR;
        }
    }
}
