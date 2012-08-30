package com.l7tech.console.panels.saml;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.SamlElementGenericConfig;
import com.l7tech.policy.assertion.xmlsec.RequireSaml;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlPolicyAssertion;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.saml.SubjectStatement;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The SAML Subject Confirmation selections <code>WizardStepPanel</code>
 *
 * @author emil
 * @version Jan 20, 2005
 */
public class SubjectConfirmationWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JCheckBox confirmationSenderVouchesButton;
    private JCheckBox confirmationHolderOfKeyButton;
    private JCheckBox confirmationBearerButton;
    private JCheckBox confirmationNoneButton;
    private JCheckBox checkBoxSVMessageSignature;
    private JCheckBox checkBoxHoKMessageSignature;
    private JComboBox subjectConfirmationMethodComboBox;
    private JLabel extraTextLabel;
    private JPanel issueMethodsPanel;
    private JPanel validateMethodsPanel;
    private JCheckBox subjectCertIncludeCheckbox;
    private JComboBox certificateInclusionComboBox;
    private JSpinner notBeforeSpinner;
    private JSpinner notOnOrAfterSpinner;
    private JTextField recipientTextField;
    private JTextField addressTextField;
    private JLabel addressLabel;
    private JTextField inResponseToTextField;
    private JCheckBox checkValidityPeriodCheckBox;
    private JLabel inResponseToLabel;
    private JPanel addValidityPeriodPanel;
    private JCheckBox checkAddressCheckBox;
    private JPanel subjectConfirmationDataPanel;
    private JCheckBox notBeforeSecondsInCheckBox;
    private JCheckBox notOnOrAfterCheckBox;

    private Map<String, JToggleButton> confirmationsMap;
    private boolean showTitleLabel;
    private boolean enableSubjectConfirmationData;
    private boolean isSoapAssertion = true;

    private final boolean issueMode;
    private final RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
        @Override
        public void run() {
            enableDisable();
        }
    };

    /**
     * Creates new form SubjectConfirmationWizardStepPanel
     */
    public SubjectConfirmationWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, boolean issueMode) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = issueMode;
        initialize();
    }

    /**
     * Creates new form SubjectConfirmationWizardStepPanel
     */
    public SubjectConfirmationWizardStepPanel(WizardStepPanel next, boolean issueMode) {
        this(next, true, issueMode);
    }


    /**
     * Creates new form Subject confirmation WizardPanel
     */
    public SubjectConfirmationWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog owner, boolean issueMode) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = issueMode;
        setOwner(owner);
        initialize();
    }

    public SubjectConfirmationWizardStepPanel(WizardStepPanel next) {
        this(next, true, false);
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        if ( issueMode ) {
            SamlElementGenericConfig issuerConfiguration = (SamlElementGenericConfig) settings;
            final SubjectStatement.Confirmation confirmation = SubjectStatement.Confirmation.forUri(issuerConfiguration.getSubjectConfirmationMethodUri());

            subjectConfirmationMethodComboBox.setSelectedItem( confirmation );

            KeyInfoInclusionType ckitype = issuerConfiguration.getSubjectConfirmationKeyInfoType();
            if (ckitype == null) ckitype = KeyInfoInclusionType.CERT;
            switch(ckitype) {
                case NONE:
                    subjectCertIncludeCheckbox.setSelected(false);
                    break;
                case CERT:
                case STR_SKI:
                case STR_THUMBPRINT:
                    subjectCertIncludeCheckbox.setSelected(true);
                    certificateInclusionComboBox.setSelectedItem( ckitype );
                    break;
                default:
                    throw new RuntimeException("Unsupported Subject KeyInfoInclusionType: " + ckitype); // Can't happen
            }

            setText( addressTextField, issuerConfiguration.getSubjectConfirmationDataAddress() );
            setText( recipientTextField, issuerConfiguration.getSubjectConfirmationDataRecipient() );
            setText( inResponseToTextField, issuerConfiguration.getSubjectConfirmationDataInResponseTo() );
            if ( issuerConfiguration.getSubjectConfirmationDataNotBeforeSecondsInPast() >= 0 ) {
                notBeforeSecondsInCheckBox.setSelected( true );
                notBeforeSpinner.setValue( issuerConfiguration.getSubjectConfirmationDataNotBeforeSecondsInPast() );
            }
            if ( issuerConfiguration.getSubjectConfirmationDataNotOnOrAfterExpirySeconds() >= 0 ) {
                notOnOrAfterCheckBox.setSelected( true );
                notOnOrAfterSpinner.setValue( issuerConfiguration.getSubjectConfirmationDataNotOnOrAfterExpirySeconds() );
            }
        } else {
            RequireSaml requestSaml = (RequireSaml)settings;
            requestSaml.getSubjectConfirmations();
            for (JToggleButton jToggleButton : confirmationsMap.values()) {
                jToggleButton.setSelected(false);
            }
            String[] confirmations = requestSaml.getSubjectConfirmations();
            for (String confirmation : confirmations) {
                JToggleButton jc = confirmationsMap.get(confirmation);
                if (jc == null) {
                    throw new IllegalArgumentException("No widget for confirmation " + confirmation);
                }
                jc.setSelected(true);
            }

            if (requestSaml instanceof RequireWssSaml) {
                checkBoxSVMessageSignature.setSelected(((RequireWssSaml) requestSaml).isRequireSenderVouchesWithMessageSignature());
                checkBoxHoKMessageSignature.setSelected(((RequireWssSaml)requestSaml).isRequireHolderOfKeyWithMessageSignature());
                checkBoxSVMessageSignature.setEnabled(confirmationSenderVouchesButton.isSelected());
                checkBoxHoKMessageSignature.setEnabled(confirmationHolderOfKeyButton.isSelected());
            } else {
                isSoapAssertion = false;
            }

            confirmationNoneButton.setSelected(requestSaml.isNoSubjectConfirmation());

            setText( recipientTextField, requestSaml.getSubjectConfirmationDataRecipient() );
            checkAddressCheckBox.setSelected( requestSaml.isSubjectConfirmationDataCheckAddress() );
            checkValidityPeriodCheckBox.setSelected( requestSaml.isSubjectConfirmationDataCheckValidity() );
        }

        final Integer version = ((SamlPolicyAssertion)settings).getVersion();
        enableSubjectConfirmationData = version == null || version != 1;

        enableDisable();
    }

    private void enableDisable() {
        if (issueMode) {
            boolean isHok = SubjectStatement.HOLDER_OF_KEY.equals(subjectConfirmationMethodComboBox.getSelectedItem());
            boolean isNone = null == subjectConfirmationMethodComboBox.getSelectedItem();

            subjectCertIncludeCheckbox.setEnabled( isHok );
            boolean subjectCertEnabled =
                    subjectCertIncludeCheckbox.isEnabled() &&
                    subjectCertIncludeCheckbox.isSelected();

            certificateInclusionComboBox.setEnabled(subjectCertEnabled);

            Utilities.setEnabled( subjectConfirmationDataPanel, enableSubjectConfirmationData && !isNone );
            if ( subjectConfirmationDataPanel.isEnabled() ) {
                final boolean enableNotBefore = notBeforeSecondsInCheckBox.isEnabled() && notBeforeSecondsInCheckBox.isSelected();
                notBeforeSpinner.setEnabled( enableNotBefore );

                final boolean notOnOrAfter = notOnOrAfterCheckBox.isEnabled() && notOnOrAfterCheckBox.isSelected();
                notOnOrAfterSpinner.setEnabled( notOnOrAfter );
            }
        } else {
            if (!isSoapAssertion) {
                checkBoxHoKMessageSignature.setVisible(false);
                checkBoxSVMessageSignature.setVisible(false);
            } else {
                checkBoxHoKMessageSignature.setEnabled(confirmationHolderOfKeyButton.isSelected());
                checkBoxSVMessageSignature.setEnabled(confirmationSenderVouchesButton.isSelected());
            }

            final boolean confirmationMethodWithData =
                    confirmationSenderVouchesButton.isSelected() ||
                            confirmationHolderOfKeyButton.isSelected() ||
                            confirmationBearerButton.isSelected();

            Utilities.setEnabled(subjectConfirmationDataPanel, enableSubjectConfirmationData && confirmationMethodWithData);
        }
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     * <p/>
     * This is a noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        if (issueMode) {
            SamlElementGenericConfig issuerConfiguration = (SamlElementGenericConfig) settings;
            SubjectStatement.Confirmation confirmation = (SubjectStatement.Confirmation)subjectConfirmationMethodComboBox.getSelectedItem();
            if ( confirmation != null ) {
                issuerConfiguration.setSubjectConfirmationMethodUri(confirmation.getUri());
            } else {
                issuerConfiguration.setSubjectConfirmationMethodUri(null);
            }

            if (subjectCertIncludeCheckbox.isSelected()) {
                KeyInfoInclusionType inclusion = (KeyInfoInclusionType) certificateInclusionComboBox.getSelectedItem();
                if ( inclusion != null ) {
                    issuerConfiguration.setSubjectConfirmationKeyInfoType( inclusion );
                } else {
                    throw new RuntimeException("No Subject Cert inclusion type selected"); //Can't happen
                }
            } else {
                issuerConfiguration.setSubjectConfirmationKeyInfoType(KeyInfoInclusionType.NONE);
            }

            issuerConfiguration.setSubjectConfirmationDataAddress( getText(addressTextField) );
            issuerConfiguration.setSubjectConfirmationDataRecipient( getText(recipientTextField) );
            issuerConfiguration.setSubjectConfirmationDataInResponseTo( getText(inResponseToTextField) );
            final boolean useNotBeforePeriod = notBeforeSecondsInCheckBox.isSelected() && notBeforeSecondsInCheckBox.isEnabled();
            issuerConfiguration.setSubjectConfirmationDataNotBeforeSecondsInPast(
                    useNotBeforePeriod  ? (Integer) notBeforeSpinner.getValue() : -1 );
            final boolean useNotOnOrAfterPeriod = notOnOrAfterCheckBox.isSelected() && notOnOrAfterCheckBox.isEnabled();
            issuerConfiguration.setSubjectConfirmationDataNotOnOrAfterExpirySeconds(
                    useNotOnOrAfterPeriod ? (Integer)notOnOrAfterSpinner.getValue() : -1 );
        } else {
            RequireSaml requestSaml = (RequireSaml)settings;
            Collection<String> confirmations = new ArrayList<String>();
            for (Map.Entry<String, JToggleButton> entry : confirmationsMap.entrySet()) {
                JToggleButton jc = entry.getValue();
                if (jc.isSelected()) {
                    confirmations.add(entry.getKey());
                }
            }
            requestSaml.setSubjectConfirmations(confirmations.toArray(new String[confirmations.size()]));
            if (requestSaml instanceof RequireWssSaml) {
                ((RequireWssSaml)requestSaml).setRequireHolderOfKeyWithMessageSignature(checkBoxHoKMessageSignature.isSelected());
                ((RequireWssSaml)requestSaml).setRequireSenderVouchesWithMessageSignature(checkBoxSVMessageSignature.isSelected());
            }
            requestSaml.setNoSubjectConfirmation(confirmationNoneButton.isSelected());

            requestSaml.setSubjectConfirmationDataRecipient( getText(recipientTextField) );
            requestSaml.setSubjectConfirmationDataCheckAddress( checkAddressCheckBox.isSelected() );
            requestSaml.setSubjectConfirmationDataCheckValidity(checkValidityPeriodCheckBox.isSelected());
        }
    }

    private String getText( final JTextComponent textComponent ) {
        String text = textComponent.getText();

        if ( !textComponent.isEnabled() || (text != null && text.isEmpty()) ) {
            text = null;            
        }

        return text;
    }

    private void setText( final JTextComponent textComponent, final String text ) {
        if ( text != null ) {
            textComponent.setText( text );
            textComponent.setCaretPosition( 0 );
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        if (issueMode) {
            validateMethodsPanel.setVisible( false );
            checkAddressCheckBox.setVisible( false );
            checkValidityPeriodCheckBox.setVisible( false );
            extraTextLabel.setText("Issue an assertion with the following Subject Confirmation Method");

            subjectConfirmationMethodComboBox.setModel( new DefaultComboBoxModel( new SubjectStatement.Confirmation[]{
                    SubjectStatement.HOLDER_OF_KEY,
                    SubjectStatement.SENDER_VOUCHES,
                    SubjectStatement.BEARER,
                    null,
            } ) );
            subjectConfirmationMethodComboBox.setRenderer( new TextListCellRenderer<SubjectStatement.Confirmation>( new Functions.Unary<String,SubjectStatement.Confirmation>(){
                @Override
                public String call( final SubjectStatement.Confirmation confirmation ) {
                    String text = "";
                    if (confirmation == SubjectStatement.HOLDER_OF_KEY) {
                        text = "Holder of Key";
                    } else if (confirmation == SubjectStatement.SENDER_VOUCHES) {
                        text = "Sender Vouches";
                    } else if (confirmation == SubjectStatement.BEARER) {
                        text = "Bearer";
                    } else if (confirmation == null){
                        text = "None";
                    }
                    return text;
                }
            }, null, true ));
            subjectConfirmationMethodComboBox.addActionListener(enableDisableListener);                    

            certificateInclusionComboBox.setModel( new DefaultComboBoxModel( new KeyInfoInclusionType[]{
                    KeyInfoInclusionType.CERT,
                    KeyInfoInclusionType.STR_SKI,
                    KeyInfoInclusionType.STR_THUMBPRINT
            } ) );
            certificateInclusionComboBox.setRenderer( new TextListCellRenderer<KeyInfoInclusionType>( new Functions.Unary<String,KeyInfoInclusionType>(){
                @Override
                public String call( final KeyInfoInclusionType keyInfoInclusionType ) {
                    String text = "";
                    if (keyInfoInclusionType == KeyInfoInclusionType.CERT) {
                        text = "Literal Certificate (X509Data)";
                    } else if (keyInfoInclusionType == KeyInfoInclusionType.STR_SKI) {
                        text = "Security Token Reference using SKI";
                    } else if (keyInfoInclusionType == KeyInfoInclusionType.STR_THUMBPRINT) {
                        text = "Security Token Reference using SHA1 Thumbprint";
                    }
                    return text;
                }
            }, null, true ));

            subjectCertIncludeCheckbox.addActionListener(enableDisableListener);
            notBeforeSecondsInCheckBox.addActionListener(enableDisableListener);
            notOnOrAfterCheckBox.addActionListener(enableDisableListener);

            notBeforeSpinner.setModel( new SpinnerNumberModel(120, 0, 3600, 1) );
            notOnOrAfterSpinner.setModel( new SpinnerNumberModel(300, 30, 3600, 1) );

            validationRules.add(new InputValidator.NumberSpinnerValidationRule( notBeforeSpinner, "Not Before seconds in past"));
            validationRules.add(new InputValidator.NumberSpinnerValidationRule( notOnOrAfterSpinner, "Not On Or After seconds in future"));
        } else {
            issueMethodsPanel.setVisible( false );
            addressTextField.setVisible( false );
            addressLabel.setVisible( false );
            inResponseToLabel.setVisible( false );
            inResponseToTextField.setVisible( false );
            addValidityPeriodPanel.setVisible( false );
            notBeforeSecondsInCheckBox.addActionListener(enableDisableListener);
            notOnOrAfterCheckBox.addActionListener(enableDisableListener);
        }

        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        String toolTipPRoofOfPosession = "<html>Require the Proof Of Possession -  Signed Message Body or Timestamp" +
          "<br>Alternatively, Proof of Possession can be secured with SSL Client Certificate by using SSL transport.</html>";

        confirmationHolderOfKeyButton.setToolTipText("<html>Key Info for the Subject, that the Assertion describes<br>" +
          " MUST be present within the Subject Confirmation.</html>");

        confirmationHolderOfKeyButton.addItemListener( enableDisableListener );

        checkBoxHoKMessageSignature.setToolTipText(toolTipPRoofOfPosession);

        confirmationSenderVouchesButton.setToolTipText("<html>The attesting entity, different form the subject,<br>" +
          " vouches for the verification of the subject.</html>");
        confirmationSenderVouchesButton.addItemListener( enableDisableListener );
        confirmationBearerButton.setToolTipText("<html>Browser/POST Profile of SAML</html>");
        confirmationBearerButton.addItemListener( enableDisableListener );

        confirmationNoneButton.setToolTipText("<html>No Subject Confirmation MUST be present</html>");
        confirmationsMap = new HashMap<String, JToggleButton>();
        confirmationsMap.put(SamlConstants.CONFIRMATION_HOLDER_OF_KEY, confirmationHolderOfKeyButton);
        confirmationsMap.put(SamlConstants.CONFIRMATION_SENDER_VOUCHES, confirmationSenderVouchesButton);
        confirmationsMap.put(SamlConstants.CONFIRMATION_BEARER, confirmationBearerButton);

        checkBoxSVMessageSignature.setToolTipText(toolTipPRoofOfPosession);

        for (Map.Entry<String, JToggleButton> entry : confirmationsMap.entrySet()) {
            JToggleButton jc = entry.getValue();
            jc.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    notifyListeners();
                }
            });
        }
        confirmationNoneButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                notifyListeners();
            }
        });
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Subject Confirmation";
    }

    @Override
    public String getDescription() {
        if (!issueMode) {
            return "<html>Specify one or more subject confirmations that will be accepted by the gateway" +
                   " and whether the message signature is required as the proof material</html>";
        } else {
            return "<html>Specify the subject confirmation method that will be included in the assertion.</html>";
        }
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one.
     *
     * @return true if the panel is valid, false otherwis
     */
    @Override
    public boolean canAdvance() {
        if (confirmationNoneButton.isSelected() || issueMode) {
            return true;
        }

        for (Map.Entry<String, JToggleButton> entry : confirmationsMap.entrySet()) {
            JToggleButton jc = entry.getValue();
            if (jc.isSelected()) {
                return true;
            }
        }

        return confirmationNoneButton.isSelected();
    }
}
