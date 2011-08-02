package com.l7tech.external.assertions.jsontransformation.console;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.ResolveContextVariablesPanel;
import com.l7tech.console.panels.TargetMessagePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.jsontransformation.JsonTransformationAdmin;
import com.l7tech.external.assertions.jsontransformation.JsonTransformationAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import static com.l7tech.external.assertions.jsontransformation.JsonTransformationAssertion.Transformation.*;

public class JsonTransformationPropertiesDialog extends AssertionPropertiesOkCancelSupport<JsonTransformationAssertion> {

    private JPanel propertyPanel;
    private JTextField rootTagTextField;
    private JComboBox transformationComboBox;
    private JButton testButton;
    private JTextArea testOutputTextArea;
    private JTextArea testInputTextArea;
    private JPanel sourcePanelHolder;
    private JPanel destinationPanelHolder;
    private JSplitPane testSplitPane;
    private JTabbedPane tabPane;
    private TargetMessagePanel sourcePanel = new TargetMessagePanel() ;
    private TargetMessagePanel destinationPanel = new TargetMessagePanel();

    private ResourceBundle resourceBundle = ResourceBundle.getBundle(JsonTransformationPropertiesDialog.class.getName());
    private InputValidator validators;


    public JsonTransformationPropertiesDialog(final Window parent, final JsonTransformationAssertion assertion) {
        super(JsonTransformationAssertion.class, parent, assertion, true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        getCancelButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        testButton.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                doTest();
            }
        }));

        transformationComboBox.addItem(getPropertyValue("xml.json"));
        transformationComboBox.addItem(getPropertyValue("json.xml"));
        transformationComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rootTagTextField.setEnabled(transformationComboBox.getSelectedIndex()==1);
            }
        });

        sourcePanel.setAllowNonMessageVariables(false);
        sourcePanelHolder.setLayout(new BorderLayout());
        sourcePanelHolder.add(sourcePanel,BorderLayout.CENTER);
        destinationPanel.setAllowNonMessageVariables(false);
        destinationPanelHolder.setLayout(new BorderLayout());
        destinationPanelHolder.add(destinationPanel,BorderLayout.CENTER);

        tabPane.setTitleAt(0,getPropertyValue("source.dest.tab.title"));
        tabPane.setTitleAt(1,getPropertyValue("test.tab.title"));

        testSplitPane.setDividerLocation(0.5);
        testSplitPane.setResizeWeight(0.5);

        validators = new InputValidator( this, getTitle() );

        validators.addRule(new InputValidator.ComponentValidationRule(sourcePanel) {
            @Override
            public String getValidationError() {
                return sourcePanel.check();
            }
        });

        validators.addRule(new InputValidator.ComponentValidationRule(destinationPanel) {
            @Override
            public String getValidationError() {
                return destinationPanel.check();
            }
        });
    }

    private void doTest() {
        String rootTag, input;
        JsonTransformationAssertion.Transformation transformation;
        try{
            transformation = getTransformation();
            rootTag = rootTagTextField.getText();
            input = testInputTextArea.getText();
        } catch(ValidationException ex){
            testOutputTextArea.setText(ex.getMessage());
            return;
        }

        // resolve context vars
        String[] refTag = Syntax.getReferencedNames(rootTag);
        if(refTag.length > 0 ){
            Map<String, Object> contextVars = null;
            ResolveContextVariablesPanel dlg = new ResolveContextVariablesPanel(this,refTag);
            dlg.pack();
            Utilities.centerOnParentWindow(dlg);
            dlg.setVisible(true);
            if(!dlg.getWasOked())
                return;
            contextVars = dlg.getValues();
            rootTag = (String)contextVars.get(refTag[0]);
        }

        try {
            JsonTransformationAdmin admin = Registry.getDefault().getExtensionInterface(JsonTransformationAdmin.class, null);
            String strResponse = admin.testTransform(input, transformation, rootTag);
            if(transformation.equals(JsonTransformationAssertion.Transformation.JSON_to_XML))
            {
                strResponse = XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(strResponse));
            }
            testOutputTextArea.setText(strResponse);
        } catch (Exception e) {
            testOutputTextArea.setText( e.getMessage());
        }

    }

    @Override
    public JsonTransformationAssertion getData(JsonTransformationAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if(error != null){
            throw new ValidationException(error);
        }
        assertion.setRootTagString(rootTagTextField.getText());
        sourcePanel.updateModel(assertion);
        MessageTargetableSupport destTarget = assertion.getDestinationMessageTarget();
        destinationPanel.updateModel(destTarget);
        assertion.setDestinationMessageTarget(destTarget);

        assertion.setTransformation(getTransformation());
        return assertion;
    }

    private JsonTransformationAssertion.Transformation getTransformation() throws ValidationException{
        JsonTransformationAssertion.Transformation trans;
        switch (transformationComboBox.getSelectedIndex()){
            case 0: trans= XML_to_JSON; break;
            case 1: trans= JSON_to_XML; break;
            default: throw new ValidationException(getPropertyValue("invalid.transformation"));
        }
        return trans;
    }

    @Override
    public void setData(JsonTransformationAssertion assertion) {
        final String rootTag = assertion.getRootTagString();
        if(rootTag != null && !rootTag.trim().isEmpty()){
            rootTagTextField.setText(rootTag);
        }
        sourcePanel.setModel(assertion, getPreviousAssertion());
        destinationPanel.setModel(assertion.getDestinationMessageTarget(), assertion ,getPreviousAssertion(), true);

        switch (assertion.getTransformation()){
            case XML_to_JSON: transformationComboBox.setSelectedIndex(0);  break;
            case JSON_to_XML: transformationComboBox.setSelectedIndex(1); break;
            default: throw new ValidationException(getPropertyValue("invalid.transformation"));
        }
    }

    @Override
    protected JPanel createPropertyPanel() {
        return propertyPanel;
    }


    private String getPropertyValue(String propKey){
        String propertyName = resourceBundle.getString(propKey);
        if(propertyName.charAt(propertyName.length() - 1) == ':'){
            propertyName = propertyName.substring(0, propertyName.length() - 1);
        }
        return propertyName;
    }

}
