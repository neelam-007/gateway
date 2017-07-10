package com.l7tech.console.panels;

import com.l7tech.console.util.FilterListModel;
import com.l7tech.gui.widgets.JCheckBoxListModel;
import com.l7tech.gui.widgets.JCheckBoxListModelAware;
import com.l7tech.util.ArrayUtils;
import org.jetbrains.annotations.NotNull;

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

    /**
     * Refers to the last selected entry in the view across filter views
     */
    private JCheckBox lastSelectedEntry;

    public CipherSuiteListModel(String[] allCiphers, Set<String> defaultCiphers) {
        super(new ArrayList<>());
        this.allCiphers = ArrayUtils.copy(allCiphers);
        this.defaultCiphers = new LinkedHashSet<>(defaultCiphers);
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
     * Get the code name for the specified entry.
     *
     * @param entry  one of the checkbox list entries.  Required.
     * @return the code name for this entry, ie "SSL_RSA_WITH_3DES_EDE_CBC_SHA".
     */
    private static String getEntryCode(JCheckBox entry) {
        Object code = entry.getClientProperty(CLIENT_PROPERTY_ENTRY_CODE);
        return code != null ? code.toString() : entry.getText();
    }

    private String buildEntryCodeString() {
        StringBuilder ret = new StringBuilder(128);
        boolean isFirst = true;

        for (int index = 0; index < getSize(); index++) {
            final JCheckBox entry = getElementAt(index);

            if (entry.isSelected()) {
                if (!isFirst) ret.append(',');
                ret.append(getEntryCode(entry));
                isFirst = false;
            }
        }

        return ret.toString();
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
        List<JCheckBox> entries = new ArrayList<>();

        for (String cipher : enabled) {
            entries.add(new JCheckBox(cipher, true));
        }

        for (String cipher : all) {
            if (!enabled.contains(cipher))
                entries.add(new JCheckBox(cipher, false));
        }

        setEntries(entries);
    }

    /**
     * Reset the cipher list to the defaults.
     */
    public void setDefaultCipherList() {
        List<JCheckBox> entries = new ArrayList<>();

        for (String cipher : allCiphers) {
            entries.add(new JCheckBox(cipher, defaultCiphers.contains(cipher)));
        }

        setEntries(entries);
    }

    /**
     * Get last selected entry from the list across the multiple filter views.
     * It helps to restore the previous selection due to filter.
     * @return Last selected JCheckBox entry in the list
     */
    public JCheckBox getLastSelectedEntry() {
        return lastSelectedEntry;
    }

    /**
     * Configure the specified JList to use this as its list model via specified filter list model.
     * <p/>
     * This will set the list model, the cell renderer, and the selection model.
     *
     * @param jList the JList to configure.  Required.
     * @param filterListModel filter list model
     */
    public void attachToJListViaFilterView(final JList jList, final FilterListModel<JCheckBox> filterListModel) {
        JCheckBoxListModel.attachToJList(jList, filterListModel, createFilterListModelAware(filterListModel));

        // Keep track of the last entry selection
        jList.addListSelectionListener(e -> {
            JCheckBox selectedEntry = (JCheckBox) jList.getSelectedValue();

            if (selectedEntry != null) {
                lastSelectedEntry = selectedEntry;
            }
        });
    }

    /**
     * Creates the filter list model aware instance using the filter model
     * @param filterListModel
     * @return
     */
    private JCheckBoxListModelAware createFilterListModelAware(@NotNull final FilterListModel<JCheckBox> filterListModel) {
        return new JCheckBoxListModelAware() {
            @Override
            public void swapEntries(int index1, int index2) {
                CipherSuiteListModel.this.swapEntries(filterListModel.getOriginalIndex(index1), filterListModel.getOriginalIndex(index2));
            }

            @Override
            public void arm(int index) {
                CipherSuiteListModel.this.arm(filterListModel.getOriginalIndex(index));
            }

            @Override
            public void disarm() {
                CipherSuiteListModel.this.disarm();
            }

            @Override
            public void toggle(int index) {
                CipherSuiteListModel.this.toggle(filterListModel.getOriginalIndex(index));
            }
        };
    }
}
