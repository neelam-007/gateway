package com.l7tech.console.panels;

import java.util.Locale;
import java.util.ResourceBundle;
import java.awt.BorderLayout;
import javax.swing.*;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.util.Functions;

/**
 * WizardStepPanel for configuration of certificate validation options.
 *
 * <p>This can be used with all identity provider wizards.</p>
 *
 * @author Steve Jones
 */
public class IdentityProviderCertificateValidationConfigPanel extends IdentityProviderStepPanel {

    //- PUBLIC

    /**
     * Constructor - create a new provider type panel.
     *
     * @param next  The panel for use in the next step.
     */
    public IdentityProviderCertificateValidationConfigPanel(WizardStepPanel next) {
        this(next, false);
    }

    /**
     * Constructor - create a new provider type panel.
     *
     * @param next  The panel for use in the next step.
     */
    public IdentityProviderCertificateValidationConfigPanel(WizardStepPanel next, boolean readOnly) {
        super(next);
        initResources();
        initComponents(readOnly);
    }

   /**
     * Populate the configuration data from the wizard input object to the visual components of the panel.
     *
     * @param settings  The current value of configuration items in the wizard input object.
     */
    @Override
    public void readSettings(Object settings) {
        if (settings != null) {
            if (settings instanceof IdentityProviderConfig) {

                IdentityProviderConfig iProviderConfig = (IdentityProviderConfig) settings;
                validationOptionComboBox.setSelectedItem(iProviderConfig.getCertificateValidationType());
            }
        }
    }

    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
    @Override
    public void storeSettings(Object settings) {
        if (settings instanceof IdentityProviderConfig) {
            IdentityProviderConfig config = (IdentityProviderConfig) settings;
            config.setCertificateValidationType((CertificateValidationType) validationOptionComboBox.getSelectedItem());
        }
    }

    /** @return the wizard step label    */
    @Override
    public String getStepLabel() {
        return resources.getString(RES_STEP_TITLE);
    }

    /**
     * Provide the description for the step being taken on this panel.
     *
     * @return  String  The descritpion of the step.
     */
    @Override
    public String getDescription() {
        return resources.getString(RES_STEP_DESCRIPTION);
    }

    // - PRIVATE

    private static final String RES_STEP_TITLE = "validation.step.label";
    private static final String RES_STEP_DESCRIPTION = "validation.step.description";
    private static final String RES_VALTYPE_PREFIX = "validation.option.";
    private static final String RES_VALTYPE_DEFAULT = "default";

    private JPanel mainPanel;
    private JComboBox validationOptionComboBox;
    private ResourceBundle resources;

    /**
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.IdentityProviderDialog", locale);
    }

    /**
     * Init UI, disable components if read-only 
     */
    private void initComponents(final boolean readOnly) {
        setLayout(new BorderLayout());

        DefaultComboBoxModel model = new DefaultComboBoxModel( CertificateValidationType.values());
        model.insertElementAt(null, 0);
        validationOptionComboBox.setModel(model);
        validationOptionComboBox.setRenderer(new TextListCellRenderer<CertificateValidationType>(
                new Functions.Unary<String,CertificateValidationType>(){
                    @Override
                    public String call( final CertificateValidationType type ) {
                        String labelKey = RES_VALTYPE_PREFIX + RES_VALTYPE_DEFAULT;
                        if (type != null) {
                            labelKey = RES_VALTYPE_PREFIX + type.name();
                        }

                        return resources.getString(labelKey);
                    }
                }, null, true));

        validationOptionComboBox.setEnabled(!readOnly);

        add(mainPanel, BorderLayout.CENTER);
    }
}
