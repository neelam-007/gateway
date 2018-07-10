package com.l7tech.external.assertions.evaluatejsonpathexpression.console;


import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.FindException;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetMessagePanel;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.evaluatejsonpathexpression.EvaluateJsonPathExpressionAdmin;
import com.l7tech.external.assertions.evaluatejsonpathexpression.EvaluateJsonPathExpressionAssertion;
import com.l7tech.external.assertions.evaluatejsonpathexpression.JsonPathEvaluator;
import com.l7tech.external.assertions.evaluatejsonpathexpression.JsonPathExpressionResult;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

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
    private static final Logger logger = Logger.getLogger(EvaluateJsonPathExpressionPropertiesDialog.class.getName());

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
        targetVariablePrefix.setAssertion(assertion, getPreviousAssertion());
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

        //As we set it to JsonPath default value, in future if other evaluators are supported we will enable the drop down for the same.
        cbEvaluator.setEnabled(false);
        testButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    if(Syntax.getReferencedNames(textFieldExpression.getText()).length > 0){
                        showErrorDialog();
                        return;
                    }
                    final EvaluateJsonPathExpressionAdmin admin = Registry.getDefault().getExtensionInterface(EvaluateJsonPathExpressionAdmin.class, null);
                    final JsonPathExpressionResult result = admin.testEvaluation(
                            JsonPathEvaluator.valueOf(cbEvaluator.getSelectedItem().toString(),isJsonCompressionEnabled()),
                            testInputArea.getText().trim(), textFieldExpression.getText().trim());
                    StringBuilder sb = new StringBuilder("found = ").append(result.isFound()).append("\r\n")
                            .append("count = ").append(result.getCount());
                    List<String> results = result.getResults();
                    if (result.isFound()) {
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
                } catch (EvaluateJsonPathExpressionAdmin.EvaluateJsonPathExpressionTestException e1) {
                    testOutputArea.setText(e1.getMessage());
                }
            }
        });
    }

    private void showErrorDialog(){
        DialogDisplayer.showMessageDialog(this,
                "Cannot test using context variable as Expression value.",
                "Error",
                JOptionPane.ERROR_MESSAGE, null);
    }

    private void toggleButtonState() {
        getOkButton().setEnabled(
                sourcePanel.isValidTarget() &&
                        targetVariablePrefix.isEntryValid() &&
                        !textFieldExpression.getText().trim().isEmpty()
        );
        testButton.setEnabled(!textFieldExpression.getText().trim().isEmpty() && !testInputArea.getText().trim().isEmpty());
    }


    private boolean isJsonCompressionEnabled(){
        boolean withCompression = false;
        try {
            ClusterProperty clusterProperty = Registry.getDefault().getClusterStatusAdmin().findPropertyByName("json.evalJsonPathWithCompression");
            if (clusterProperty != null) {
                withCompression = Boolean.parseBoolean(clusterProperty.getValue());
            }
        } catch (FindException e) {
            logger.warning("could not retrieve Cluster Wide Property: \"json.evalJsonPathWithCompression\", therefore setting \"json.evalJsonPathWithCompression\" as default value i.e FALSE" + e.getMessage());
            withCompression = false;
        }
        return withCompression;
    }
}
