package com.l7tech.console.util;

import com.l7tech.console.panels.CipherSuiteListModel;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Utilities for GUI controls that configure TLS cipher suites.
 */
public final class CipherSuiteGuiUtil {

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
     * @param defaultCipherListButton  a button to use for the "Use Default Ciphers" button, or null to disable this functionality.
     * @param selectNoneButton a button to use for the "Un-check all cipher suites" button, or null to disable this functionality.
     * @param selectAllButton a button to use for the "Check all cipher suites" button, or null to disable this functionality.
     * @param moveUpButton  a button to use for the "Move Up" functionality, or null to disable this functionality.
     * @param moveDownButton  a button to use for the "Move Down" functionality, or null to disable this functionality.
     * @return the new CipherSuiteListModel.  Never null.
     */
    public static CipherSuiteListModel createCipherSuiteListModel(final JList cipherSuiteList, boolean outbound,
            final @Nullable JButton defaultCipherListButton, final @Nullable JButton selectNoneButton, final @Nullable JButton selectAllButton, final @Nullable JButton moveUpButton, final @Nullable JButton moveDownButton) {
        String[] allCiphers = getCipherSuiteNames( outbound );
        Set<String> defaultCiphers = new LinkedHashSet<String>(Arrays.asList(
                Registry.getDefault().getTransportAdmin().getDefaultCipherSuiteNames()));
        for (String cipher : new ArrayList<String>(defaultCiphers)) {
            if (!CipherSuiteGuiUtil.cipherSuiteShouldBeCheckedByDefault(cipher))
                defaultCiphers.remove(cipher);
        }
        final CipherSuiteListModel cipherSuiteListModel = new CipherSuiteListModel(allCiphers, defaultCiphers);
        cipherSuiteListModel.attachToJList(cipherSuiteList);
        cipherSuiteList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableCipherSuiteButtons(cipherSuiteList, cipherSuiteListModel, moveUpButton, moveDownButton);
            }
        });

        if (defaultCipherListButton != null)
            defaultCipherListButton.addActionListener(createDefaultCipherListActionListener(cipherSuiteListModel));

        if (moveUpButton != null)
            moveUpButton.addActionListener(createMoveUpActionListener(cipherSuiteList, cipherSuiteListModel));

        if (moveDownButton != null)
            moveDownButton.addActionListener(createMoveDownActionListener(cipherSuiteList, cipherSuiteListModel));

        if (selectAllButton != null)
            selectAllButton.addActionListener(createSelectAllActionListener(cipherSuiteListModel));

        if (selectNoneButton != null)
            selectNoneButton.addActionListener(createSelectNoneActionListener(cipherSuiteListModel));

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

    public static ActionListener createDefaultCipherListActionListener(final CipherSuiteListModel cipherSuiteListModel) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cipherSuiteListModel.setDefaultCipherList();
            }
        };
    }

    public static ActionListener createSelectNoneActionListener(final CipherSuiteListModel cipherSuiteListModel) {
        return createSetAllCheckBoxesActionListener(cipherSuiteListModel, false);
    }

    public static ActionListener createSelectAllActionListener(CipherSuiteListModel cipherSuiteListModel) {
        return createSetAllCheckBoxesActionListener(cipherSuiteListModel, true);
    }

    private static ActionListener createSetAllCheckBoxesActionListener(final CipherSuiteListModel cipherSuiteListModel, final boolean desiredState) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cipherSuiteListModel.visitEntries(new Functions.Binary<Boolean, Integer, JCheckBox>() {
                    @Override
                    public Boolean call(Integer integer, JCheckBox jCheckBox) {
                        return desiredState;
                    }
                });
            }
        };
    }

    public static void enableOrDisableCipherSuiteButtons(final JList cipherSuiteList, final CipherSuiteListModel cipherSuiteListModel, final JButton moveUpButton, final JButton moveDownButton) {
        int index = cipherSuiteList.getSelectedIndex();
        if (moveUpButton != null)
            moveUpButton.setEnabled(index > 0);
        if (moveDownButton != null)
            moveDownButton.setEnabled(index >= 0 && index < cipherSuiteListModel.getSize() - 1);
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

        if ( outbound )
            ret.add( "TLS_EMPTY_RENEGOTIATION_INFO_SCSV" );

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
        return !cipherSuiteName.contains("_WITH_NULL_") && !cipherSuiteName.contains("_anon_") && !cipherSuiteName.contains("_EXPORT_") && (
                cipherSuiteName.contains("_RSA_") ||
                cipherSuiteName.contains("_ECDSA_") ||
                cipherSuiteName.contains("_SCSV")
        );
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
        return cipherSuiteShouldBeVisible(cipherSuiteName) &&
                !cipherSuiteName.contains( "_WITH_RC4_" ) &&
                !cipherSuiteName.contains( "_WITH_DES_" );
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
