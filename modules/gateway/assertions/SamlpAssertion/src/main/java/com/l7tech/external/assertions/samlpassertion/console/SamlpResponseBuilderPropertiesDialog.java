package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.saml.SamlIssuerPanel;
import com.l7tech.external.assertions.samlpassertion.SamlStatus;
import com.l7tech.external.assertions.samlpassertion.SamlVersion;
import com.l7tech.external.assertions.samlpassertion.SamlpResponseBuilderAssertion;
import com.l7tech.gui.util.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.*;

import static com.l7tech.external.assertions.samlpassertion.SamlVersion.*;

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

        samlIssuerPanel = new SamlIssuerPanel(false, true);
        samlIssuerHolderPanel.setLayout(new BorderLayout());
        samlIssuerHolderPanel.add(samlIssuerPanel, BorderLayout.CENTER);

        samlVersionComboBox.setModel(new DefaultComboBoxModel(new SamlVersion[]{SAML2, SAML1_1}));

        final RunOnChangeListener versionListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                final String currentVersionStatus = statusCodeComboBox.getEditor().getItem().toString().trim();
                updateComponentsForVersion();

                // is the previous status an actual status?
                final boolean isValidStatus = allStatuses.contains(currentVersionStatus);

                if (!isValidStatus && !currentVersionStatus.isEmpty()) {
                    statusCodeComboBox.setSelectedItem(currentVersionStatus);
                }
            }
        });

        samlVersionComboBox.addActionListener(versionListener);

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

            @Override
            public void focusGained(FocusEvent e) {
                final Object source = e.getSource();
                if (!(source instanceof JTextField)) return;
                final JTextField sourceField = (JTextField) source;

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if(sourceField.getText().equals(autoString)){
                            sourceField.setText("");
                        }
                    }
                });
            }
        };
        
        idTextField.addFocusListener(focusAdapter);
        responseIdTextField.addFocusListener(focusAdapter);
        issueInstantTextField.addFocusListener(focusAdapter);
        issueInstant1_1TextField.addFocusListener(focusAdapter);
   }

    @Override
    public void setData(SamlpResponseBuilderAssertion assertion) {

        samlIssuerPanel.setData(assertion);

        switch (assertion.getVersion()) {
            case 2:
                samlVersionComboBox.setSelectedItem(SAML2);
                break;
            case 1:
                samlVersionComboBox.setSelectedItem(SAML1_1);
                break;
            default:
                throw new RuntimeException("Unknown SAML version");
        }

        signResponseCheckBox.setSelected(assertion.isSignResponse());
        includeSignerCertChainCheckBox.setSelected( assertion.isIncludeSignerCertChain() );
        validateWebSSORulesCheckBox.setSelected(assertion.isValidateWebSsoRules());
        statusMessageTextField.setText(assertion.getStatusMessage());
        statusDetailTextField.setText(assertion.getStatusDetail());

        final String issueInstant = assertion.getIssueInstant();
        final String issueInstantText = (issueInstant == null || issueInstant.trim().isEmpty()) ? autoString : issueInstant;
        issueInstantTextField.setText(issueInstantText);
        issueInstant1_1TextField.setText(issueInstantText);

        final String inResponseTo = assertion.getInResponseTo();
        if(inResponseTo != null && !inResponseTo.trim().isEmpty()){
            inResponseToTextField.setText(inResponseTo);
            inResponseTo1_1TextField.setText(inResponseTo);
        }

        final String responseAssertions = assertion.getResponseAssertions();
        if(responseAssertions != null && !responseAssertions.trim().isEmpty())
            assertionsTextField.setText(responseAssertions);

        final String responseId = assertion.getResponseId();
        final String responseIdText = (responseId == null || responseId.trim().isEmpty()) ? autoString: responseId;
        idTextField.setText(responseIdText);
        responseIdTextField.setText(responseIdText);

        if(assertion.getVersion() == 2){
            final String destination = assertion.getDestination();
            if(destination != null && !destination.trim().isEmpty()) destinationTextField.setText(destination);

            final String consent = assertion.getConsent();
            if(consent != null && !consent.trim().isEmpty()) consentTextField.setText(consent);

            final String encryptedAssertions = assertion.getEncryptedAssertions();
            if(encryptedAssertions != null && !encryptedAssertions.trim().isEmpty())
                encryptedAssertionsTextField.setText(encryptedAssertions);

            final String responseExtensions = assertion.getResponseExtensions();
            if(responseExtensions != null && !responseExtensions.trim().isEmpty())
                extensionsTextField.setText(responseExtensions);
        } else if (assertion.getVersion() == 1){
            final String recipient = assertion.getRecipient();
            if(recipient != null && !recipient.trim().isEmpty()) recipientTextField.setText(recipient);
        }

        updateComponentsForVersion();
        statusCodeComboBox.setSelectedItem(assertion.getSamlStatusCode());
    }
    
    @Override
    public SamlpResponseBuilderAssertion getData(SamlpResponseBuilderAssertion assertion) throws ValidationException {
        validateData();
        final SamlVersion samlVersion = (SamlVersion) samlVersionComboBox.getSelectedItem();
        assertion.setVersion(samlVersion.getVersionInt());
        assertion.setSignResponse(signResponseCheckBox.isSelected());
        assertion.setIncludeSignerCertChain( includeSignerCertChainCheckBox.isSelected() );
        assertion.setValidateWebSsoRules(validateWebSSORulesCheckBox.isSelected());

        samlIssuerPanel.getData(assertion);

        // Access editor directly go get current text
        assertion.setSamlStatusCode(((String) statusCodeComboBox.getEditor().getItem()).trim());
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

                final String encryptedAssertions = encryptedAssertionsTextField.getText().trim();
                assertion.setEncryptedAssertions((encryptedAssertions.isEmpty()) ? null : encryptedAssertions);

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

    private boolean isNullOrEmptyOrAuto(@Nullable final String equal, @Nullable final String test){
        return test == null || test.trim().isEmpty() || test.trim().equals(equal);
    }

    private void validateData() throws ValidationException{

        samlIssuerPanel.validateData();

        //Check status code against assertion selection, also validate it's a URI
        final String samlStatus = ((String) statusCodeComboBox.getEditor().getItem()).trim();
        final String statusString = stripColon(resources.getString("responseStatus.code"));
        final String variableRefErrorMsg = " may only contain valid context variables references";
        final String invalidVarReference = " contains an invalid variable reference.";
        try {
            final String[] referencedNames = Syntax.getReferencedNames(samlStatus);
            if (referencedNames.length == 0) {
                final SamlVersion samlVersion = (SamlVersion) samlVersionComboBox.getSelectedItem();
                if (samlVersion.getVersionInt() == 2) {
                    if (!statusSaml2.contains(samlStatus)) {
                        throw new ValidationException("Invalid SAML 2.0 status code");
                    }
                } else if (!statusSaml1.contains(samlStatus)) {
                    throw new ValidationException("Invalid SAML 1.1 status code");
                }
            }
        } catch (VariableNameSyntaxException e) {
            throw new ValidationException(statusString + invalidVarReference);
        }

        final String respAssertions = assertionsTextField.getText().trim();

        if(!respAssertions.isEmpty() && !Syntax.validateStringOnlyReferencesVariables(respAssertions)){
            throw new ValidationException(stripColon(resources.getString("responseElements.assertions")) + variableRefErrorMsg);
        }

        final String statusDetails = statusDetailTextField.getText();
        if(statusDetails != null && !statusDetails.trim().isEmpty()){
            if(!Syntax.validateStringOnlyReferencesVariables(statusDetails))
                throw new ValidationException(stripColon(resources.getString("responseStatus.detail")) + variableRefErrorMsg);

        }

        final SamlVersion samlVersion =
                (SamlVersion) samlVersionComboBox.getSelectedItem();
            switch(samlVersion){
                case SAML2:
                    final String encryptedAssertions = encryptedAssertionsTextField.getText().trim();
                    if(!encryptedAssertions.isEmpty() && !Syntax.validateStringOnlyReferencesVariables(encryptedAssertions))
                        throw new ValidationException(stripColon(resources.getString("responseElements.encryptedAssertions.2_0_only")) +
                                variableRefErrorMsg);

                    if (validateWebSSORulesCheckBox.isSelected()) {
                        final boolean statusHasVar = Syntax.getReferencedNames(samlStatus).length != 0;
                        final boolean isSuccess = samlStatus.equals(SamlStatus.SAML2_SUCCESS.getValue());
                        if(respAssertions.isEmpty() && encryptedAssertions.isEmpty() && isSuccess && validateWebSSORulesCheckBox.isSelected()){
                            throw new ValidationException("If no assertions are entered the status cannot be success when Validate Web SSO Rules is configured.");
                        } else if ((!respAssertions.isEmpty() || !encryptedAssertions.isEmpty()) && !isSuccess && !statusHasVar){
                            // status must be success
                            throw new ValidationException("If status is not success then no assertions can be entered when Validate Web SSO Rules is configured.");
                        }
                    }

                    final String extensions = extensionsTextField.getText().trim();
                    if(!extensions.isEmpty() && !Syntax.validateStringOnlyReferencesVariables(extensions)){
                        throw new ValidationException(stripColon(resources.getString("responseElements.extensions.2_0_only")) +
                                variableRefErrorMsg);
                    }
                    break;
            }

        if (!Syntax.validateAnyVariableReferences(statusMessageTextField.getText().trim())) {
            throw new ValidationException(stripColon(resources.getString("responseStatus.message")) + invalidVarReference);
        }

        if (!Syntax.validateAnyVariableReferences(idTextField.getText().trim())) {
            throw new ValidationException(stripColon(resources.getString("responseAttributes.Id")) + invalidVarReference);
        }

        if (!Syntax.validateAnyVariableReferences(responseIdTextField.getText().trim())) {
            throw new ValidationException(stripColon(resources.getString("responseAttributes.responseId")) + invalidVarReference);
        }

        if (!Syntax.validateAnyVariableReferences(issueInstantTextField.getText().trim())) {
            throw new ValidationException(stripColon(resources.getString("responseAttributes.issueInstant")) + invalidVarReference);
        }

        if (!Syntax.validateAnyVariableReferences(issueInstant1_1TextField.getText().trim())) {
            throw new ValidationException(stripColon(resources.getString("responseAttributes.issueInstant")) + invalidVarReference);
        }

        if (!Syntax.validateAnyVariableReferences(inResponseToTextField.getText().trim())) {
            throw new ValidationException(stripColon(resources.getString("responseAttributes.inResponseTo")) + invalidVarReference);
        }

        if (!Syntax.validateAnyVariableReferences(inResponseTo1_1TextField.getText().trim())) {
            throw new ValidationException(stripColon(resources.getString("responseAttributes.inResponseTo")) + invalidVarReference);
        }

        if (!Syntax.validateAnyVariableReferences(destinationTextField.getText().trim())) {
            throw new ValidationException(stripColon(resources.getString("responseAttributes.destination")) + invalidVarReference);
        }

        if (!Syntax.validateAnyVariableReferences(consentTextField.getText().trim())) {
            throw new ValidationException(stripColon(resources.getString("responseAttributes.consent")) + invalidVarReference);
        }

        if (!Syntax.validateAnyVariableReferences(recipientTextField.getText().trim())) {
            throw new ValidationException(stripColon(resources.getString("responseAttributes.recipient")) + invalidVarReference);
        }
    }

    private String stripColon(String resourceName){
        return resourceName.substring(0, resourceName.length() - 1);
    }

    private void updateComponentsForVersion(){

        final Object item = samlVersionComboBox.getSelectedItem();
        final boolean saml2 = item == null || (item).equals(SAML2);
        if (saml2) {
            issuerTabbedPane.setEnabledAt(1, true);
            saml2_0Panel.setVisible(true);
            saml1_1Panel.setVisible(false);
            statusCodeComboBox.setModel(new DefaultComboBoxModel(SamlStatus.getSaml2xStatusesStrings()));
            extensionsTextField.setVisible(true);
            extensionsLabel.setVisible(true);
            encryptedAssertionsLabel.setVisible(true);
            encryptedAssertionsTextField.setVisible(true);
            samlIssuerPanel.setVersion(2);
        } else {
            issuerTabbedPane.setEnabledAt(1, false);
            saml2_0Panel.setVisible(false);
            saml1_1Panel.setVisible(true);
            statusCodeComboBox.setModel(new DefaultComboBoxModel(SamlStatus.getSaml1xStatusesStrings()));
            extensionsTextField.setVisible(false);
            extensionsLabel.setVisible(false);
            encryptedAssertionsLabel.setVisible(false);
            encryptedAssertionsTextField.setVisible(false);
            samlIssuerPanel.setVersion(1);
        }
        DialogDisplayer.pack(SamlpResponseBuilderPropertiesDialog.this);
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
    private JLabel extensionsLabel;
    private JLabel encryptedAssertionsLabel;
    private JTextField encryptedAssertionsTextField;
    private JCheckBox validateWebSSORulesCheckBox;
    private JPanel samlIssuerHolderPanel;
    private JTabbedPane issuerTabbedPane;
    private JCheckBox includeSignerCertChainCheckBox;
    private SamlIssuerPanel samlIssuerPanel;

    private final Set<String> allStatuses = SamlStatus.getAllSamlStatusSet();
    private final Set<String> statusSaml2 = SamlStatus.getSaml2xStatusSet();
    private final Set<String> statusSaml1 = SamlStatus.getSaml1xStatusSet();

    private static final String autoString = "<auto>";
    private static final ResourceBundle resources = ResourceBundle.getBundle( SamlpResponseBuilderPropertiesDialog.class.getName() );
}
