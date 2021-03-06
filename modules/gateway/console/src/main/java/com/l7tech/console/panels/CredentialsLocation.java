package com.l7tech.console.panels;

import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class contains UI utilities for handling credentials locations such as
 * create <code>ComboBoxModel</code>, <code>JComboBox</code> etc.
 * <p>
 * The class cannot be instantiated.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class CredentialsLocation {

    /** private constructor, this class cannot be instantiated */
    private CredentialsLocation() {
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
        Object[] values = newCredentialsLocationMap(true).keySet().toArray();
        ComboBoxModel cbm = new DefaultComboBoxModel(values);
        cbm.setSelectedItem("HTTP Basic");
        return cbm;
    }

    /**
     * create the credentials location (http, ws message) combobox model, not including "Anonymous".
     */
    static ComboBoxModel getCredentialsLocationComboBoxModelNonAnonymous(boolean soap) {
        final Map<String, Assertion> map = newCredentialsLocationMap(soap);
        List<Object> newValues = new ArrayList<Object>();

        for (String key : map.keySet()) {
            if(!key.equals("Anonymous")){
                newValues.add(key);
            }
        }

        return new DefaultComboBoxModel(newValues.toArray());
    }

    /**
     * Create and return the new credential location <code>Map<code/>
     * Every time the new map with new credentials location instances.
     * This is because some elements have state that the UI processing
     * may change.
     *
     * @return the new credentials location map
     */
    static Map<String, Assertion> newCredentialsLocationMap(boolean soap) {
        final Map<String, Assertion> credentialsLocationMap = new TreeMap<String, Assertion>();
        credentialsLocationMap.put("Anonymous", new TrueAssertion());

        credentialsLocationMap.put("HTTP Basic", new HttpBasic());
        credentialsLocationMap.put("HTTP Cookie (Single-sign-on token)", new CookieCredentialSourceAssertion());
        credentialsLocationMap.put("SSL or TLS with Client Certificate", new SslAssertion(true));
        // don't add this (see bug 4691)
        //credentialsLocationMap.put("Windows Integrated", new HttpNegotiate() );
        if (soap) {
            credentialsLocationMap.put("WS Token Basic", new WssBasic());
            credentialsLocationMap.put("Encrypted UsernameToken", new EncryptedUsernameTokenAssertion());
            credentialsLocationMap.put("WSS Signature", new RequireWssX509Cert());
            credentialsLocationMap.put("WS Secure Conversation", new SecureConversation());
            // don't add this (see bug 4691)
            //credentialsLocationMap.put("WSS Kerberos", new RequestWssKerberos());
        }

        return credentialsLocationMap;
    }
}
