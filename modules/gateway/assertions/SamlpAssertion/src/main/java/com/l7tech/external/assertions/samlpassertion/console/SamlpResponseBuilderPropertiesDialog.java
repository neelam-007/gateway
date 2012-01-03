package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.samlpassertion.SamlStatus;
import com.l7tech.external.assertions.samlpassertion.SamlVersion;
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
        if(TopComponents.getInstance().isApplet()){
            contentPane.setPreferredSize(new Dimension(510, 470));
        }

        addIssuerCheckBox.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                customIssuerTextField.setEnabled(addIssuerCheckBox.isSelected());
            }
        }));

        samlVersionComboBox.setModel(new DefaultComboBoxModel(new SamlVersion[]{SAML2, SAML1_1}));

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
        customIssuerTextField.addFocusListener(focusAdapter);

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

        if(assertion.getSamlVersion() == SAML2){
            addIssuerCheckBox.setSelected(assertion.isAddIssuer());
            customIssuerTextField.setEnabled(assertion.isAddIssuer());
            final String customIssuer = assertion.getCustomIssuer();
            final String customIdText = (customIssuer == null || customIssuer.trim().isEmpty()) ? autoString: customIssuer;
            customIssuerTextField.setText(customIdText);

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
        } else if (assertion.getSamlVersion() == SAML1_1){
            final String recipient = assertion.getRecipient();
            if(recipient != null && !recipient.trim().isEmpty()) recipientTextField.setText(recipient);
        }

        updateComponentsForVersion();
        statusCodeComboBox.setSelectedItem(assertion.getSamlStatus());
    }
    
    @Override
    public SamlpResponseBuilderAssertion getData(SamlpResponseBuilderAssertion assertion) throws ValidationException {
        validateData();
        final SamlVersion samlVersion = (SamlVersion) samlVersionComboBox.getSelectedItem();
        assertion.setSamlVersion(samlVersion);
        assertion.setSignResponse(signResponseCheckBox.isSelected());
        assertion.setValidateWebSsoRules(validateWebSSORulesCheckBox.isSelected());
        assertion.setAddIssuer(addIssuerCheckBox.isSelected());
        final String customIssuer = customIssuerTextField.getText().trim();
        assertion.setCustomIssuer(isNullOrEmptyOrAuto(autoString, customIssuer)? null: customIssuer);

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

    private boolean isNullOrEmptyOrAuto(final String equal, final String test){
        return test == null || test.trim().isEmpty() || test.trim().equals(equal);
    }

    private void validateData() {

        //Check status code against assertion selection
        final SamlStatus samlStatus = (SamlStatus) statusCodeComboBox.getSelectedItem();
        final String respAssertions = assertionsTextField.getText().trim();

        if(!respAssertions.isEmpty() && !Syntax.validateStringOnlyReferencesVariables(respAssertions)){
            throw new ValidationException(resources.getString("responseElements.assertions") +
                    " may only reference context variables");
        }

        final String statusDetails = statusDetailTextField.getText();
        if(statusDetails != null && !statusDetails.trim().isEmpty()){
            if(!Syntax.validateStringOnlyReferencesVariables(statusDetails))
                throw new ValidationException(resources.getString("responseStatus.detail") +
                    " may only reference context variables");

        }

        final SamlVersion samlVersion =
                (SamlVersion) samlVersionComboBox.getSelectedItem();
            switch(samlVersion){
                case SAML2:
                    final String encryptedAssertions = encryptedAssertionsTextField.getText().trim();
                    if(!encryptedAssertions.isEmpty() && !Syntax.validateStringOnlyReferencesVariables(encryptedAssertions))
                        throw new ValidationException(resources.getString("responseElements.encryptedAssertions.2_0_only") +
                                " may only reference context variables");

                    final boolean isSuccess = samlStatus.equals(SamlStatus.SAML2_SUCCESS);
                    if(respAssertions.isEmpty() && encryptedAssertions.isEmpty() && isSuccess && validateWebSSORulesCheckBox.isSelected()){
                        throw new ValidationException("If no assertions are entered the status cannot be success when Validate Web SSO Rules is configured.");
                    } else if ((!respAssertions.isEmpty() || !encryptedAssertions.isEmpty()) && !isSuccess){
                        // status must be success
                        throw new ValidationException("If status is not success then no assertions can be entered.");
                    }

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
            customIssuerTextField.setVisible(true);
            extensionsTextField.setVisible(true);
            extensionsLabel.setVisible(true);
            encryptedAssertionsLabel.setVisible(true);
            encryptedAssertionsTextField.setVisible(true);
        } else {
            saml2_0Panel.setVisible(false);
            saml1_1Panel.setVisible(true);
            statusCodeComboBox.setModel(new DefaultComboBoxModel(SamlStatus.getSaml1xStatuses().toArray()));
            addIssuerCheckBox.setVisible(false);
            customIssuerTextField.setVisible(false);
            extensionsTextField.setVisible(false);
            extensionsLabel.setVisible(false);
            encryptedAssertionsLabel.setVisible(false);
            encryptedAssertionsTextField.setVisible(false);
        }
        SamlpResponseBuilderPropertiesDialog.this.pack();
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
    private JLabel extensionsLabel;
    private JLabel encryptedAssertionsLabel;
    private JTextField encryptedAssertionsTextField;
    private JCheckBox validateWebSSORulesCheckBox;
    private JTextField customIssuerTextField;

    private static final String autoString = "<auto>";
    private static final ResourceBundle resources = ResourceBundle.getBundle( SamlpResponseBuilderPropertiesDialog.class.getName() );
}
