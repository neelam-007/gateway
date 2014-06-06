package com.l7tech.console.panels;

import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.ContextMenuTextField;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * <code>ProtectedServiceWizardPanel</code> that represent a step in the wizard
 * <code>WizardStepPanel</code> that collects endpoint access control attributes.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ProtectedServiceWizardPanel extends WizardStepPanel {

    /** Creates new form ServicePanel */
    public ProtectedServiceWizardPanel() {
        super(null); //todo: temporary, upgrade old wizard panels
        initComponents();
    }

    @Override
    public String getDescription() {
        return "Specify how the Gateway gains access to the Web service. " +
               "Include access credentials, if required.";
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() {
        getCredentialsPanel();
        GridBagConstraints gridBagConstraints;

        setLayout(new BorderLayout());

        add(getServiceUrlPanel(), BorderLayout.NORTH);
        getMainPanel().setLayout(new BoxLayout(getMainPanel(), BoxLayout.X_AXIS));
        getMainPanel().setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));

        getCredentialsAndTransportPanel().setLayout(new GridBagLayout());

        getCredentialsAndTransportPanel().add(getAnonymousRadioButton(),
                                         new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                                GridBagConstraints.WEST,
                                                                GridBagConstraints.NONE,
                                                                new Insets(0, 0, 0, 0), 0, 0));
        getCredentialsAndTransportPanel().add(getCredentialsNeededRadioButton(),
                                         new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                                GridBagConstraints.WEST,
                                                                GridBagConstraints.NONE,
                                                                new Insets(0, 0, 0, 0), 0, 0));


        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 99;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getCredentialsAndTransportPanel().add(Box.createGlue(), gridBagConstraints);
        showOrHideCredentialsPanel();

        getMainPanel().add(getCredentialsAndTransportPanel());

        add(getMainPanel(), BorderLayout.CENTER);
        getServiceUrlTextField().setEditable(false);
        getButtonChangeUrl().setText("Change");
    }

    private ButtonGroup getAnonymousButtonGroup() {
        if (anonymousButtonGroup == null) {
            anonymousButtonGroup = new ButtonGroup();
        }
        return anonymousButtonGroup;
    }

    private JRadioButton getCredentialsNeededRadioButton() {
        if (credentialsNeededRadioButton == null) {
            credentialsNeededRadioButton = new JRadioButton("The Gateway will need to " +
                                                            "provide credentials to access this Web service");
            getAnonymousButtonGroup().add(credentialsNeededRadioButton);
            credentialsNeededRadioButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showOrHideCredentialsPanel();
                    checkValid();
                }
            });
        }
        return credentialsNeededRadioButton;
    }

    private JRadioButton getAnonymousRadioButton() {
        if (anonymousRadioButton == null) {
            anonymousRadioButton = new JRadioButton("The Gateway can access " +
                                                    "this protected Web service anonymously");
            getAnonymousButtonGroup().add(anonymousRadioButton);
            anonymousRadioButton.setSelected(true);
            anonymousRadioButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showOrHideCredentialsPanel();
                    checkValid();
                }
            });
        }
        return anonymousRadioButton;
    }

    private void showOrHideCredentialsPanel() {
        if (isAnonymous()) {
            getCredentialsAndTransportPanel().remove(getCredentialsPanel());
            getCredentialsAndTransportPanel().validate();
        } else {
            getCredentialsAndTransportPanel().add(getCredentialsPanel(),
                                             new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                                                                    GridBagConstraints.WEST,
                                                                    GridBagConstraints.NONE,
                                                                    new Insets(6, 48, 24, 0), 0, 0));
            getCredentialsAndTransportPanel().validate();
        }
    }

    private boolean isDataValid() {
        return isAnonymous() || ( getIdentityTextField().getText().length() > 0 );
    }

    @Override
    public boolean canAdvance() {
        return isDataValid();
    }

    @Override
    public boolean canFinish() {
        return isDataValid();
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings. This is a
     * noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     */
    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        final PublishServiceWizard.ServiceAndAssertion
          collect = (PublishServiceWizard.ServiceAndAssertion)settings;
        final HttpRoutingAssertion routingAssertion;
        if (isAnonymous()) {
            String url = getValidRoutingURL();
            if (url == null) {
                routingAssertion = new HttpRoutingAssertion();
            } else {
                routingAssertion = new HttpRoutingAssertion(url);
            }
        } else {
            routingAssertion = new HttpRoutingAssertion(getServiceUrlTextField().getText(), getIdentityTextField().getText(), getCredentials(), getRealmTextField().getText());
        }
        // SSM-4647 change default configuration to forward all headers
        routingAssertion.getRequestHeaderRules().setForwardAll(true);
        routingAssertion.getResponseHeaderRules().setForwardAll(true);
        collect.setRoutingAssertion(routingAssertion);
    }

    private String getValidRoutingURL() {
        String res = getServiceUrlTextField().getText();
        boolean ok = true;
        try {
            if (res != null && res.length() > 0) new URL(res);
        } catch (MalformedURLException e) {
            ok = false;
        }
        while (!ok) {
            res = JOptionPane.showInputDialog("The url " + res + " is not valid.\nPlease provide valid routing url.");
            ok = true;
            if (res != null && res.length() > 0)
                try {
                    new URL(res);
                    getServiceUrlTextField().setText(res);
                } catch (MalformedURLException e) {
                    ok = false;
                }
        }
        if (res != null && res.length() < 1) return null;
        return res;
    }

    /**
     * get the credentials that were entered.
     *
     * todo: deal with certificates too
     * @return the credentialsd byte array
     */
    private String getCredentials() {
        char[] cpass = getIdentityPasswordField().getPassword();
        return String.valueOf(cpass);
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     *
     * @param settings the object representing wizard panel state
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     */
    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (!(settings instanceof PublishServiceWizard.ServiceAndAssertion)) {
            throw new IllegalArgumentException();
        }
        try {
            PublishServiceWizard.ServiceAndAssertion
              sa = (PublishServiceWizard.ServiceAndAssertion)settings;
            PublishedService publishedService = sa.getService();

            service.parseWsdlStrategy( new ServiceDocumentWsdlStrategy(sa.getServiceDocuments()) );
            service.setWsdlUrl(publishedService.getWsdlUrl());
            service.setWsdlXml(publishedService.getWsdlXml());
            service.setDefaultRoutingUrl( sa.isServiceControlRequired() ? sa.getWsdlPortInfo().getAccessPointURL() : null);
            String text = getServiceUrlTextField().getText();
            if (text == null || "".equals(text)) {
                getServiceUrlTextField().setText(sa.getServiceURI());
            }

        } catch (MalformedURLException e) {
            logger.log( Level.WARNING, "Error updating service from settings.", e );
        }

    }


    /** @return the wizard step label    */
    @Override
    public String getStepLabel() {
        return "Credential Transportation";
    }


    private JPanel getServiceUrlPanel() {
        if (serviceUrlPanel != null)
            return serviceUrlPanel;

        serviceUrlPanel = new JPanel();
        serviceUrlPanel.setLayout(new BoxLayout(serviceUrlPanel, BoxLayout.X_AXIS));

        JLabel serviceUrlLabel = new JLabel();
        serviceUrlLabel.setText("Web Service URL:");
        serviceUrlLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 5)));
        serviceUrlPanel.add(serviceUrlLabel);

        getServiceUrlTextField().setText("");
        getServiceUrlTextField().setPreferredSize(new Dimension(200, 20));
        serviceUrlPanel.add(getServiceUrlTextField());
        serviceUrlPanel.add(getButtonChangeUrl());

        return serviceUrlPanel;
    }


    private JComboBox getAuthenticationMethodComboBox() {
        if (authenticationMethodComboBox != null)
            return authenticationMethodComboBox;
        authenticationMethodComboBox =
          CredentialsLocation.getCredentialsLocationComboBox();

        return authenticationMethodComboBox;
    }

    private JPanel getCredentialsPanel() {
        if (credentialsPanel != null)
            return credentialsPanel;

        credentialsPanel = new JPanel();
        credentialsPanel.setLayout(new BoxLayout(credentialsPanel, BoxLayout.Y_AXIS));
        credentialsPanel.setBorder(new EmptyBorder(new Insets(5, 5, 5, 0)));

        JPanel identityPanel = new JPanel();
        identityPanel.setLayout(new BoxLayout(identityPanel, BoxLayout.X_AXIS));
        JLabel identityLabel = new JLabel();
        identityLabel.setText("User Name:");
        identityLabel.setLabelFor(getIdentityTextField());
        identityPanel.add(identityLabel);
        identityPanel.add(Box.createRigidArea(new Dimension(20, 10)));
        getIdentityTextField().setPreferredSize(new Dimension(50, 20));
        identityPanel.add(getIdentityTextField());
        identityPanel.add(Box.createGlue());
        credentialsPanel.add(identityPanel);
        credentialsPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        JPanel passwordPanel = new JPanel();
        passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.X_AXIS));

        JLabel passwordLabel = new JLabel();
        passwordLabel.setText("Password:");
        passwordLabel.setLabelFor(getIdentityPasswordField());
        passwordPanel.add(passwordLabel);

        passwordPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        getIdentityPasswordField().setPreferredSize(new Dimension(50, 20));
        passwordPanel.add(getIdentityPasswordField());
        passwordPanel.add(Box.createGlue());
        credentialsPanel.add(passwordPanel);

        credentialsPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        JPanel realmPanel = new JPanel();
        realmPanel.setLayout(new BoxLayout(realmPanel, BoxLayout.X_AXIS));
        JLabel realmLabel = new JLabel();
        realmLabel.setText("Realm (optional):");
        realmLabel.setLabelFor(getRealmTextField());
        realmPanel.add(realmLabel);

        realmPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        getRealmTextField().setPreferredSize(new Dimension(50, 20));
        realmPanel.add(getRealmTextField());

        realmPanel.add(Box.createGlue());
        credentialsPanel.add(realmPanel);



        Utilities.equalizeComponentSizes(
                realmLabel,
                identityLabel,
                passwordLabel);


        Utilities.equalizeComponentWidth(
                getIdentityPasswordField(),
                getRealmTextField(),
                getIdentityTextField(),
                getAuthenticationMethodComboBox());

        return credentialsPanel;
    }

    private boolean isAnonymous() {
        return getAnonymousRadioButton().isSelected();
    }

    private void checkValid() {
        boolean isValid = isDataValid();
        if (wasValid == null || !Boolean.valueOf(isValid).equals(wasValid))
            notifyListeners();
        wasValid = isValid;
    }

    private JTextField getIdentityTextField() {
        if (identityTextField == null) {
            identityTextField = new JTextField();
            identityTextField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) { checkValid(); }
                @Override
                public void removeUpdate(DocumentEvent e) { checkValid(); }
                @Override
                public void changedUpdate(DocumentEvent e) { checkValid(); }
            });
        }
        return identityTextField;
    }

    private JTextField getRealmTextField() {
        if (realmTextField == null)
            realmTextField = new JTextField();
        return realmTextField;
    }

    private JPasswordField getIdentityPasswordField() {
        if (identityPasswordField == null)
            identityPasswordField = new JPasswordField();
        return identityPasswordField;
    }

    private JPanel getCredentialsAndTransportPanel() {
        if (credentialsAndTransportPanel == null)
            credentialsAndTransportPanel = new JPanel();
        return credentialsAndTransportPanel;
    }

    private JPanel getMainPanel() {
        if (mainPanel == null)
            mainPanel = new JPanel();
        return mainPanel;
    }

    private JTextField getServiceUrlTextField() {
        if (serviceUrlTextField == null)
            serviceUrlTextField = new ContextMenuTextField();
        return serviceUrlTextField;
    }

    private void doChangeUrl() {
        getServiceUrlTextField().setEditable(true);
        getButtonChangeUrl().setText("Default");
    }

    private void doDefaultUrl() {
        try {
            URL url = service.serviceUrl();
            if ( url != null )
                getServiceUrlTextField().setText(url.toExternalForm());
        } catch (Exception e1) {
            //todo: errormanger?
        } 
        getServiceUrlTextField().setEditable(false);
        getButtonChangeUrl().setText("Change");
    }

    private JButton getButtonChangeUrl() {
        if (buttonChangeUrl == null) {
            buttonChangeUrl = new JButton("Change");
            buttonChangeUrl.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    buttonChangeUrl.setPreferredSize(buttonChangeUrl.getSize());
                    if (getServiceUrlTextField().isEditable()) {
                        doDefaultUrl();
                    } else {
                        doChangeUrl();
                    }
                }
            });
        }
        return buttonChangeUrl;
    }

    private static final Logger logger = Logger.getLogger(ProtectedServiceWizardPanel.class.getName());

    private JPanel credentialsPanel;
    private JComboBox authenticationMethodComboBox;
    private JTextField identityTextField;
    private JTextField realmTextField;
    private JPasswordField identityPasswordField;
    private ButtonGroup anonymousButtonGroup;
    private JRadioButton anonymousRadioButton;
    private JRadioButton credentialsNeededRadioButton;
    private JPanel credentialsAndTransportPanel;
    private JPanel serviceUrlPanel;
    private JTextField serviceUrlTextField;
    private JButton buttonChangeUrl;
    private JPanel mainPanel;

    private Boolean wasValid = null;
    private PublishedService service = new PublishedService();

}
