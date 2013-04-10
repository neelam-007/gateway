package com.l7tech.console.util;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Widget that allows selection of a security zone.
 */
public class SecurityZoneWidget extends JComboBox<SecurityZone> {
    private static final Logger logger = Logger.getLogger(SecurityZoneWidget.class.getName());

    public SecurityZoneWidget() {
        if (Registry.getDefault().isAdminContextPresent()) {
            reloadZones();
        }
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                if (value instanceof SecurityZone) {
                    SecurityZone securityZone = (SecurityZone) value;
                    value = securityZone.getName();
                }

                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        setBorder(new LineBorder(new Color(150, 30, 0), 2, false));
    }

    public void reloadZones() {
        Collection<SecurityZone> zones;
        try {
            zones = Registry.getDefault().getRbacAdmin().findAllSecurityZones();
        } catch (FindException e) {
            String msg = "Unable to load list of security zones: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, e);
            zones = Collections.emptyList();
        }

        setModel(new DefaultComboBoxModel<SecurityZone>(zones.toArray(new SecurityZone[zones.size()])));
    }

    public SecurityZone getSelectedZone() {
        return (SecurityZone)getSelectedItem();
    }

    public void setSelectedZone(SecurityZone zone) {
        setSelectedItem(zone);
    }
}
