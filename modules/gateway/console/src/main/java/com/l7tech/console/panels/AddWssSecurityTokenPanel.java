package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.AddWssSecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.xml.KeyReference;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

/**
 * GUI configuration panel for the {@link AddWssSecurityToken} assertion.
 */
public class AddWssSecurityTokenPanel extends ValidatedPanel<AddWssSecurityToken>  {
    private JPanel mainPanel;
    private JComboBox tokenTypeCombo;
    private JRadioButton bstRadio;
    private JRadioButton strRadio;
    private JRadioButton issuerSerialRadio;
    private JCheckBox includePasswordCheckBox;
    private JTextField usernameTextField;
    private JCheckBox showPasswordCheckBox;
    private JCheckBox includeNonceCheckBox;
    private JCheckBox includeCreatedCheckBox;
    private JCheckBox digestPasswordCheckBox;
    private JPasswordField passwordField;
    private JCheckBox encryptUsernameTokenCheckBox;
    private JRadioButton useLastGatheredRequestRadioButton;
    private JRadioButton useSpecifiedCredentialsRadioButton;
    private JPanel samlVariablePanel;
    private TargetVariablePanel samlTargetVariablePanel;
    private JPanel wsscSessionVariablePanel;
    private JCheckBox includeSecurityContextTokenInMessageCheckBox;
    private TargetVariablePanel wsscSessionVariableTargetVariablePanel;
    private JTabbedPane propertiesTabbedPane;
    private JCheckBox signTokenCheckBox;

    private AddWssSecurityToken assertion;
   
    public AddWssSecurityTokenPanel(AddWssSecurityToken model) {
        super("assertion");
        this.assertion = model;
        init();
    }

    @Override
    protected void initComponents() {
        RunOnChangeListener updater = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                updateVisibleTab();
                checkSyntax();
                updateButtons();
            }
        } );

        // Neuter tabbed pane back into something resembling a CardLayout panel by hiding tabs and border
        propertiesTabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
                return 0;
            }

            @Override
            protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect) {
            }

            @Override
            protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            }
        });

        tokenTypeCombo.setModel(new DefaultComboBoxModel(AddWssSecurityToken.SUPPORTED_TOKEN_TYPES));
        tokenTypeCombo.addActionListener(updater);

        samlTargetVariablePanel = new TargetVariablePanel();
        samlTargetVariablePanel.setValueWillBeWritten(false);
        samlTargetVariablePanel.setValueWillBeRead(true);
        samlVariablePanel.setLayout(new BorderLayout());
        samlVariablePanel.add(samlTargetVariablePanel, BorderLayout.CENTER);

        wsscSessionVariableTargetVariablePanel = new TargetVariablePanel();
        wsscSessionVariableTargetVariablePanel.setValueWillBeWritten(false);
        wsscSessionVariableTargetVariablePanel.setValueWillBeRead(true);
        wsscSessionVariablePanel.setLayout(new BorderLayout());
        wsscSessionVariablePanel.add(wsscSessionVariableTargetVariablePanel, BorderLayout.CENTER);

        includePasswordCheckBox.addActionListener(updater);

        ButtonGroup bg = new ButtonGroup();
        bg.add(bstRadio);
        bg.add(strRadio);
        bg.add(issuerSerialRadio);

        bstRadio.addActionListener(updater);
        strRadio.addActionListener(updater);
        issuerSerialRadio.addActionListener(updater);
        useSpecifiedCredentialsRadioButton.addActionListener(updater);
        useLastGatheredRequestRadioButton.addActionListener(updater);

        Utilities.configureShowPasswordButton(showPasswordCheckBox, passwordField);

        includePasswordCheckBox.addActionListener( updater );

        Utilities.enableGrayOnDisabled(usernameTextField);
        Utilities.enableGrayOnDisabled(passwordField);

        updateVisibleTab();
        updateButtons();

        add(mainPanel, BorderLayout.CENTER);
    }

    private void updateVisibleTab() {
        propertiesTabbedPane.setSelectedIndex(tokenTypeCombo.getSelectedIndex());
    }

    private void updateButtons() {
        boolean specifiedCreds = useSpecifiedCredentialsRadioButton.isSelected();
        includeNonceCheckBox.setEnabled(specifiedCreds);
        includeCreatedCheckBox.setEnabled(specifiedCreds);
        usernameTextField.setEnabled(specifiedCreds);
        digestPasswordCheckBox.setEnabled(specifiedCreds);
        encryptUsernameTokenCheckBox.setEnabled(specifiedCreds);

        boolean includePassword = includePasswordCheckBox.isSelected();
        boolean specifiedPassword = specifiedCreds && includePassword;
        passwordField.setEnabled(specifiedPassword);
        showPasswordCheckBox.setEnabled(specifiedPassword);
        digestPasswordCheckBox.setEnabled(specifiedPassword);
    }

    private void validateData() throws AssertionPropertiesOkCancelSupport.ValidationException {
        final Object type = tokenTypeCombo.getSelectedItem();
        if (SecurityTokenType.WSSC_CONTEXT.equals(type)) {
            wsscSessionVariableTargetVariablePanel.updateStatus();
            if (!wsscSessionVariableTargetVariablePanel.isEntryValid())
                throw new AssertionPropertiesOkCancelSupport.ValidationException(wsscSessionVariableTargetVariablePanel.getErrorMessage());
        } else if (SecurityTokenType.SAML_ASSERTION.equals(type)) {
            samlTargetVariablePanel.updateStatus();
            if (!samlTargetVariablePanel.isEntryValid())
                throw new AssertionPropertiesOkCancelSupport.ValidationException(samlTargetVariablePanel.getErrorMessage()); 
        }
    }

    @Override
    protected void doUpdateModel() {
        validateData();
        SecurityTokenType type = (SecurityTokenType)tokenTypeCombo.getSelectedItem();
        final boolean canIncludePass = type == SecurityTokenType.WSS_USERNAME;
        includePasswordCheckBox.setEnabled(canIncludePass);
        assertion.setIncludePassword(canIncludePass && includePasswordCheckBox.isSelected());
        assertion.setTokenType(type);
        assertion.setProtectTokens(signTokenCheckBox.isSelected());

        if (SecurityTokenType.WSS_USERNAME.equals(type)) {
            if (bstRadio.isSelected()) {
                assertion.setKeyReference(KeyReference.BST.getName());
            } else if (strRadio.isSelected()) {
                assertion.setKeyReference(KeyReference.SKI.getName());
            } else if (issuerSerialRadio.isSelected()) {
                assertion.setKeyReference(KeyReference.ISSUER_SERIAL.getName());
            } else {
                throw new IllegalStateException("Neither BST nor SKI nor IssuerSerial selected");
            }
        } else {
            assertion.setKeyReference(null);
        }

        assertion.setUsername(null);
        assertion.setPassword(null);
        if (useSpecifiedCredentialsRadioButton.isSelected()) {
            assertion.setUseLastGatheredCredentials(false);
            assertion.setUsername(usernameTextField.getText());
            assertion.setPassword(includePasswordCheckBox.isSelected() ? new String(passwordField.getPassword()) : null);
        } else {
            assertion.setUseLastGatheredCredentials(true);
        }
        assertion.setIncludeNonce(includeNonceCheckBox.isSelected());
        assertion.setIncludeCreated(includeCreatedCheckBox.isSelected());
        assertion.setDigest(digestPasswordCheckBox.isSelected());
        assertion.setEncrypt(encryptUsernameTokenCheckBox.isSelected());

        assertion.setSamlAssertionVariable(SecurityTokenType.SAML_ASSERTION == type ? samlTargetVariablePanel.getVariable() : null);

        assertion.setWsscSessionVariable(SecurityTokenType.WSSC_CONTEXT == type ? wsscSessionVariableTargetVariablePanel.getVariable() : null);
        assertion.setOmitSecurityContextToken(SecurityTokenType.WSSC_CONTEXT == type && !includeSecurityContextTokenInMessageCheckBox.isSelected());
    }

    @Override
    protected AddWssSecurityToken getModel() {
        return assertion;
    }

    public AddWssSecurityToken getData() {
        doUpdateModel();
        return assertion;

    }

    @Override
    public void focusFirstComponent() {
    }

    public void setModel(AddWssSecurityToken model, Assertion prevAssertion){
        this.assertion = model;
        final SecurityTokenType tokenType = assertion.getTokenType();
        tokenTypeCombo.setSelectedItem(tokenType);
        samlTargetVariablePanel.setAssertion(assertion,prevAssertion);
        wsscSessionVariableTargetVariablePanel.setAssertion(assertion,prevAssertion);
        if (SecurityTokenType.WSS_USERNAME == tokenType) {
            includePasswordCheckBox.setEnabled(true);
            includePasswordCheckBox.setSelected(assertion.isIncludePassword());
            usernameTextField.setText( assertion.getUsername() == null ? "" : assertion.getUsername() );
            passwordField.setText( assertion.getPassword() == null ? "" : assertion.getPassword() );
            includePasswordCheckBox.setSelected( assertion.isIncludePassword() );
            includeNonceCheckBox.setSelected( assertion.isIncludeNonce() );
            includeCreatedCheckBox.setSelected( assertion.isIncludeCreated() );
            digestPasswordCheckBox.setSelected( assertion.isDigest() );
            encryptUsernameTokenCheckBox.setSelected( assertion.isEncrypt() );
            if ( KeyReference.BST.getName().equals(assertion.getKeyReference()) ) {
                bstRadio.setSelected(true);
            } else if ( KeyReference.ISSUER_SERIAL.getName().equals(assertion.getKeyReference()) ) {
                issuerSerialRadio.setSelected(true);
            } else {
                strRadio.setSelected(true);
            }

            if (assertion.isUseLastGatheredCredentials()) {
                useSpecifiedCredentialsRadioButton.setSelected(false);
                useLastGatheredRequestRadioButton.setSelected(true);
            } else {
                useLastGatheredRequestRadioButton.setSelected(false);
                useSpecifiedCredentialsRadioButton.setSelected(true);
            }
        } else if (SecurityTokenType.WSSC_CONTEXT == tokenType) {
            wsscSessionVariableTargetVariablePanel.setVariable(assertion.getWsscSessionVariable() == null ? "" : assertion.getWsscSessionVariable());
            includeSecurityContextTokenInMessageCheckBox.setSelected(!assertion.isOmitSecurityContextToken());
        } else if (SecurityTokenType.SAML_ASSERTION == tokenType) {
            samlTargetVariablePanel.setVariable(assertion.getSamlAssertionVariable() == null ? "" : assertion.getSamlAssertionVariable());
        } else if (SecurityTokenType.WSS_ENCRYPTEDKEY == tokenType) {
            // No extra configuration for this type, currently.
        }

        signTokenCheckBox.setSelected(assertion.isProtectTokens());
        
        updateVisibleTab();
        updateButtons();
    }

}
