package com.l7tech.external.assertions.jsontransformation.console;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.ResolveContextVariablesPanel;
import com.l7tech.console.panels.TargetMessagePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.jsontransformation.JsonTransformationAdmin;
import com.l7tech.external.assertions.jsontransformation.JsonTransformationAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.variable.Syntax;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.ResourceBundle;

import static com.l7tech.external.assertions.jsontransformation.JsonTransformationAssertion.Transformation.JSON_to_XML;
import static com.l7tech.external.assertions.jsontransformation.JsonTransformationAssertion.Transformation.XML_to_JSON;

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
    private JComboBox transformationConvention;
    private JCheckBox prettyPrint;
    private JCheckBox convertAsJSONArrayCheckBox;
    private TargetMessagePanel sourcePanel = new TargetMessagePanel() ;
    private TargetMessagePanel destinationPanel = new TargetMessagePanel();

    private ResourceBundle resourceBundle = ResourceBundle.getBundle(JsonTransformationPropertiesDialog.class.getName());
    private InputValidator validators;


    public JsonTransformationPropertiesDialog(final Window parent, final JsonTransformationAssertion assertion) {
        super(JsonTransformationAssertion.class, parent, assertion, true);
        initComponents(parent);
    }

    private void initComponents(final Window parent) {
        super.initComponents();
        convertAsJSONArrayCheckBox.setEnabled(false);
        getCancelButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if(rootTagTextField.isEnabled()){
                    if(Syntax.getReferencedNames(rootTagTextField.getText()).length > 0){
                        JOptionPane.showMessageDialog(parent, "Cannot test using context variable as Root Tag name.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if(rootTagTextField.getText().trim().isEmpty()){
                        JOptionPane.showMessageDialog(parent, "Root Tag is required.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if(!JsonTransformationAssertion.ROOT_TAG_VERIFIER.matcher(rootTagTextField.getText()).matches()){
                        JOptionPane.showMessageDialog(parent, "Invalid Root Tag specified.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                doTest();
            }
        });

        transformationComboBox.addItem(getPropertyValue("xml.json"));
        transformationComboBox.addItem(getPropertyValue("json.xml"));
        transformationComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onTransformationTypeChange();
            }
        });

        transformationConvention.addItem(getPropertyValue("convention.standard"));
        transformationConvention.addItem(getPropertyValue("convention.jsonml"));
        transformationConvention.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onTransformationTypeChange();
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

        validators.addRule(new InputValidator.ComponentValidationRule(rootTagTextField) {
            @Override
            public String getValidationError() {
                if (rootTagTextField.isEnabled() && rootTagTextField.getText().trim().isEmpty()) {
                    return "Root Tag text is required";
                }
                return null;
            }
        });


    }

    private void doTest() {
        String rootTag, input;
        JsonTransformationAssertion.Transformation transformation;
        JsonTransformationAssertion.TransformationConvention convention;
        try{
            convention = getConvention();
            transformation = getTransformation();
            rootTag = rootTagTextField.getText();
            input = testInputTextArea.getText();
        } catch(ValidationException ex){
            testOutputTextArea.setText(ex.getMessage());
            return;
        }

        // resolve context vars
        String[] refTag = Syntax.getReferencedNames(rootTag);
        if(rootTagTextField.isEnabled() && refTag.length > 0 ){
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
        String strResponse = null;
        try {
            JsonTransformationAdmin admin = Registry.getDefault().getExtensionInterface(JsonTransformationAdmin.class, null);
            strResponse = admin.testTransform(input, transformation, convention, rootTag, prettyPrint.isSelected(), convertAsJSONArrayCheckBox.isSelected());
            if(transformation.equals(JsonTransformationAssertion.Transformation.JSON_to_XML))
            {
                Document document = XmlUtil.stringToDocument(strResponse);
                strResponse = prettyPrint.isSelected() ? XmlUtil.nodeToFormattedString(document) : XmlUtil.nodeToString(document);
            }
            testOutputTextArea.setText(strResponse);
        } catch (Exception e) {
            testOutputTextArea.setText("Converted XML is invalid.  " + e.getMessage());
        }
    }

    @Override
    public JsonTransformationAssertion getData(JsonTransformationAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if(error != null){
            throw new ValidationException(error);
        }
        if(getTransformation() == JsonTransformationAssertion.Transformation.JSON_to_XML &&
                getConvention() == JsonTransformationAssertion.TransformationConvention.STANDARD &&
                Syntax.getReferencedNames(rootTagTextField.getText()).length == 0 &&
                !JsonTransformationAssertion.ROOT_TAG_VERIFIER.matcher(rootTagTextField.getText()).matches()){
            throw new ValidationException("Invalid Root Tag specified.");
        }
        assertion.setRootTagString(rootTagTextField.getText());
        sourcePanel.updateModel(assertion);
        MessageTargetableSupport destTarget = assertion.getDestinationMessageTarget();
        destinationPanel.updateModel(destTarget);
        assertion.setDestinationMessageTarget(destTarget);

        assertion.setTransformation(getTransformation());
        assertion.setConvention(getConvention());
        assertion.setPrettyPrint(prettyPrint.isSelected());
        assertion.setArrayForm(convertAsJSONArrayCheckBox.isEnabled() && convertAsJSONArrayCheckBox.isSelected());
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

    private JsonTransformationAssertion.TransformationConvention getConvention() throws ValidationException{
        JsonTransformationAssertion.TransformationConvention convention;
        Object obj = transformationConvention.getSelectedItem();
        if("Standard".equals(obj)){
            convention = JsonTransformationAssertion.TransformationConvention.STANDARD;
        }
        else if("JSONML".equals(obj)){
            convention = JsonTransformationAssertion.TransformationConvention.JSONML;
        }
        else {
            throw new ValidationException(getPropertyValue("invalid.transformation.convention"));
        }
        return convention;
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

        String convention = "";
        switch (assertion.getConvention()){
            case STANDARD:
                convention = getPropertyValue("convention.standard");
                break;
            case JSONML:
                convention = getPropertyValue("convention.jsonml");
                break;
            default:
                throw new ValidationException(getPropertyValue("invalid.transformation.convention"));
        }
        transformationConvention.setSelectedItem(convention);
        prettyPrint.setSelected(assertion.isPrettyPrint());
        convertAsJSONArrayCheckBox.setSelected(assertion.isArrayForm());
        onTransformationTypeChange();
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

    private void onTransformationTypeChange() {
        Object target = transformationComboBox.getSelectedItem();
        Object convention = transformationConvention.getSelectedItem();
        //jsonml notation does not allow us to specify a root node in the converted xml output
        //we should disable this if it's the case.
        boolean enabled = "JSON To XML".equals(target) && "Standard".equals(convention);
        rootTagTextField.setEnabled(enabled);

        boolean enableArrayForm = "XML To JSON".equals(target) && "JSONML".equals(convention);
        convertAsJSONArrayCheckBox.setEnabled(enableArrayForm);
    }
}
