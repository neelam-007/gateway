package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.external.assertions.xmlsec.NonSoapCheckVerifyResultsAssertion;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.JCheckBoxListModel;
import com.l7tech.security.xml.SupportedSignatureMethods;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NonSoapCheckVerifyResultsAssertionPropertiesDialog extends NonSoapSecurityAssertionDialog<NonSoapCheckVerifyResultsAssertion> {
    private JPanel contentPane;
    private JList signatureMethodsList;
    private JList digestMethodsList;
    private JCheckBox gatherSignerCertificateSCheckBox;
    private JCheckBox allowMultipleSignaturesCheckBox;
    private JSplitPane splitPane;

    private Map<String,String> digestMethods = new HashMap<String,String>() {{
        put("http://www.w3.org/2000/09/xmldsig#sha1", "SHA-1");
        put("http://www.w3.org/2001/04/xmlenc#ripemd160", "RIPEMD-160");
    }};

    public NonSoapCheckVerifyResultsAssertionPropertiesDialog(Window owner, NonSoapCheckVerifyResultsAssertion assertion) {
        super(owner, assertion);
        initComponents();
        setData(assertion);
        getXpathExpressionLabel().setText("Signed element(s) XPath:");
        getControlsBelowXpath().setLayout(new BorderLayout());
        getControlsBelowXpath().add(createExtraPanel(), BorderLayout.CENTER);
    }

    private JPanel createExtraPanel() {
        java.util.List<JCheckBox> sigBoxes = new ArrayList<JCheckBox>();
        for (SupportedSignatureMethods sm : SupportedSignatureMethods.values()) {
            if ("HMAC".equals(sm.getKeyAlgorithmName())) // Omit HMAC (Bug #7787)
                continue;
            sigBoxes.add(new JCheckBox(sm.getDisplayName()));
            digestMethods.put(sm.getMessageDigestIdentifier(), sm.getDigestAlgorithmName());
        }
        new JCheckBoxListModel(sigBoxes).attachToJList(signatureMethodsList);

        java.util.List<JCheckBox> digBoxes = new ArrayList<JCheckBox>();
        for (String digestName : digestMethods.values()) {
            digBoxes.add(new JCheckBox(digestName));
        }
        new JCheckBoxListModel(digBoxes).attachToJList(digestMethodsList);

        Utilities.deuglifySplitPane(splitPane);

        return contentPane;
    }
}
