package com.l7tech.console.util;

import com.l7tech.console.panels.CipherSuiteListModel;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.widgets.JCheckBoxListModel;
import com.l7tech.util.ConfigFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Utilities for GUI controls that configure TLS cipher suites.
 */
public final class CipherSuiteGuiUtil {

    private static List defaultCipherList;
    private static List visibleCipherList;

    private CipherSuiteGuiUtil() {}

    public static final boolean INCLUDE_ALL_CIPHERS = ConfigFactory.getBooleanProperty( "com.l7tech.console.connector.includeAllCiphers", false );

    /**
     * Set up a group of GUI controls for manipulating enabled TLS cipher suites.
     * <p/>
     * This method creates a CipherSuiteListModel, populating it with all cipher suites known to the Gateway that
     * should be visible, and initially selecting those cipher suites that should be enabled by default.
     * <p/>
     * The provided cipherSuiteList is configured to use the new list model.
     * <p/>
     * Any buttons provided for "Enable Defaults", "Move Up" or "Move Down" will be configured to be enabled
     * only when appropriate, and will have action listeners attached to them that perform the appropriate action
     * when clicked.
     *
     * @param cipherSuiteList  a JList to configure.  Required.
     * @param outbound true if the cipher suites will be used for outbound TLS.  If so, additional SCSV values might be included.
     * @param filterTextField  a text field used to filter the model, or null to disable this functionality
     * @param defaultCipherListButton  a button to use for the "Use Default Ciphers" button, or null to disable this functionality.
     * @param selectNoneButton a button to use for the "Un-check all cipher suites" button, or null to disable this functionality.
     * @param selectAllButton a button to use for the "Check all cipher suites" button, or null to disable this functionality.
     * @param moveUpButton  a button to use for the "Move Up" functionality, or null to disable this functionality.
     * @param moveDownButton  a button to use for the "Move Down" functionality, or null to disable this functionality.
     * @return the new CipherSuiteListModel.  Never null.
     */
    public static CipherSuiteListModel createCipherSuiteListModel(final JList cipherSuiteList, boolean outbound,
                                                                  final @Nullable JTextField filterTextField,
                                                                  final @Nullable JButton defaultCipherListButton,
                                                                  final @Nullable JButton selectNoneButton, final @Nullable JButton selectAllButton,
                                                                  final @Nullable JButton moveUpButton, final @Nullable JButton moveDownButton) {
        final String[] allCiphers = getCipherSuiteNames( outbound );
        final Set<String> defaultCiphers = new LinkedHashSet<String>(Arrays.asList(
                Registry.getDefault().getTransportAdmin().getDefaultCipherSuiteNames()));

        for (String cipher : new ArrayList<>(defaultCiphers)) {
            if (!CipherSuiteGuiUtil.cipherSuiteShouldBeCheckedByDefault(cipher))
                defaultCiphers.remove(cipher);
        }

        final CipherSuiteListModel cipherSuiteListModel = new CipherSuiteListModel(allCiphers, defaultCiphers);

        // Define document listener for Filter Text Field. And, ensure
        // 1. updating the list model with matching items
        // 2. restoring the previous selection if possible
        if (filterTextField != null) {
            final FilterListModel<JCheckBox> filterListModel = new FilterListModel<>(cipherSuiteListModel);
            cipherSuiteListModel.attachToJListViaFilterView(cipherSuiteList, filterListModel);

            TextComponentPauseListenerManager.registerPauseListener(filterTextField, new PauseListenerAdapter() {
                public void textEntryPaused(JTextComponent component, long msecs) {
                    final String filterText = filterTextField.getText();

                    // Matching items will be shown if non-empty filter is specified. Otherwise, all the items will be shown.
                    // (criteria: CONTAINS + CASE INSENSITIVE)
                    filterListModel.setFilter(StringUtils.isEmpty(filterText) ? null :
                            (Filter<JCheckBox>) item -> item.getText().toLowerCase().contains(filterText.toLowerCase()));
                    cipherSuiteList.clearSelection();

                    // find out the last selected entry. If it is part of current filter view, restore the selection.
                    int selectedIndex = filterListModel.getElementIndex(cipherSuiteListModel.getLastSelectedEntry());
                    if (selectedIndex != -1) {
                        cipherSuiteList.setSelectedIndex(selectedIndex);
                    }

                    // update the move-up/down buttons due to filter.
                    enableOrDisableCipherSuiteButtons(cipherSuiteList, cipherSuiteListModel, moveUpButton, moveDownButton);
                }
            }, 100);
        } else {
            cipherSuiteListModel.attachToJList(cipherSuiteList);
        }

        cipherSuiteList.addListSelectionListener(e -> enableOrDisableCipherSuiteButtons(cipherSuiteList, cipherSuiteListModel, moveUpButton, moveDownButton));

        if (defaultCipherListButton != null)
            defaultCipherListButton.addActionListener(createDefaultCipherListActionListener(cipherSuiteListModel, filterTextField));

        if (moveUpButton != null)
            moveUpButton.addActionListener(createMoveUpActionListener(cipherSuiteList, cipherSuiteListModel));

        if (moveDownButton != null)
            moveDownButton.addActionListener(createMoveDownActionListener(cipherSuiteList, cipherSuiteListModel));

        if (selectAllButton != null)
            selectAllButton.addActionListener(createSelectAllCipherSuitesActionListener(cipherSuiteList, cipherSuiteListModel));

        if (selectNoneButton != null)
            selectNoneButton.addActionListener(createSelectNoCipherSuitesActionListener(cipherSuiteList, cipherSuiteListModel));

        // Let's set the move-up/down buttons state. It is better not to enable them if none of the items selected.
        enableOrDisableCipherSuiteButtons(cipherSuiteList, cipherSuiteListModel, moveUpButton, moveDownButton);

        return cipherSuiteListModel;
    }

    public static ActionListener createMoveDownActionListener(final JList cipherSuiteList, final CipherSuiteListModel cipherSuiteListModel) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = cipherSuiteList.getSelectedIndex();
                if (index < 0 || index >= cipherSuiteListModel.getSize() - 1) return;
                int nextIndex = index + 1;
                cipherSuiteListModel.swapEntries(index, nextIndex);
                cipherSuiteList.setSelectedIndex(nextIndex);
                cipherSuiteList.ensureIndexIsVisible(nextIndex);
            }
        };
    }

    public static ActionListener createMoveUpActionListener(final JList cipherSuiteList, final CipherSuiteListModel cipherSuiteListModel) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = cipherSuiteList.getSelectedIndex();
                if (index < 1) return;
                int prevIndex = index - 1;
                cipherSuiteListModel.swapEntries(prevIndex, index);
                cipherSuiteList.setSelectedIndex(prevIndex);
                cipherSuiteList.ensureIndexIsVisible(prevIndex);
            }
        };
    }

    public static ActionListener createDefaultCipherListActionListener(final CipherSuiteListModel cipherSuiteListModel, final JTextField filterTextField) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cipherSuiteListModel.setDefaultCipherList();
                if (filterTextField != null) filterTextField.setText("");
            }
        };
    }

    public static ActionListener createSelectAllCipherSuitesActionListener(final JList cipherSuiteList, final CipherSuiteListModel cipherSuiteListModel) {
        return createSelectCipherSuitesActionListener(cipherSuiteList, cipherSuiteListModel, true);
    }

    public static ActionListener createSelectNoCipherSuitesActionListener(final JList cipherSuiteList, final CipherSuiteListModel cipherSuiteListModel) {
        return createSelectCipherSuitesActionListener(cipherSuiteList, cipherSuiteListModel, false);
    }

    public static ActionListener createSelectCipherSuitesActionListener(final JList cipherSuiteList, final CipherSuiteListModel cipherSuiteListModel, final boolean allSuites) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cipherSuiteListModel.disarm();
                setAllCipherSuitesTo(cipherSuiteList.getModel(), allSuites);
                cipherSuiteList.repaint();
            }
        };
    }

    public static void enableOrDisableCipherSuiteButtons(final JList cipherSuiteList, final CipherSuiteListModel cipherSuiteListModel, final JButton moveUpButton, final JButton moveDownButton) {
        int index = cipherSuiteList.getSelectedIndex();
        ListModel<JCheckBox> model = cipherSuiteList.getModel();
        boolean isFiltered = (model instanceof FilterListModel) && (((FilterListModel<JCheckBox>)model).getFilter() != null);

        // Disable move-up/down buttons if model is filtered
        if (moveUpButton != null)
            moveUpButton.setEnabled(index > 0 && !isFiltered);
        if (moveDownButton != null)
            moveDownButton.setEnabled(index >= 0 && index < cipherSuiteListModel.getSize() - 1 && !isFiltered);
    }

    public static void setAllCipherSuitesTo(ListModel<JCheckBox> model, boolean checked) {
        JCheckBoxListModel.visitEntriesForStateChange(model, (index, entry) -> { return checked; });
    }

    public static String[] getCipherSuiteNames( boolean outbound ) {
        String[] unfiltered = Registry.getDefault().getTransportAdmin().getAllCipherSuiteNames();
        List<String> ret = new ArrayList<String>();

        if ( INCLUDE_ALL_CIPHERS ) {
            ret.addAll( Arrays.asList( unfiltered ) );
        } else {
            for (String name : unfiltered) {
                if (cipherSuiteShouldBeVisible(name))
                    ret.add(name);
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    /**
     * Check if the specified cipher suite should be shown or hidden in the UI.
     * <P/>
     * Currently a cipher suite is shown if is RSA or ECDSA based (or an SCSV), as long as it is neither _WITH_NULL_ (which
     * does no encryption) nor _anon_ (which does no authentication) nor _EXPORT_ (which uses a 40 bit key so weak it is
     * nowadays effectively equivalent to _WITH_NULL_).
     *
     * @param cipherSuiteName the name of an SSL cipher suite to check, ie "TLS_RSA_WITH_AES_128_CBC_SHA".  Required.
     * @return true if this cipher suite should be shown in the UI (regardless of whether it should be checked by default
     *         in new connectors).  False if this cipher suite should be hidden in the UI.
     */
    public static boolean cipherSuiteShouldBeVisible(String cipherSuiteName) {
        List cipherList = getVisibleCipherList();
        if (cipherList != null) {
            return cipherList.contains(cipherSuiteName);
        }
        return false;
    }

    /**
     * Check if the specified cipher suite should be checked by default in newly created listen ports.
     * <p/>
     * Currently we enable by default any cipher suite that is visible in the GUI as long as it isn't RC4 or DES based.
     *
     * @param cipherSuiteName the name of an SSL cipher suite to check, ie "TLS_RSA_WITH_AES_128_CBC_SHA".  Required.
     * @return true if this cipher suite should be checked by default in the UI.
     */
    public static boolean cipherSuiteShouldBeCheckedByDefault(String cipherSuiteName) {
        List cipherList = getDefaultCipherList();
        if (cipherList != null) {
            return cipherList.contains(cipherSuiteName);
        }
        return false;
    }

    private static List getDefaultCipherList() {
        if (defaultCipherList == null && Registry.getDefault().isAdminContextPresent()) {
            defaultCipherList = Arrays.asList(Registry.getDefault().getTransportAdmin().getDefaultCipherSuiteNames());

        }
        return defaultCipherList;
    }

    private static List getVisibleCipherList() {
        if (visibleCipherList == null && Registry.getDefault().isAdminContextPresent()) {
            visibleCipherList = Arrays.asList(Registry.getDefault().getTransportAdmin().getVisibleCipherSuiteNames());
        }
        return visibleCipherList;
    }

    /**
     * Check if the specified TLS version is unsupported by the current Gateway node's default TLS provider.
     *
     * @param selectedTlsVersion a TLS version to check, ie "TLSv1" or "TLSv1.2".  Required.
     * @return  false if the specified TLS version is definitely not supported by the default TLS provider.  True if it may be supported.
     */
    public static boolean isSupportedTlsVersion(Object selectedTlsVersion) {
        if (!Registry.getDefault().isAdminContextPresent())
            return true;
        Set<String> protos = new HashSet<String>(Arrays.asList(Registry.getDefault().getTransportAdmin().getAllProtocolVersions(true)));
        return protos.contains(String.valueOf(selectedTlsVersion));
    }
}
