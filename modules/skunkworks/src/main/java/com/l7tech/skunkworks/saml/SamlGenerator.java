package com.l7tech.skunkworks.saml;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gui.ExceptionDialog;
import com.l7tech.gui.util.GuiCertUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlAssertionGenerator;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.saml.SubjectStatement;
import com.l7tech.security.token.http.TlsClientCertToken;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.saml.SamlAssertion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.StringTokenizer;
import java.util.logging.Level;

public class SamlGenerator {

    //- PUBLIC

    /**
     *
     */
    public SamlGenerator() {
        initComponents();
    }

    /**
     * Display the generator window.
     */
    public void showGenerator() {
        mainFrame.setVisible(true);
    }

    /**
     *
     */
    public static void main(final String[] args) {
        SamlGenerator samlGenerator = new SamlGenerator();
        samlGenerator.showGenerator();
    }

    //- PRIVATE

    //
    private static final String SELECTION_NONE = "  --NONE--";

    private static final String[] SAML_NAMESPACES = { SamlConstants.NS_SAML, SamlConstants.NS_SAML2};

    private static final String[] ALL_SUBJECT_CONFIRMATIONS = {
        SamlConstants.CONFIRMATION_SENDER_VOUCHES,
        SamlConstants.CONFIRMATION_HOLDER_OF_KEY,
        SamlConstants.CONFIRMATION_BEARER,
        SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES,
        SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY,
        SamlConstants.CONFIRMATION_SAML2_BEARER
    };

    private static final String[] ALL_AUTH_METHODS = (String[])
            ArrayUtils.concat(SamlConstants.ALL_AUTHENTICATIONS,
                              SamlConstants.ALL_AUTHENTICATIONS_SAML2);

    private static final String[] AUTH_METHODS_WITH_CERTS = {
        // SAML 1.1
        SamlConstants.SSL_TLS_CERTIFICATE_AUTHENTICATION,
        SamlConstants.X509_PKI_AUTHENTICATION,
        SamlConstants.XML_DSIG_AUTHENTICATION,
        // SAML 2.0
        SamlConstants.AUTHENTICATION_SAML2_TLS_CERT,
        SamlConstants.AUTHENTICATION_SAML2_X509,
        SamlConstants.AUTHENTICATION_SAML2_XMLDSIG,
    };

    // bound components
    private JPanel mainPanel;
    private JTextArea inputMessageTextArea;
    private JTextArea decoratedMessageTextArea;
    private JComboBox versionComboBox;
    private JTextField idTextField;
    private JTextField issuerTextField;
    private JTextField notBeforeTextField;
    private JTextField notOnOrAfterTextField;
    private JTextField audienceRestrictionTextField;
    private JButton issuerCertButton;
    private JLabel issuerCertLabel;
    private JButton subjectCertButton;
    private JLabel subjectCertLabel;
    private JCheckBox signMessageCheckBox;
    private JCheckBox subjectThumbprintCheckBox;
    private JCheckBox issuerThumbprintCheckBox;
    private JTextField nameIdentifierTextField;
    private JTextField nameQualifierTextField;
    private JComboBox nameIdentifierFormatComboBox;
    private JComboBox subjectConfirmationComboBox;
    private JComboBox authenticationMethodComboBox;
    private JTextField resourceTextField;
    private JTextField actionTextField;
    private JTextField actionNamespaceTextField;
    private JTextField attributeNameTextField;
    private JTextField attributeNamespaceTextField;
    private JTextField attributeValueTextField;
    private JCheckBox formatXMLCheckBox;
    private JButton generateButton;

    // components
    private JFrame mainFrame;

    //
    private PrivateKey issuerPrivateKey;
    private X509Certificate issuerCertificate;
    private PrivateKey subjectPrivateKey;
    private X509Certificate subjectCertificate;

    /**
     *
     */
    private void initComponents() {
        mainFrame = new JFrame("Saml Assertion Generator v0.4");

        mainFrame.setLayout(new BorderLayout());
        mainFrame.add(mainPanel);

        versionComboBox.setModel(new DefaultComboBoxModel(new String[]{"1.1", "2.0"}));
        subjectConfirmationComboBox.setModel(new DefaultComboBoxModel(ALL_SUBJECT_CONFIRMATIONS));
        ((DefaultComboBoxModel)subjectConfirmationComboBox.getModel()).insertElementAt(SELECTION_NONE, 0);
        subjectConfirmationComboBox.getModel().setSelectedItem(SELECTION_NONE);

        authenticationMethodComboBox.setModel(new DefaultComboBoxModel(ALL_AUTH_METHODS));
        ((DefaultComboBoxModel)authenticationMethodComboBox.getModel()).insertElementAt(SELECTION_NONE, 0);
        authenticationMethodComboBox.getModel().setSelectedItem(SELECTION_NONE);

        nameIdentifierFormatComboBox.setModel(new DefaultComboBoxModel(SamlConstants.ALL_NAMEIDENTIFIERS_SAML2));
        ((DefaultComboBoxModel)nameIdentifierFormatComboBox.getModel()).insertElementAt(SELECTION_NONE, 0);
        nameIdentifierFormatComboBox.getModel().setSelectedItem(SELECTION_NONE);

        issuerCertButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                GuiCertUtil.ImportedData data = GuiCertUtil.importCertificate(mainFrame, true, getCallbackHandler());
                if (data != null) {
                    issuerCertificate = data.getCertificate();
                    issuerPrivateKey = data.getPrivateKey();
                    if (issuerCertificate != null)
                        issuerCertLabel.setText( CertUtils.extractFirstCommonNameFromCertificate(issuerCertificate));
                    else
                        issuerCertLabel.setText("");
                }
            }
        });

        subjectCertButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                GuiCertUtil.ImportedData data = GuiCertUtil.importCertificate(mainFrame, false, getCallbackHandler());
                if (data != null) {
                    subjectCertificate = data.getCertificate();
                    subjectPrivateKey = data.getPrivateKey();
                    if (subjectCertificate != null)
                        subjectCertLabel.setText(CertUtils.extractFirstCommonNameFromCertificate(subjectCertificate));
                    else
                        subjectCertLabel.setText("");
                }
            }
        });

        generateButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                generateSamlStatements();
            }
        });

        mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        mainFrame.pack();
        Utilities.centerOnScreen(mainFrame);
    }

    /**
     *
     */
    private void generateSamlStatements() {
        if (issuerPrivateKey == null) {
            displayError("You must select an issuer key/certificate.");
            return;
        }

        String subjectConfirmation = (String) subjectConfirmationComboBox.getSelectedItem();
        if ((SamlConstants.CONFIRMATION_HOLDER_OF_KEY.equals(subjectConfirmation) ||
            SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY.equals(subjectConfirmation)) &&
            inputMessageTextArea.getText().length() > 0) {
            if (subjectPrivateKey == null) {
                displayError("You must select a subject key/certificate (for HOK).");
                return;
            }
        }

        String authenticationMethod = (String) authenticationMethodComboBox.getSelectedItem();
        if (subjectCertificate == null) {
            displayError("You must select a subject certificate.");
            return;
        }

        try {
            SignerInfo issuerInfo = new SignerInfo(issuerPrivateKey, new X509Certificate[]{issuerCertificate});

            // Set generation options
            SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
            if (versionComboBox.getSelectedItem().equals("2.0")) samlOptions.setVersion(2);
            samlOptions.setSignAssertion(false);
            samlOptions.setClientAddress(InetAddress.getLocalHost());
            samlOptions.setAttestingEntity(issuerInfo);
            if (idTextField.getText().length()>0)
                samlOptions.setId(idTextField.getText());
            else
                samlOptions.setId("saml-" + System.currentTimeMillis());
            samlOptions.setIssuerKeyInfoType(issuerThumbprintCheckBox.isSelected() ? KeyInfoInclusionType.STR_THUMBPRINT : KeyInfoInclusionType.CERT);
            samlOptions.setSignAssertion(false);

            // Generate
            SamlAssertionGenerator samlGenerator = new SamlAssertionGenerator(issuerInfo);
            // this gets fixed up later
            LoginCredentials credentials = LoginCredentials.makeLoginCredentials(new TlsClientCertToken(subjectCertificate), SslAssertion.class);
            SubjectStatement.Confirmation confirmationMethod = SubjectStatement.BEARER;
            if (SamlConstants.CONFIRMATION_HOLDER_OF_KEY.equals(subjectConfirmation) ||
                SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY.equals(subjectConfirmation)) {
                confirmationMethod = SubjectStatement.HOLDER_OF_KEY;
            }
            else if (SamlConstants.CONFIRMATION_SENDER_VOUCHES.equals(subjectConfirmation) ||
                SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES.equals(subjectConfirmation)) {
                confirmationMethod = SubjectStatement.SENDER_VOUCHES;
            }

            SubjectStatement subjectStatement = null;
            boolean signonly = false;
            boolean thumb = subjectThumbprintCheckBox.isSelected();
            KeyInfoInclusionType keyInfoType = thumb ? KeyInfoInclusionType.STR_THUMBPRINT : KeyInfoInclusionType.CERT;
            if (!SELECTION_NONE.equals(authenticationMethod)) {
                subjectStatement =
                    SubjectStatement.createAuthenticationStatement(credentials, confirmationMethod, keyInfoType, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
            } else if (attributeNameTextField.getText().length() > 0) {
                String attribute = attributeNameTextField.getText();
                String attributeNs = attributeNamespaceTextField.getText();
                if (attributeNs.length() == 0) attributeNs = null;
                String attributeValue = attributeValueTextField.getText();
                subjectStatement =
                    SubjectStatement.createAttributeStatement(credentials, confirmationMethod, attribute, attributeNs, attributeValue, keyInfoType, NameIdentifierInclusionType.FROM_CREDS, null, null, null);
            } else if (resourceTextField.getText().length() > 0) {
                String resource = resourceTextField.getText();
                String action = actionTextField.getText();
                String actionNamespace = actionNamespaceTextField.getText();
                if (actionNamespace.length() == 0) actionNamespace = null;
                subjectStatement =
                    SubjectStatement.createAuthorizationStatement(credentials, confirmationMethod, keyInfoType, resource, action, actionNamespace, NameIdentifierInclusionType.FROM_CREDS, null, null, null);
            }
            else if (inputMessageTextArea.getText().length() > 0 && signMessageCheckBox.isSelected()) {
                signonly = true;
            }

            if (subjectStatement == null && !signonly) {
                displayError("You must enter some statement information (authn, authz or attribute).");
                return;
            }

            Document assertion = null;
            if (!signonly) {
                assertion = samlGenerator.createAssertion(subjectStatement, samlOptions);

                // Tweak as necessary
                if (issuerTextField.getText().length() > 0) {
                    if (samlOptions.getVersion()!=2) {
                        assertion.getDocumentElement().setAttribute("Issuer", issuerTextField.getText());
                    }
                    else {
                        Element issuer = DomUtils.findFirstChildElementByName(assertion.getDocumentElement(), SAML_NAMESPACES, "Issuer");
                        if (issuer != null) {
                            DomUtils.setTextContent(issuer, issuerTextField.getText());
                        }
                    }
                }
                Element conditions = DomUtils.findFirstChildElementByName(assertion.getDocumentElement(), SAML_NAMESPACES, "Conditions");
                if (conditions != null) {
                    if (notBeforeTextField.getText().trim().equals("-")) {
                        conditions.removeAttribute("NotBefore");
                    } else if (notBeforeTextField.getText().length() > 0) {
                        conditions.setAttribute("NotBefore", notBeforeTextField.getText());
                    }
                    if (notOnOrAfterTextField.getText().trim().equals("-")) {
                        conditions.removeAttribute("NotOnOrAfter");
                    } else if (notOnOrAfterTextField.getText().length() > 0) {
                        conditions.setAttribute("NotOnOrAfter", notOnOrAfterTextField.getText());
                    }
                    if (audienceRestrictionTextField.getText().length() > 0) {
                        String namespace = samlOptions.getVersion()==2 ? SamlConstants.NS_SAML2 : SamlConstants.NS_SAML;
                        String prefix = samlOptions.getVersion()==2 ? SamlConstants.NS_SAML2_PREFIX : SamlConstants.NS_SAML_PREFIX;
                        String audienceRestrictionElementName = samlOptions.getVersion()==2 ? "AudienceRestriction" : "AudienceRestrictionCondition";
                        Element restriction = DomUtils.findOnlyOneChildElementByName(conditions, namespace, audienceRestrictionElementName);
                        if (restriction == null) {
                            restriction = DomUtils.createAndAppendElementNS(conditions, audienceRestrictionElementName, namespace, prefix);
                        }
                        StringTokenizer audienceTok = new StringTokenizer(audienceRestrictionTextField.getText(), " ");
                        while (audienceTok.hasMoreTokens()) {
                            String audienceRestriction = audienceTok.nextToken();
                            Element audience = DomUtils.createAndAppendElementNS(restriction, "Audience", namespace, prefix);
                            DomUtils.setTextContent(audience, audienceRestriction);
                        }
                    }
                }
                if (!ArrayUtils.contains(ALL_SUBJECT_CONFIRMATIONS, subjectConfirmation)) {
                    // then strip out any confirmation
                    String namespace = samlOptions.getVersion()==2 ? SamlConstants.NS_SAML2 : SamlConstants.NS_SAML;
                    NodeList confirmationEles = assertion.getDocumentElement().getElementsByTagNameNS(namespace, "SubjectConfirmation");
                    for (int c=0; c<confirmationEles.getLength(); c++) {
                        Node confirmation = confirmationEles.item(c);
                        confirmation.getParentNode().removeChild(confirmation);
                    }
                }
                //1.1 <saml:NameIdentifier Format="urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName">
                //2.0 <saml:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName">
                if (!SELECTION_NONE.equals(nameIdentifierFormatComboBox.getSelectedItem()) ||
                    nameQualifierTextField.getText().length() > 0 ||
                    nameIdentifierTextField.getText().length() > 0) {
                    NodeList nameIdNodeList;
                    if (samlOptions.getVersion()!=2) {
                        nameIdNodeList = assertion.getDocumentElement().getElementsByTagNameNS(SamlConstants.NS_SAML, "NameIdentifier");
                    }
                    else {
                        nameIdNodeList = assertion.getDocumentElement().getElementsByTagNameNS(SamlConstants.NS_SAML2, "NameID");
                    }

                    if (nameIdNodeList != null && nameIdNodeList.getLength()>0) {
                        Element nameIdEl = (Element) nameIdNodeList.item(0);
                        if (!SELECTION_NONE.equals(nameIdentifierFormatComboBox.getSelectedItem()))
                            nameIdEl.setAttribute("Format", (String)nameIdentifierFormatComboBox.getSelectedItem());
                        if (nameIdentifierTextField.getText().length() > 0)
                            DomUtils.setTextContent(nameIdEl, nameIdentifierTextField.getText());
                        if (nameQualifierTextField.getText().length() > 0)
                            nameIdEl.setAttribute("NameQualifier", nameQualifierTextField.getText());
                    }
                }
                //1.1 <saml:AuthenticationStatement AuthenticationMethod="urn:ietf:rfc:3075">
                //2.0 <saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig</saml:AuthnContextClassRef>
                if (!SELECTION_NONE.equals(authenticationMethod)) {
                    if (samlOptions.getVersion()!=2) {
                        Element statement = DomUtils.findFirstChildElementByName(
                                assertion.getDocumentElement(),
                                SamlConstants.NS_SAML,
                                "AuthenticationStatement");
                        if (statement != null)
                            statement.setAttribute("AuthenticationMethod", authenticationMethod);
                    }
                    else {
                        NodeList nodeList = assertion.getDocumentElement().getElementsByTagNameNS(SamlConstants.NS_SAML2, "AuthnContextClassRef");
                        if (nodeList != null && nodeList.getLength()>0) {
                            Element authContextClassRefEl = (Element) nodeList.item(0);
                            DomUtils.setTextContent(authContextClassRefEl, authenticationMethod);
                        }
                    }
                }

                // Reformat
                if (formatXMLCheckBox.isSelected()) {
                    DomUtils.stripWhitespace(assertion.getDocumentElement());
                    XmlUtil.format(assertion,true);
                }

                // Sign
                samlOptions.setSignAssertion(true);
                SamlAssertionGenerator.signAssertion(samlOptions,
                        assertion,
                        issuerInfo.getPrivate(),
                        issuerInfo.getCertificateChain(),
                        samlOptions.getIssuerKeyInfoType());
            }

            String assertionStr;

            // Decorate SOAP message?
            if (inputMessageTextArea.getText().length() > 0) {
                // get soap message, add assertion and (optionally) sign it
                String messageText = inputMessageTextArea.getText();
                Document soapDoc = XmlUtil.stringToDocument(messageText);
                WssDecoratorImpl deco = new WssDecoratorImpl();
                DecorationRequirements decoReq = new DecorationRequirements();
                decoReq.setSecurityHeaderActor(null);
                if (assertion != null) decoReq.setSenderSamlToken(SamlAssertion.newInstance(assertion.getDocumentElement()), false);
                else {
                    decoReq.setSenderMessageSigningPrivateKey(subjectPrivateKey);
                    decoReq.setSenderMessageSigningCertificate(subjectCertificate);
                }
                if (signMessageCheckBox.isSelected()) {
                    decoReq.setSignTimestamp(true);
                    if (!signonly) {
                        if (!SamlConstants.CONFIRMATION_SENDER_VOUCHES.equals(subjectConfirmation) &&
                            !SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES.equals(subjectConfirmation)) {
                            if (SamlConstants.CONFIRMATION_BEARER.equals(subjectConfirmation) ||
                                SamlConstants.CONFIRMATION_SAML2_BEARER.equals(subjectConfirmation)) {
                                decoReq.setSenderMessageSigningCertificate(subjectCertificate);
                            }
                            decoReq.setSenderMessageSigningPrivateKey(subjectPrivateKey);
                        }
                        else {
                            decoReq.setSenderMessageSigningCertificate(issuerCertificate);
                            decoReq.setSenderMessageSigningPrivateKey(issuerPrivateKey);
                        }
                    }
                    decoReq.getElementsToSign().addAll(DomUtils.findChildElementsByName(soapDoc.getDocumentElement(), SoapConstants.ENVELOPE_URIS.toArray(new String[0]), "Body"));

                    // sign address if present
                    Element env = soapDoc.getDocumentElement();
                    if (DomUtils.hasChildNodesOfType(env, Node.ELEMENT_NODE)) {
                        Element maybeHeader = DomUtils.findFirstChildElement(env);
                        decoReq.getElementsToSign().addAll(DomUtils.findChildElementsByName(maybeHeader, SoapConstants.WSA_NAMESPACE_ARRAY, "Action"));
                        decoReq.getElementsToSign().addAll(DomUtils.findChildElementsByName(maybeHeader, SoapConstants.WSA_NAMESPACE_ARRAY, "MessageID"));
                        decoReq.getElementsToSign().addAll(DomUtils.findChildElementsByName(maybeHeader, SoapConstants.WSA_NAMESPACE_ARRAY, "ReplyTo"));
                        decoReq.getElementsToSign().addAll(DomUtils.findChildElementsByName(maybeHeader, SoapConstants.WSA_NAMESPACE_ARRAY, "To"));
                    }
                }
                deco.decorateMessage(new Message(soapDoc), decoReq);
                assertionStr = XmlUtil.nodeToString(soapDoc);
            }
            else {
                assertionStr = XmlUtil.nodeToString(assertion);
            }

            decoratedMessageTextArea.setText(assertionStr);
        }
        catch(Exception e) {
            e.printStackTrace();
            displayError("Could not generate assertion. \n\t" + e.getMessage());
        }
    }

    /**
     *
     */
    private CallbackHandler getCallbackHandler() {
        return new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                PasswordCallback passwordCallback = null;

                for (Callback callback : callbacks) {
                    if (callback instanceof PasswordCallback) {
                        passwordCallback = (PasswordCallback) callback;
                    }
                }

                if (passwordCallback != null) {
                    final JPasswordField pwd = new JPasswordField(22);
                    JOptionPane pane = new JOptionPane(pwd, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
                    JDialog dialog = pane.createDialog(null, "Enter " + passwordCallback.getPrompt());
                    dialog.addWindowFocusListener(new WindowAdapter(){
                        @Override
                        public void windowGainedFocus(WindowEvent e) {
                            pwd.requestFocusInWindow();
                        }
                    });
                    dialog.setVisible(true);
                    dialog.dispose();
                    Object value = pane.getValue();
                    if (value != null && ((Integer)value) == JOptionPane.OK_OPTION)
                        passwordCallback.setPassword(pwd.getPassword());
                }
            }
        };
    }

    /**
     *
     */
    private boolean requiresCertificate(String authenticationMethod) {
        return ArrayUtils.contains(AUTH_METHODS_WITH_CERTS, authenticationMethod);
    }

    /**
     *
     */
    private void displayError(String message) {
        displayError("SAML Assertion Generation Error", message);
    }

    /**
     *
     */
    private void displayError(String title, String message) {
        ExceptionDialog ed;
        ed = ExceptionDialog.createExceptionDialog(mainFrame, title, null, message, null, Level.WARNING);
        ed.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        ed.pack();
        Utilities.centerOnScreen(ed);
        ed.setVisible(true);
    }
}
