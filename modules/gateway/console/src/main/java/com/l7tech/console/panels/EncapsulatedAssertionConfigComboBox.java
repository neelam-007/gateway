package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ComboBox that can be used to choose an available EncapsulatedAssertionConfig.
 */
public class EncapsulatedAssertionConfigComboBox extends JComboBox<EncapsulatedAssertionConfig> {
    private static Logger logger = Logger.getLogger(EncapsulatedAssertionConfigComboBox.class.getName());
    private List<EncapsulatedAssertionConfig> encapsulatedAssertionConfigs;

    public EncapsulatedAssertionConfigComboBox() {
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    setText(((EncapsulatedAssertionConfig) value).getName());
                }
                return this;
            }
        });
        reload();
    }

    public void reload() {
        try {
            final EncapsulatedAssertionConfig selectedEncapsulatedAssertionConfig = getSelectedEncapsulatedAssertion();

            encapsulatedAssertionConfigs = new ArrayList<>(Registry.getDefault().getEncapsulatedAssertionAdmin().findAllEncapsulatedAssertionConfigs());
            Collections.sort(encapsulatedAssertionConfigs, new ResolvingComparator<>(new Resolver<EncapsulatedAssertionConfig, String>() {
                @Override
                public String resolve(final EncapsulatedAssertionConfig key) {
                    return key.getName().toLowerCase();
                }
            }, false));
            setModel(new DefaultComboBoxModel<>(encapsulatedAssertionConfigs.toArray(new EncapsulatedAssertionConfig[encapsulatedAssertionConfigs.size()])));

            if (selectedEncapsulatedAssertionConfig != null && containsItem(selectedEncapsulatedAssertionConfig.getGoid())) {
                // Select currently selected item.
                setSelectedEncapsulatedAssertionConfig(selectedEncapsulatedAssertionConfig.getGoid());
            }
        } catch (FindException e) {
            final String msg = "Unable to list encapsulated assertions: " + ExceptionUtils.getMessage(e);
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            DialogDisplayer.showMessageDialog(this, "Unable to list encapsulated assertions", msg, e);
        }
    }

    public EncapsulatedAssertionConfig getSelectedEncapsulatedAssertion() {
        return (EncapsulatedAssertionConfig) getSelectedItem();
    }

    /**
     * @param goid the goid of the EncapsulatedAssertionConfig to select in the dropdown or
     *             #EncapsulatedAssertionConfig.DEFAULT_GOID if none should be selected.
     *             If the goid does not correspond to an available EncapsulatedAssertionConfig, the selected item will
     *             be shown as 'encapsulated assertions details are unavailable'.
     */
    public void setSelectedEncapsulatedAssertionConfig(Goid goid) {
        Integer selectedIndex = null;
        for (int i = 0; i < encapsulatedAssertionConfigs.size(); i++) {
            EncapsulatedAssertionConfig encapsulatedAssertionConfig = encapsulatedAssertionConfigs.get(i);
            if (Goid.equals(goid, encapsulatedAssertionConfig.getGoid())) {
                selectedIndex = i;
                break;
            }
        }
        if (selectedIndex != null) {
            setSelectedIndex(selectedIndex);
        } else if (!Goid.isDefault(goid)) {
            // oid not found in available encapsulated assertions - could be not readable by current user
            logger.log(Level.WARNING, "Encapsulated Assertion goid not available to current user");
            final EncapsulatedAssertionConfig unavailableEncapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
            unavailableEncapsulatedAssertionConfig.setGoid(goid);
            unavailableEncapsulatedAssertionConfig.setName("Current encapsulated assertions (encapsulated assertions details are unavailable)");
            encapsulatedAssertionConfigs.add(0, unavailableEncapsulatedAssertionConfig);
            setModel(new DefaultComboBoxModel<>(encapsulatedAssertionConfigs.toArray(new EncapsulatedAssertionConfig[encapsulatedAssertionConfigs.size()])));
            setSelectedIndex(0);
        } else {
            // encapsulated assertion does not yet exist in the database
            setSelectedItem(null);
        }
    }

    public boolean containsItem(Goid goid) {
        for (EncapsulatedAssertionConfig EncapsulatedAssertionConfig : encapsulatedAssertionConfigs) {
            if (Goid.equals(EncapsulatedAssertionConfig.getGoid(), goid)) {
                return true;
            }
        }

        return false;
    }
}