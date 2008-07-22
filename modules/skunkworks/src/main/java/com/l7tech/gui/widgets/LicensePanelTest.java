/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gui.widgets;

import com.l7tech.gateway.common.License;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.console.panels.LicensePanel;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.cert.X509Certificate;

/**
 * @author mike
 */
public class LicensePanelTest {
    public static final String TEST_LICENSE =
            "<license Id=\"1001\" xmlns=\"http://l7tech.com/license\">\n" +
        "    <description>Layer 7 Internal Developer License</description>\n" +
        "    <valid>2005-09-23T22:48:08.187Z</valid>\n" +
        "    <expires>2101-01-01T23:48:08.187Z</expires>\n" +
        "    <host name=\"*\"/>\n" +
        "    <ip address=\"*\"/>\n" +
        "    <product name=\"Layer 7 SecureSpan Suite\">\n" +
        "        <version major=\"3\" minor=\"4\"/>\n" +
        "    </product>\n" +
        "    <licensee contactEmail=\"developers@layer7tech.com\" name=\"Layer 7 Developer\"/>\n" +
        "    <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
        "        <ds:SignedInfo>\n" +
        "            <ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
        "            <ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>\n" +
        "            <ds:Reference URI=\"#1001\">\n" +
        "                <ds:Transforms>\n" +
        "                    <ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>\n" +
        "                    <ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>\n" +
        "                </ds:Transforms>\n" +
        "                <ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>\n" +
        "                <ds:DigestValue>hM95b6G/X6Di/xOY/YD3BQq87Y4=</ds:DigestValue>\n" +
        "            </ds:Reference>\n" +
        "        </ds:SignedInfo>\n" +
        "        <ds:SignatureValue>DV0y9EJuTLOQY1rdKuMaN5Td2YxRi/ozbrGNLQjCXwMOjk3PobGeMdiNWWVqaHxFbJ7/5z15lc1mQoN0rR8LV1pnYRGHRyv+ZKX41fEyRLCvcU/+k3zU6l7+Lscz+5c2+kHpw3tEJKhZ5Njb7LUMH4uw4Tc1whNNidpbE9atsxg=</ds:SignatureValue>\n" +
        "        <ds:KeyInfo>\n" +
        "            <ds:KeyName>CN=riker</ds:KeyName>\n" +
        "        </ds:KeyInfo>\n" +
        "    </ds:Signature>\n" +
        "</license>";

    public static void main(String[] args) {
        final JDialog dlg = new JDialog();
        final LicensePanel licensePanel = new LicensePanel("My SSG", true);

        Container p = dlg.getContentPane();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(licensePanel);

        JPanel frobPanel = new JPanel();
        p.add(frobPanel);
        frobPanel.setLayout(new BoxLayout(frobPanel, BoxLayout.X_AXIS));
        frobPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        addLicensePanelTestButtons(frobPanel, licensePanel);

        JButton packButton = new JButton("Pack");
        frobPanel.add(packButton);
        packButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.pack();
            }
        });

        JButton quitButton = new JButton("Quit");
        frobPanel.add(quitButton);
        quitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.setVisible(false);
            }
        });

        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setModal(true);
        dlg.setVisible(true);
        dlg.dispose();
        System.exit(0);
    }

    public static void addLicensePanelTestButtons(Container frobHolder, final LicensePanel licensePanel) {
        JButton noneButton = new JButton("Set to No License");
        frobHolder.add(noneButton);
        noneButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                licensePanel.setLicense(null);
            }
        });

        JButton errorButton = new JButton("Set to Invalid License");
        frobHolder.add(errorButton);
        errorButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                licensePanel.setLicenseError("blah foo bar quick brown fox jumps over the lazy dog and then we " +
                        "have a lengthy license error to explain my god this is a long error message i dont know " +
                        "if i can take it what should i write about here anyway to make it long enough? " +
                        "i know i'll tell a story about an exception: there once was an exception.  " +
                        "it was caught.  the end");
            }
        });

        JButton goodButton = new JButton("Set to valid license");
        frobHolder.add(goodButton);
        goodButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                licensePanel.setLicense(getTestLicense());
            }
        });
    }

    private static License getTestLicense() {
        try {
            return new License(TEST_LICENSE, new X509Certificate[] { null/*TestDocuments.getDotNetServerCertificate()*/ }, GatewayFeatureSets.getFeatureSetExpander());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
