package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.SquigglyFieldUtils;
import com.l7tech.external.assertions.samlpassertion.ProcessSamlAttributeQueryRequestAssertion;
import com.l7tech.external.assertions.samlpassertion.SamlVersion;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.widgets.JCheckBoxListModel;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.l7tech.external.assertions.samlpassertion.ProcessSamlAttributeQueryRequestAssertion.SUPPORTED_ATTRIBUTE_NAMEFORMATS;
import static com.l7tech.external.assertions.samlpassertion.ProcessSamlAttributeQueryRequestAssertion.SUPPORTED_SUBJECT_FORMATS;

/**
 * Dialog to configure how an SAML AttributeQuery message is processed. Only version 2.0 is currently supported.
 */
public class ProcessSamlAttributeQueryRequestPropertiesDialog extends AssertionPropertiesOkCancelSupport<ProcessSamlAttributeQueryRequestAssertion> {

    public ProcessSamlAttributeQueryRequestPropertiesDialog(final Window parent,
                                                            final ProcessSamlAttributeQueryRequestAssertion assertion) {
        super(ProcessSamlAttributeQueryRequestAssertion.class, parent, assertion, true);
        initComponents();
        setData(assertion);
        super.initComponents();
    }

    @Override
    public void setData(ProcessSamlAttributeQueryRequestAssertion assertion) {

        final SamlVersion samlVersion = assertion.getSamlVersion();
        samlVersionComboBox.setSelectedItem(samlVersion);
        soapEncapsulatedCheckBox.setSelected(assertion.isSoapEncapsulated());

        if (samlVersion == SamlVersion.SAML2) {
            requireIssuerCheckBox.setSelected(assertion.isRequireIssuer());
            requireSignatureCheckBox.setSelected(assertion.isRequireSignature());
            iDCheckBox.setSelected(assertion.isRequireId());
            versionCheckBox.setSelected(assertion.isRequireVersion());
            issueInstantCheckBox.setSelected(assertion.isRequireIssueInstant());
            consentCheckBox.setSelected(assertion.isRequireConsent());
            destinationCheckBox.setSelected(assertion.isRequireDestination());
            destinationSquigglyTextField.setText(assertion.getDestination());

            // Subject
            nameIDCheckBox.setSelected(assertion.isAllowNameId());
            encryptedIDCheckBox.setSelected(assertion.isAllowEncryptedId());
            decryptCheckBox.setSelected(assertion.isDecryptEncryptedId());
            customSubjectFormatsSquigglyTextField.setText(assertion.getCustomSubjectFormats());

            if (assertion.isRequireSubjectFormat()) {
                requireSubjectFormatCheckBox.setSelected(true);
            }

            final String subjectFormats = assertion.getSubjectFormats();
            if (subjectFormats != null) {
                final Set<String> configuredSubjectFormatSet = new HashSet<String>(Arrays.asList(TextUtils.URI_STRING_SPLIT_PATTERN.split(subjectFormats)));

                final List<JCheckBox> boxesToCheck = subjectFormatListModel.filterEntries(new Functions.Unary<Boolean, JCheckBox>() {
                    @Override
                    public Boolean call(JCheckBox jCheckBox) {
                        final String text = jCheckBox.getText();
                        return configuredSubjectFormatSet.contains(text) && SUPPORTED_SUBJECT_FORMATS.contains(text);
                    }
                });

                for (JCheckBox jCheckBox : boxesToCheck) {
                    jCheckBox.setSelected(true);
                }
            }

            // Attributes
            requireAttributesCheckBox.setSelected(assertion.isRequireAttributes());
            verifyUniqueNameNameFormatCheckBox.setSelected(assertion.isVerifyAttributesAreUnique());
            requireAttrNameFormatCheckBox.setSelected(assertion.isRequireAttributeNameFormat());

            final String attributeNameFormats = assertion.getAttributeNameFormats();
            if (attributeNameFormats != null) {
                final Set<String> attrNameFormatSet = new HashSet<String>(Arrays.asList(TextUtils.URI_STRING_SPLIT_PATTERN.split(attributeNameFormats)));

                final List<JCheckBox> attrBoxesToCheck = attributeNameFormatListModel.filterEntries(new Functions.Unary<Boolean, JCheckBox>() {
                    @Override
                    public Boolean call(JCheckBox jCheckBox) {
                        final String text = jCheckBox.getText();
                        return attrNameFormatSet.contains(text) && SUPPORTED_ATTRIBUTE_NAMEFORMATS.contains(text);
                    }
                });

                for (JCheckBox jCheckBox : attrBoxesToCheck) {
                    jCheckBox.setSelected(true);
                }
            }

            final String customAttributeNameFormats = assertion.getCustomAttributeNameFormats();
            if (customAttributeNameFormats != null) {
                customAttributeNameFormatsSquigglyTextField.setText(customAttributeNameFormats);
            }

        } else {
            throw new IllegalStateException("Only SAML 2.0 is currently supported");// remove / update when 1.1 is also supported.
        }

        targetVariablePanel.setVariable(assertion.getVariablePrefix());
        targetVariablePanel.setDefaultVariableOrPrefix(ProcessSamlAttributeQueryRequestAssertion.DEFAULT_PREFIX);
        targetVariablePanel.setAssertion(assertion,getPreviousAssertion());

        if (assertion.getSamlVersion() == SamlVersion.SAML2) {
            targetVariablePanel.setSuffixes(ProcessSamlAttributeQueryRequestAssertion.VARIABLE_SUFFIXES_V2);
        }
        // update for version 1.1 when supported

        enableDisableComponents();
    }

    @Override
    public ProcessSamlAttributeQueryRequestAssertion getData(ProcessSamlAttributeQueryRequestAssertion assertion)
            throws ValidationException {

        final SamlVersion samlVersion = (SamlVersion) samlVersionComboBox.getSelectedItem();
        assertion.setSoapEncapsulated(soapEncapsulatedCheckBox.isSelected());
        if (samlVersion == SamlVersion.SAML2) {
            assertion.setRequireIssuer(requireIssuerCheckBox.isSelected());
            assertion.setRequireSignature(requireSignatureCheckBox.isSelected());
            assertion.setRequireId(iDCheckBox.isSelected());
            assertion.setRequireVersion(versionCheckBox.isSelected());
            assertion.setRequireIssueInstant(issueInstantCheckBox.isSelected());
            assertion.setRequireConsent(consentCheckBox.isSelected());
            assertion.setRequireDestination(destinationCheckBox.isSelected());
            if (destinationCheckBox.isSelected()) {
                final String errorString = SquigglyFieldUtils.validateSquigglyFieldForUris(destinationSquigglyTextField);
                if (errorString != null) {
                    throw new ValidationException("Invalid Destination: " + errorString);
                }

                assertion.setDestination(destinationSquigglyTextField.getText().trim());
            } else {
                assertion.setDestination(null);
            }

            // Subject

            final boolean allowNameId = nameIDCheckBox.isSelected();
            assertion.setAllowNameId(allowNameId);
            final boolean allowEncryptedId = encryptedIDCheckBox.isSelected();
            if (!allowNameId && !allowEncryptedId) {
                throw new ValidationException("Either NameID or EncryptedID must be allowed.");
            }

            assertion.setAllowEncryptedId(allowEncryptedId);
            assertion.setDecryptEncryptedId(allowEncryptedId && decryptCheckBox.isSelected());

            assertion.setRequireSubjectFormat(requireSubjectFormatCheckBox.isSelected());
            final StringBuilder subjectFormatBuilder = new StringBuilder();

            final List<JCheckBox> checkedFormats = subjectFormatListModel.getAllCheckedEntries();
            for (JCheckBox checkedEntry : checkedFormats) {
                subjectFormatBuilder.append(checkedEntry.getText());
                subjectFormatBuilder.append(" ");
            }

            final String errorString = SquigglyFieldUtils.validateSquigglyFieldForUris(customSubjectFormatsSquigglyTextField);
            if (errorString != null) {
                throw new ValidationException("Invalid Subject Format: " + errorString);
            }

            final String customValue = customSubjectFormatsSquigglyTextField.getText().trim();
            if (subjectFormatBuilder.toString().isEmpty() && customValue.isEmpty()) {
                throw new ValidationException("At least one Subject Format or Custom value must be configured.");
            }

            assertion.setSubjectFormats(subjectFormatBuilder.toString());
            assertion.setCustomSubjectFormats(customValue);

            // Attributes
            assertion.setRequireAttributes(requireAttributesCheckBox.isSelected());
            assertion.setRequireAttributeNameFormat(requireAttrNameFormatCheckBox.isSelected());
            assertion.setVerifyAttributesAreUnique(verifyUniqueNameNameFormatCheckBox.isSelected());

            final StringBuilder attributeNameFormatBuilder = new StringBuilder();

            final List<JCheckBox> checkedNameFormats = attributeNameFormatListModel.getAllCheckedEntries();
            for (JCheckBox checkedNameFormat : checkedNameFormats) {
                attributeNameFormatBuilder.append(checkedNameFormat.getText());
                attributeNameFormatBuilder.append(" ");
            }

            final String customNameFormatError = SquigglyFieldUtils.validateSquigglyFieldForUris(customAttributeNameFormatsSquigglyTextField);
            if (customNameFormatError != null) {
                throw new ValidationException("Invalid NameFormat: " + customNameFormatError);
            }

            final String customNameFormat = customAttributeNameFormatsSquigglyTextField.getText().trim();
            if (attributeNameFormatBuilder.toString().isEmpty() && customNameFormat.isEmpty()) {
                throw new ValidationException("At least one Attribute NameFormat or Custom value must be configured.");
            }

            assertion.setAttributeNameFormats(attributeNameFormatBuilder.toString());
            assertion.setCustomAttributeNameFormats(customNameFormat);

        }
        // update when SAML 1.1 is supported

        assertion.setVariablePrefix(targetVariablePanel.getVariable());
        return assertion;
    }

    // - PROTECTED

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    protected void initComponents() {

        final RunOnChangeListener onChangeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisableComponents();
            }
        });

        samlVersionComboBox.addActionListener(onChangeListener);
        destinationCheckBox.addActionListener(onChangeListener);
        encryptedIDCheckBox.addActionListener(onChangeListener);
        decryptCheckBox.addActionListener(onChangeListener);

        final PauseListenerAdapter pauseListenerAdapter = new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                if (component instanceof SquigglyTextField) {
                    final SquigglyTextField sqigglyField = (SquigglyTextField) component;
                    SquigglyFieldUtils.validateSquigglyFieldForUris(sqigglyField);
                }
            }
        };
        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(destinationSquigglyTextField, pauseListenerAdapter, 500);
        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(customSubjectFormatsSquigglyTextField, pauseListenerAdapter, 500);
        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(customAttributeNameFormatsSquigglyTextField, pauseListenerAdapter, 500);

        samlVersionComboBox.setModel(new DefaultComboBoxModel(new SamlVersion[]{SamlVersion.SAML2}));
        samlVersionComboBox.setRenderer(new TextListCellRenderer<SamlVersion>(new Functions.Unary<String, SamlVersion>() {
            @Override
            public String call(final SamlVersion samlVersion) {
                return samlVersion.toString();
            }
        }));

        final List<JCheckBox> allSubjectFormats = new ArrayList<JCheckBox>();
        for (String nameFormat : SUPPORTED_SUBJECT_FORMATS) {
            allSubjectFormats.add(new JCheckBox(nameFormat));
        }

        subjectFormatListModel = new JCheckBoxListModel(allSubjectFormats);
        subjectFormatListModel.attachToJList(subjectFormatList);

        final List<JCheckBox> allAttributeNameFormats = new ArrayList<JCheckBox>();
        for (String nameFormat : SUPPORTED_ATTRIBUTE_NAMEFORMATS) {
            allAttributeNameFormats.add(new JCheckBox(nameFormat));
        }
        attributeNameFormatListModel = new JCheckBoxListModel(allAttributeNameFormats);
        attributeNameFormatListModel.attachToJList(allowedAttributeNameFormatsList);
    }

    // - PRIVATE
    private JComboBox samlVersionComboBox;
    private JCheckBox soapEncapsulatedCheckBox;
    private JCheckBox requireIssuerCheckBox;
    private JCheckBox iDCheckBox;
    private JCheckBox versionCheckBox;
    private JCheckBox issueInstantCheckBox;
    private JCheckBox consentCheckBox;
    private JCheckBox destinationCheckBox;
    private JCheckBox nameIDCheckBox;
    private JCheckBox encryptedIDCheckBox;
    private JCheckBox decryptCheckBox;
    private JCheckBox requireAttributesCheckBox;
    private JCheckBox verifyUniqueNameNameFormatCheckBox;
    private JCheckBox requireAttrNameFormatCheckBox;
    private JPanel mainPanel;
    private TargetVariablePanel targetVariablePanel;
    private SquigglyTextField destinationSquigglyTextField;
    private SquigglyTextField customSubjectFormatsSquigglyTextField;
    private SquigglyTextField customAttributeNameFormatsSquigglyTextField;
    private JCheckBox requireSubjectFormatCheckBox;
    private JList subjectFormatList;
    private JList allowedAttributeNameFormatsList;
    private JCheckBox requireSignatureCheckBox;
    private JCheckBoxListModel subjectFormatListModel;
    private JCheckBoxListModel attributeNameFormatListModel;

    private void enableDisableComponents() {
        boolean enableAny = !isReadOnly();

        if (samlVersionComboBox.getSelectedItem() == SamlVersion.SAML2) {
            soapEncapsulatedCheckBox.setEnabled(enableAny);
            requireIssuerCheckBox.setEnabled(enableAny);
            iDCheckBox.setEnabled(enableAny);
            versionCheckBox.setEnabled(enableAny);
            issueInstantCheckBox.setEnabled(enableAny);
            consentCheckBox.setEnabled(enableAny);
            destinationCheckBox.setEnabled(enableAny);
            destinationSquigglyTextField.setEditable(enableAny && destinationCheckBox.isSelected());

            nameIDCheckBox.setEnabled(enableAny);
            encryptedIDCheckBox.setEnabled(enableAny);
            decryptCheckBox.setEnabled(enableAny && encryptedIDCheckBox.isSelected());
            customSubjectFormatsSquigglyTextField.setEnabled(enableAny);

            requireAttributesCheckBox.setEnabled(enableAny);
            verifyUniqueNameNameFormatCheckBox.setEnabled(enableAny);

            customAttributeNameFormatsSquigglyTextField.setEnabled(enableAny);
            requireAttrNameFormatCheckBox.setEnabled(enableAny);
        }
        // update for version 1.1 when support is added.

        getOkButton().setEnabled( enableAny && targetVariablePanel.isEntryValid());
    }
}
