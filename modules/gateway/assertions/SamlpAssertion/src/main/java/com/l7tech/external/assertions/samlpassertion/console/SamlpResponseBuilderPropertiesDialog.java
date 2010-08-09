package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.samlpassertion.SamlStatus;
import com.l7tech.external.assertions.samlpassertion.SamlpResponseBuilderAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ResourceBundle;

import static com.l7tech.external.assertions.samlpassertion.SamlpResponseBuilderAssertion.SamlVersion.*;

public class SamlpResponseBuilderPropertiesDialog extends AssertionPropertiesOkCancelSupport<SamlpResponseBuilderAssertion> {

    public SamlpResponseBuilderPropertiesDialog(final Window parent,
                                                final SamlpResponseBuilderAssertion assertion) {
        super(SamlpResponseBuilderAssertion.class, parent, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        samlVersionComboBox.setModel(new DefaultComboBoxModel(new SamlpResponseBuilderAssertion.SamlVersion[]{SAML2, SAML1_1}));

        final RunOnChangeListener versionListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                updateComponentsForVersion();
            }
        });

        samlVersionComboBox.addActionListener(versionListener);

        statusCodeComboBox.setRenderer( new TextListCellRenderer<SamlStatus>( new Functions.Unary<String,SamlStatus>(){
            @Override
            public String call( final SamlStatus dataType ) {
                return dataType.getValue();
            }
        } ) );


        responseAttributesTabbedPane.remove(saml1_1Panel);
        responseAttributesTabbedPane.remove(saml2_0Panel);

        tabHolder.removeAll();
        tabHolder.setLayout(new BoxLayout(tabHolder, BoxLayout.Y_AXIS));
        tabHolder.add(saml1_1Panel);
        tabHolder.add(saml2_0Panel);

        final FocusAdapter focusAdapter = new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                final Object source = e.getSource();
                if (!(source instanceof JTextField)) return;
                final JTextField sourceField = (JTextField) source;

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if(sourceField.getText().trim().isEmpty()){
                            sourceField.setText(autoString);
                        }
                    }
                });
            }
        };
        
        idTextField.addFocusListener(focusAdapter);
        responseIdTextField.addFocusListener(focusAdapter);
        issueInstantTextField.addFocusListener(focusAdapter);
        issueInstant1_1TextField.addFocusListener(focusAdapter);
        
        updateComponentsForVersion();
    }

    @Override
    public void setData(SamlpResponseBuilderAssertion assertion) {

        switch (assertion.getSamlVersion()) {
            case SAML2:
                samlVersionComboBox.setSelectedItem(SAML2);
                break;
            case SAML1_1:
                samlVersionComboBox.setSelectedItem(SAML1_1);
                break;
            default:
                throw new RuntimeException("Unknown SAML version");
        }

        signResponseCheckBox.setSelected(assertion.isSignResponse());
        addIssuerCheckBox.setSelected(assertion.isAddIssuer());
        statusMessageTextField.setText(assertion.getStatusMessage());
        statusDetailTextField.setText(assertion.getStatusDetail());

        final String responseId = assertion.getResponseId();
        final String responseIdText = (responseId == null || responseId.trim().isEmpty()) ? autoString: responseId;

        final String issueInstant = assertion.getIssueInstant();
        final String issueInstantText = (issueInstant == null || issueInstant.trim().isEmpty()) ? autoString : issueInstant;

        idTextField.setText(responseIdText);
        issueInstantTextField.setText(issueInstantText);

        responseIdTextField.setText(responseIdText);
        issueInstant1_1TextField.setText(issueInstantText);

        final String inResponseTo = assertion.getInResponseTo();
        if(inResponseTo != null && !inResponseTo.trim().isEmpty()){
            inResponseToTextField.setText(inResponseTo);
            inResponseTo1_1TextField.setText(inResponseTo);
        }

        final String destination = assertion.getDestination();
        if(destination != null && !destination.trim().isEmpty()) destinationTextField.setText(destination);

        final String consent = assertion.getConsent();
        if(consent != null && !consent.trim().isEmpty()) consentTextField.setText(consent);

        final String responseExtensions = assertion.getResponseExtensions();
        if(responseExtensions != null && !responseExtensions.trim().isEmpty())
            extensionsTextField.setText(responseExtensions);

        final String recipient = assertion.getRecipient();
        if(recipient != null && !recipient.trim().isEmpty()) recipientTextField.setText(recipient);

        final String responseAssertions = assertion.getResponseAssertions();
        if(responseAssertions != null && !responseAssertions.trim().isEmpty()) assertionsTextField.setText(responseAssertions);

        updateComponentsForVersion();
        statusCodeComboBox.setSelectedItem(assertion.getSamlStatus());
    }
    
    @Override
    public SamlpResponseBuilderAssertion getData(SamlpResponseBuilderAssertion assertion) throws ValidationException {
        validateData();
        final SamlpResponseBuilderAssertion.SamlVersion samlVersion = (SamlpResponseBuilderAssertion.SamlVersion) samlVersionComboBox.getSelectedItem();
        assertion.setSamlVersion(samlVersion);
        assertion.setSignResponse(signResponseCheckBox.isSelected());
        assertion.setAddIssuer(addIssuerCheckBox.isSelected());
        assertion.setSamlStatus((SamlStatus) statusCodeComboBox.getSelectedItem());
        assertion.setStatusMessage(statusMessageTextField.getText());
        assertion.setStatusDetail(statusDetailTextField.getText());

        final String responseId;
        final String issueInstant;
        final String inResponseTo;
        switch (samlVersion){
            case SAML2:
                responseId = idTextField.getText().trim();
                issueInstant = issueInstantTextField.getText().trim();
                inResponseTo = inResponseToTextField.getText().trim();

                final String destination = destinationTextField.getText().trim();
                assertion.setDestination(destination.isEmpty() ? null: destination);
                final String consent = consentTextField.getText().trim();
                assertion.setConsent((consent.isEmpty()) ? null : consent);

                final String extensions = extensionsTextField.getText().trim();
                assertion.setResponseExtensions((extensions.isEmpty()) ? null : extensions);
                break;
            case SAML1_1:
                responseId = responseIdTextField.getText();
                issueInstant = issueInstant1_1TextField.getText().trim();
                inResponseTo = inResponseTo1_1TextField.getText().trim();

                final String recipient = recipientTextField.getText();
                assertion.setRecipient((recipient.isEmpty()) ? null : recipient);
                break;
            default:
                throw new RuntimeException("Unknown SAML Version");//programming error
        }

        assertion.setResponseId(isNullOrEmptyOrAuto(autoString, responseId)? null: responseId);
        assertion.setIssueInstant(isNullOrEmptyOrAuto(autoString, issueInstant) ? null : issueInstant);
        assertion.setInResponseTo(isNullOrEmptyOrAuto(null, inResponseTo) ? null: inResponseTo);

        final String responseAssertions = assertionsTextField.getText().trim();
        assertion.setResponseAssertions((responseAssertions.isEmpty()) ? null: responseAssertions);

        return assertion;
    }

    // - PRIVATE

    private boolean isNullOrEmptyOrAuto(final String equal, final String test){
        return test == null || test.trim().isEmpty() || test.trim().equals(equal);
    }

    private void validateData() {

        //Check status code against assertion selection
        final String respAssertions = assertionsTextField.getText().trim();
        final SamlStatus samlStatus = (SamlStatus) statusCodeComboBox.getSelectedItem();
        final boolean isSuccess = samlStatus.equals(SamlStatus.SAML2_SUCCESS) || samlStatus.equals(SamlStatus.SAML_SUCCESS);

        if(respAssertions.isEmpty() && isSuccess){
            throw new ValidationException("If no assertions are entered the status cannot be success.");
        } else if (!respAssertions.isEmpty() && !isSuccess){
            // status must be success
            throw new ValidationException("If status is not success then no assertions can be entered.");
        }

        if(!respAssertions.isEmpty() && !Syntax.validateStringOnlyReferencesVariables(respAssertions)){
            throw new ValidationException(resources.getString("responseElements.assertions") +
                    " may only reference context variables");
        }

        final SamlpResponseBuilderAssertion.SamlVersion samlVersion = 
                (SamlpResponseBuilderAssertion.SamlVersion) samlVersionComboBox.getSelectedItem();
            switch(samlVersion){
                case SAML2:
                    final String extensions = extensionsTextField.getText().trim();
                    if(!extensions.isEmpty() && !Syntax.validateStringOnlyReferencesVariables(extensions)){
                        throw new ValidationException(resources.getString("responseElements.extensions.2_0_only") +
                                " may only reference context variables");
                    }
                    break;
            }


    }

    private void updateComponentsForVersion(){

        final Object item = samlVersionComboBox.getSelectedItem();
        final boolean saml2 = item == null || (item).equals(SAML2);
        if (saml2) {
            saml2_0Panel.setVisible(true);
            saml1_1Panel.setVisible(false);
            statusCodeComboBox.setModel(new DefaultComboBoxModel(SamlStatus.getSaml2xStatuses().toArray()));
            addIssuerCheckBox.setVisible(true);
        } else {
            saml2_0Panel.setVisible(false);
            saml1_1Panel.setVisible(true);
            statusCodeComboBox.setModel(new DefaultComboBoxModel(SamlStatus.getSaml1xStatuses().toArray()));
            addIssuerCheckBox.setVisible(false);
        }
    }

    public static void main(String [] args){
        SamlpResponseBuilderAssertion assertion = new SamlpResponseBuilderAssertion();
        SamlpResponseBuilderPropertiesDialog dialog = new SamlpResponseBuilderPropertiesDialog(null, assertion);

        dialog.pack();
        dialog.show();

    }


    private JPanel contentPane;
    private JPanel samlVersionPanel;
    private JComboBox samlVersionComboBox;
    private JCheckBox signResponseCheckBox;
    private JPanel responseStatusPanel;
    private JComboBox statusCodeComboBox;
    private JTextField statusMessageTextField;
    private JTextField statusDetailTextField;
    private JTabbedPane responseAttributesTabbedPane;
    private JTextField idTextField;
    private JTextField issueInstantTextField;
    private JTextField inResponseToTextField;
    private JTextField destinationTextField;
    private JTextField consentTextField;
    private JPanel responseElementsPanel;
    private JTextField assertionsTextField;
    private JTextField extensionsTextField;
    private JTextField responseIdTextField;
    private JTextField issueInstant1_1TextField;
    private JTextField inResponseTo1_1TextField;
    private JTextField recipientTextField;
    private JPanel saml2_0Panel;
    private JPanel saml1_1Panel;
    private JPanel tabHolder;
    private JCheckBox addIssuerCheckBox;

    private static final String autoString = "<<auto>>";
    private static final ResourceBundle resources = ResourceBundle.getBundle( SamlpResponseBuilderPropertiesDialog.class.getName() );
}
