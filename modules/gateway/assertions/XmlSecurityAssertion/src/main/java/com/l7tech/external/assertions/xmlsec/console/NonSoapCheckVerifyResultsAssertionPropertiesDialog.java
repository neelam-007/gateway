package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.external.assertions.xmlsec.NonSoapCheckVerifyResultsAssertion;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.JCheckBoxListModel;
import com.l7tech.security.xml.SupportedSignatureMethods;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class NonSoapCheckVerifyResultsAssertionPropertiesDialog extends NonSoapSecurityAssertionDialog<NonSoapCheckVerifyResultsAssertion> {
    private JPanel contentPane;
    private JList signatureMethodsList;
    private JList digestMethodsList;
    private JCheckBox gatherCredentialsCheckBox;
    private JCheckBox allowMultipleSigsCheckBox;
    private JSplitPane splitPane;

    private JCheckBoxListModel signatureMethodsModel;
    private JCheckBoxListModel digestMethodsModel;

    private Map<String,String> digestMethods = new HashMap<String,String>() {{
        put("http://www.w3.org/2000/09/xmldsig#sha1", "SHA-1");
        put("http://www.w3.org/2001/04/xmlenc#ripemd160", "RIPEMD-160");
    }};

    private final Map<String,String> sigMethodDisplayNameToUri = new HashMap<String,String>();
    private final Map<String,String> digMethodDisplayNameToUri = new HashMap<String,String>();

    public NonSoapCheckVerifyResultsAssertionPropertiesDialog(Window owner, NonSoapCheckVerifyResultsAssertion assertion) {
        super(owner, assertion);
        initComponents();
        setData(assertion);
        getXpathExpressionLabel().setText("Signed element(s) XPath:");
        getControlsBelowXpath().setLayout(new BorderLayout());
        getControlsBelowXpath().add(createExtraPanel(), BorderLayout.CENTER);
    }

    private JPanel createExtraPanel() {
        allowMultipleSigsCheckBox.setVisible(false); // Hide for now (Bug #7895)

        getSignatureMethodsModel().attachToJList(signatureMethodsList);
        getDigestMethodsModel().attachToJList(digestMethodsList);
        Utilities.deuglifySplitPane(splitPane);

        return contentPane;
    }

    private JCheckBoxListModel getDigestMethodsModel() {
        if (digestMethodsModel != null)
            return digestMethodsModel;
        java.util.List<JCheckBox> digBoxes = new ArrayList<JCheckBox>();
        for (String digestName : digestMethods.values()) {
            digBoxes.add(new JCheckBox(digestName));
        }
        for (Map.Entry<String, String> entry : digestMethods.entrySet()) {
            digMethodDisplayNameToUri.put(entry.getValue(), entry.getKey());
        }
        return digestMethodsModel = new JCheckBoxListModel(digBoxes);
    }

    private JCheckBoxListModel getSignatureMethodsModel() {
        if (signatureMethodsModel != null)
            return signatureMethodsModel;
        java.util.List<JCheckBox> sigBoxes = new ArrayList<JCheckBox>();
        for (SupportedSignatureMethods sm : SupportedSignatureMethods.values()) {
            if ("SecretKey".equals(sm.getKeyAlgorithmName())) // Omit HMAC (Bug #7787)
                continue;
            sigBoxes.add(new JCheckBox(sm.getDisplayName()));
            digestMethods.put(sm.getMessageDigestIdentifier(), sm.getDigestAlgorithmName());
            sigMethodDisplayNameToUri.put(sm.getDisplayName(), sm.getAlgorithmIdentifier());
        }
        return signatureMethodsModel = new JCheckBoxListModel(sigBoxes);
    }

    @Override
    public void setData(NonSoapCheckVerifyResultsAssertion assertion) {
        super.setData(assertion);
        gatherCredentialsCheckBox.setSelected(assertion.isGatherCertificateCredentials());
        allowMultipleSigsCheckBox.setSelected(assertion.isAllowMultipleSigners());

        final Set<String> sigUris = new HashSet<String>(Arrays.asList(assertion.getPermittedSignatureMethodUris()));
        getSignatureMethodsModel().visitEntries(new Functions.Binary<Boolean, Integer, JCheckBox>() {
            @Override
            public Boolean call(Integer integer, JCheckBox cb) {
                String uri = sigMethodDisplayNameToUri.get(cb.getText());
                return uri != null && sigUris.contains(uri);
            }
        });

        final Set<String> digUris = new HashSet<String>(Arrays.asList(assertion.getPermittedDigestMethodUris()));
        getDigestMethodsModel().visitEntries(new Functions.Binary<Boolean, Integer, JCheckBox>() {
            @Override
            public Boolean call(Integer integer, JCheckBox cb) {
                String uri = digMethodDisplayNameToUri.get(cb.getText());
                return uri != null && digUris.contains(uri);
            }
        });
    }

    @Override
    public NonSoapCheckVerifyResultsAssertion getData(NonSoapCheckVerifyResultsAssertion assertion) throws ValidationException {
        assertion = super.getData(assertion);

        assertion.setGatherCertificateCredentials(gatherCredentialsCheckBox.isSelected());
        assertion.setAllowMultipleSigners(allowMultipleSigsCheckBox.isSelected());

        final Set<String> sigUris = new HashSet<String>();
        getSignatureMethodsModel().visitEntries(new Functions.Binary<Boolean, Integer, JCheckBox>() {
            @Override
            public Boolean call(Integer integer, JCheckBox cb) {
                if (cb.isSelected()) {
                    String uri = sigMethodDisplayNameToUri.get(cb.getText());
                    if (uri != null)
                        sigUris.add(uri);
                }
                return null;
            }
        });
        assertion.setPermittedSignatureMethodUris(sigUris.toArray(new String[sigUris.size()]));

        final Set<String> digUris = new HashSet<String>();
        getDigestMethodsModel().visitEntries(new Functions.Binary<Boolean, Integer, JCheckBox>() {
            @Override
            public Boolean call(Integer integer, JCheckBox cb) {
                if (cb.isSelected()) {
                    String uri = digMethodDisplayNameToUri.get(cb.getText());
                    if (uri != null)
                        digUris.add(uri);
                }
                return null;
            }
        });
        assertion.setPermittedDigestMethodUris(digUris.toArray(new String[digUris.size()]));

        return assertion;
    }
}
