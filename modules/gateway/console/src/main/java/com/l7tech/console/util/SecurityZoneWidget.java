package com.l7tech.console.util;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Widget that allows selection of a security zone.
 * <p/>
 * Users should call either {@link #reloadZones()} or {@link #setEntityType(com.l7tech.objectmodel.EntityType)}
 * before the widget is displayed.
 */
public class SecurityZoneWidget extends JComboBox<SecurityZone> {
    private EntityType entityType = null;
    private boolean zoneLoadAttempted = false;
    private boolean hideIfNoZones = true;

    /**
     * Semaphore used internally that represents a null security zone.
     */
    public static final SecurityZone NULL_ZONE = new SecurityZone() {
        {
            setOid(-1);
            setName("<None>");
            setDescription("%%%NULL_ZONE%%%");
        }
    };

    java.util.List<SecurityZone> loadedZones = Collections.emptyList();

    public SecurityZoneWidget() {
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

    /**
     * Setting an entity type filters the presented zone list to only those zones which permit the specified
     * entity type (and, of course, which are readable by the connected admin in the first place).
     *
     * @param entityType entity type to filter, or null to show zone list unfiltered by zone entity type restrictions.
     */
    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
        reloadZones();
    }

    /**
     * Reload the list of zones from the server.
     */
    public void reloadZones() {
        zoneLoadAttempted = true;
        Object oldSelection = getSelectedItem();

        Set<SecurityZone> zones = new HashSet<>(SecurityZoneUtil.getSecurityZones());

        if (zones.isEmpty() && hideIfNoZones)
            setVisible(false);

        if (entityType != null) {
            Set<SecurityZone> permittedZones = new HashSet<>();
            for (SecurityZone zone : zones) {
                if (zone.permitsEntityType(entityType))
                    permittedZones.add(zone);
            }
            zones = permittedZones;
        }

        loadedZones = new ArrayList<>();
        loadedZones.add(NULL_ZONE);
        loadedZones.addAll(zones);
        setModel(new DefaultComboBoxModel<>(loadedZones.toArray(new SecurityZone[loadedZones.size()])));

        setSelectedItem(oldSelection);
    }

    @Override
    public void paint(Graphics g) {
        if (!zoneLoadAttempted) {
            reloadZones();
        }
        super.paint(g);
    }

    @Nullable
    public SecurityZone getSelectedZone() {
        SecurityZone ret = (SecurityZone)getSelectedItem();
        if (NULL_ZONE == ret)
            ret = null;
        return ret;
    }

    public void setSelectedZone(@Nullable SecurityZone zone) {
        if (zone == null)
            zone = NULL_ZONE;

        setSelectedItem(zone);

        if (!zone.equals(getSelectedItem())) {
            // Wasn't present, need to add it
            loadedZones.add(zone);
            setModel(new DefaultComboBoxModel<>(loadedZones.toArray(new SecurityZone[loadedZones.size()])));
            setSelectedItem(zone);
        }
    }

    /**
     * @return true if the widget will setVisible(false) on itself when the zone list is reloaded
     *              if there are no security zones present (or readable by the current admin).
     */
    public boolean isHideIfNoZones() {
        return hideIfNoZones;
    }

    /**
     * @param hideIfNoZones true if the widget should setVisible(false) on itself when the zone list is reloaded
     *                      if there are no security zones present (or readable by the current admin).
     */
    public void setHideIfNoZones(boolean hideIfNoZones) {
        this.hideIfNoZones = hideIfNoZones;
    }
}
