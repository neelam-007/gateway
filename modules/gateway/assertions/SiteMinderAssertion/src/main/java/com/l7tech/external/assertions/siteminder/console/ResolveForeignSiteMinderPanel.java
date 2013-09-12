package com.l7tech.external.assertions.siteminder.console;

import com.l7tech.console.panels.SiteMinderConfigPropertiesDialog;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.siteminder.SiteMinderExternalReference;
import com.l7tech.gateway.common.siteminder.SiteMinderAdmin;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResolveForeignSiteMinderPanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(ResolveForeignSiteMinderPanel.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.SiteMinderConfigurationManagerWindow");

    private JPanel contentPane;
    private JPanel mainPanel;
    private JTextField hostNameTextField;
    private JTextField configurationNameTextField;
    private JButton createSiteMinderConfigurationButton;
    private JRadioButton changeRadioButton;
    private JRadioButton removeRadioButton;
    private JRadioButton ignoreRadioButton;
    private JComboBox siteMinderSelectorComboBox;

    private SiteMinderExternalReference foreignRef;

    public ResolveForeignSiteMinderPanel(WizardStepPanel next, SiteMinderExternalReference foreignRef) {
        super(next);
        this.foreignRef = foreignRef;
        initialize();

    }
    @Override
    public String getDescription() {
        return getStepLabel();
    }

    @Override
    public boolean canFinish() {
        return !hasNextPanel();
    }

    @Override
    public String getStepLabel() {
        return "Unresolved SiteMinder Configuration " + foreignRef.getSiteMinderConfiguration().getName();
    }

    @Override
    public void notifyActive() {
        populateSiteMinderComboBox();
    }

    @Override
    public boolean onNextButton() {
        if (changeRadioButton.isSelected()) {
            if (siteMinderSelectorComboBox.getSelectedIndex() < 0) return false;
            try {
                final SiteMinderConfiguration c = getSiteMinderAdmin().getSiteMinderConfiguration(
                        ((SiteMinderConfigurationKey)siteMinderSelectorComboBox.getSelectedItem()).getGoid());
                foreignRef.setLocalizeReplace(c.getGoid());
            } catch (FindException e) {
                return false;
            }
        } else if (removeRadioButton.isSelected()) {
            foreignRef.setLocalizeDelete();
        } else if (ignoreRadioButton.isSelected()) {
            foreignRef.setLocalizeIgnore();
        }
        return true;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        SiteMinderConfiguration config = foreignRef.getSiteMinderConfiguration();

        configurationNameTextField.setText(config.getName());
        hostNameTextField.setText(config.getHostname());

        // default is delete
        removeRadioButton.setSelected(true);
        siteMinderSelectorComboBox.setEnabled(false);

        // enable/disable provider selector as per action type selected
        changeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                siteMinderSelectorComboBox.setEnabled(true);
            }
        });
        removeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                siteMinderSelectorComboBox.setEnabled(false);
            }
        });
        ignoreRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                siteMinderSelectorComboBox.setEnabled(false);
            }
        });

        populateSiteMinderComboBox();

        createSiteMinderConfigurationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createSiteMinderConfiguration();
            }
        });

        enableAndDisableComponents();
    }

    private void populateSiteMinderComboBox() {
        DefaultComboBoxModel<SiteMinderConfigurationKey> agentComboBoxModel = new DefaultComboBoxModel<>();
        populateAgentComboBoxModel(agentComboBoxModel);
        siteMinderSelectorComboBox.setModel(agentComboBoxModel);
    }

    private void createSiteMinderConfiguration() {
        SiteMinderConfiguration config = foreignRef.getSiteMinderConfiguration();
        editAndSave(config);
    }

    private void editAndSave(final SiteMinderConfiguration configuration) {
        final SiteMinderConfigPropertiesDialog configPropDialog =
                new SiteMinderConfigPropertiesDialog(TopComponents.getInstance().getTopParent(), configuration);

        configPropDialog.pack();
        Utilities.centerOnScreen(configPropDialog);
        DialogDisplayer.display(configPropDialog, new Runnable() {
            @Override
            public void run() {
                if (configPropDialog.isConfirmed()){
                    final SiteMinderConfiguration newConfig = configPropDialog.getConfiguration();
                    Runnable reedit = new Runnable() {
                        @Override
                        public void run() {
                            editAndSave(newConfig);
                        }
                    };

                    //Save the connection
                    SiteMinderAdmin admin = getSiteMinderAdmin();
                    SiteMinderConfiguration config = new SiteMinderConfiguration();
                    if (admin == null) return;
                    try{
                        config.copyFrom(newConfig);
                        config.setGoid(Goid.DEFAULT_GOID);
                        config.setGoid(admin.saveSiteMinderConfiguration(config));
                    } catch (UpdateException | SaveException ex){
                        showErrorMessage(resources.getString("errors.saveFailed.title"),
                                resources.getString("errors.saveFailed.message") + " " + ExceptionUtils.getMessage(ex),
                                ex,
                                reedit);
                        return;
                    }

                    // refresh the list of connectors
                    populateSiteMinderComboBox();

                    siteMinderSelectorComboBox.setSelectedItem(new SiteMinderConfigurationKey(config));

                    changeRadioButton.setEnabled(true);
                    changeRadioButton.setSelected(true);
                    siteMinderSelectorComboBox.setEnabled(true);
                }
            }
        }) ;
    }

    /**
     * populates agent combo box with agent IDs
     * @param agentComboBoxModel the model to populate
     */
    private void populateAgentComboBoxModel(DefaultComboBoxModel<SiteMinderConfigurationKey> agentComboBoxModel) {

        try {
            java.util.List<SiteMinderConfiguration> agents = getSiteMinderAdmin().getAllSiteMinderConfigurations();
            for (SiteMinderConfiguration agent: agents) {
                agentComboBoxModel.addElement(new SiteMinderConfigurationKey(agent));
            }
        } catch (FindException e) {
            //do not throw any exceptions at this point. leave agent combo box empty
        }
    }

    private SiteMinderAdmin getSiteMinderAdmin() {
        Registry registry = Registry.getDefault();
        if (!registry.isAdminContextPresent()) {
            logger.warning("Cannot get SiteMinder Configuration Admin due to no Admin Context present.");
            return null;
        }
        return registry.getSiteMinderConfigurationAdmin();
    }

    private void enableAndDisableComponents() {
        final boolean enableSelection = siteMinderSelectorComboBox.getModel().getSize() > 0;
        changeRadioButton.setEnabled( enableSelection );

        if ( !changeRadioButton.isEnabled() && changeRadioButton.isSelected() ) {
            removeRadioButton.setSelected( true );
        }
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }


}
