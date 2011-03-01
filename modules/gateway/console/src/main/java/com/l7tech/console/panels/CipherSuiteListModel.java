package com.l7tech.console.panels;

import com.l7tech.gui.widgets.JCheckBoxListModel;
import com.l7tech.util.ArrayUtils;

import javax.swing.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A list model for a JCheckBoxList that keeps track of cipher suites.
 */
public class CipherSuiteListModel extends JCheckBoxListModel {
    public static final Pattern WS_COMMA_WS = Pattern.compile("\\s*,\\s*");


    private final String[] allCiphers;
    private final Set<String> defaultCiphers;

    public CipherSuiteListModel(String[] allCiphers, Set<String> defaultCiphers) {
        super(new ArrayList<JCheckBox>());
        this.allCiphers = ArrayUtils.copy(allCiphers);
        this.defaultCiphers = new LinkedHashSet<String>(defaultCiphers);
    }

    /**
     * @return cipher list string corresponding to all checked cipher names in order, comma delimited, ie.
     *         "TLS_RSA_WITH_AES_128_CBC_SHA, SSL_RSA_WITH_3DES_EDE_CBC_SHA", or null if the default
     *         cipher list is in use.
     */
    public String asCipherListStringOrNullIfDefault() {
        String defaultList = buildDefaultCipherListString();
        String ourList = buildEntryCodeString();
        return defaultList.equals(ourList) ? null : ourList;
    }

    /**
     * @return cipher list string corresponding to all checked cipher names in order, comma delimited, ie.
     *         "TLS_RSA_WITH_AES_128_CBC_SHA, SSL_RSA_WITH_3DES_EDE_CBC_SHA".  Never null, but may be empty
     *         if no cipher suites are selected.
     */
    public String asCipherListStringNotNull() {
        return buildEntryCodeString();
    }

    private String buildDefaultCipherListString() {
        StringBuilder ret = new StringBuilder(128);
        boolean isFirst = true;
        for (String cipher : allCiphers) {
            if (defaultCiphers.contains(cipher)) {
                if (!isFirst) ret.append(',');
                ret.append(cipher);
                isFirst = false;
            }
        }
        return ret.toString();
    }

    /**
     * Populate the list model from the specified cipher list string.
     * This will first build a master list of ciphers by appending any missing ciphers from allCiphers
     * to the end of the provided cipherList, then marking as "checked" only those ciphers that were present
     * in cipherList.
     *
     * @param cipherList a cipher list string, ie "TLS_RSA_WITH_AES_128_CBC_SHA, SSL_RSA_WITH_3DES_EDE_CBC_SHA",
     *                   or null to just use the default cipher list.
     */
    public void setCipherListString(String cipherList) {
        if (cipherList == null) {
            setDefaultCipherList();
            return;
        }

        Set<String> enabled = new LinkedHashSet<String>(Arrays.asList(WS_COMMA_WS.split(cipherList)));
        Set<String> all = new LinkedHashSet<String>(Arrays.asList(allCiphers));
        List<JCheckBox> entries = getEntries();
        entries.clear();
        for (String cipher : enabled) {
            entries.add(new JCheckBox(cipher, true));
        }
        for (String cipher : all) {
            if (!enabled.contains(cipher))
                entries.add(new JCheckBox(cipher, false));
        }
    }

    /**
     * Reset the cipher list to the defaults.
     */
    public void setDefaultCipherList() {
        List<JCheckBox> entries = getEntries();
        int oldsize = entries.size();
        entries.clear();
        for (String cipher : allCiphers)
            entries.add(new JCheckBox(cipher, defaultCiphers.contains(cipher)));
        fireContentsChanged(this, 0, Math.max(oldsize, entries.size()));
    }
}
