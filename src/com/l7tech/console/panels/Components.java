package com.l7tech.console.panels;

import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;

import javax.swing.*;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class is a bag of UI components  shared by panels.
 * <p>
 * The class cannot be instantiated.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class Components {

    /** private constructor, this class cannot be instantiated */
    private Components() {
    }

    /**
     * @return Returns the crendentials location combo box
     *         with list of known credentials locaitons.
     */
    static JComboBox getCredentialsLocationComboBox() {
        JComboBox cbx = new JComboBox();
        cbx.setModel(getCredentialsLocationComboBoxModel());
        return cbx;
    }

    /**
     * create the credentials location (http, ws message) combo box model.
     *
     * @return the <code>ComboBoxModel</code> with credentials location list
     */
    static ComboBoxModel getCredentialsLocationComboBoxModel() {
        Object[] values = credentialsLocationMap.keySet().toArray();
        ComboBoxModel cbm = new DefaultComboBoxModel(values);
        cbm.setSelectedItem("HTTP digest");
        return cbm;
    }

    /**
     * create the credentials location (http, ws message) combobox model, not including "Anonymous".
     */
    static ComboBoxModel getCredentialsLocationComboBoxModelNonAnonymous() {
        Object[] values = credentialsLocationMap.keySet().toArray();
        Object[] newValues = new Object[values.length - 1];
        System.arraycopy(values, 1, newValues, 0, values.length - 1);
        ComboBoxModel cbm = new DefaultComboBoxModel(newValues);
        cbm.setSelectedItem("HTTP digest");
        return cbm;
    }

    /**
     * @return the credentials locaiton map
     */
    static Map getCredentialsLocationMap() {
        return credentialsLocationMap;
    }

    static private Map credentialsLocationMap = new TreeMap();

    // credential locations
    static {
        credentialsLocationMap.put("Anonymous", new TrueAssertion());

        credentialsLocationMap.put("HTTP basic", new HttpBasic());
        credentialsLocationMap.put("HTTP digest", new HttpDigest());
        credentialsLocationMap.put("HTTP client cert", new HttpClientCert());

        credentialsLocationMap.put("WSS token basic", new WssBasic());
        credentialsLocationMap.put("WSS token digest", new WssDigest());
        credentialsLocationMap.put("WSS signed request", new XmlRequestSecurity());

        credentialsLocationMap = Collections.unmodifiableMap(credentialsLocationMap);
    }
}
