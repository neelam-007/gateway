package com.l7tech.console.util;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.*;

/**
 * Widget that allows selection of a security zone.
 * <p/>
 * Users should call either {@link #reloadZones} or {@link #configure(java.util.Collection, com.l7tech.gateway.common.security.rbac.OperationType, com.l7tech.objectmodel.SecurityZone)}
 * before the widget is displayed.
 */
public class SecurityZoneWidget extends JComboBox<SecurityZone> {
    private Collection<EntityType> entityTypes = null;
    private boolean zoneLoadAttempted = false;
    private boolean hideIfNoZones = true;
    private OperationType operation;
    private SecurityZone initialZone;
    private java.util.List<SecurityZone> loadedZones = Collections.emptyList();

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
     * Configure this widget. Configuration will determine which SecurityZones are available to the user.
     *
     * @param entityTypes entity types to filter, or null to show zone list unfiltered by zone entity type restrictions.
     * @param operation   the OperationType that this widget is being used for.
     *                    This will affect the SecurityZones available to the user for selection.
     *                    Default is null which will not filter any SecurityZones based on user permissions.
     * @param initialZone the initial SecurityZone to select in the combo box. If null and not OperationType.READ, the first available SecurityZone will be selected.
     */
    public void configure(@Nullable final Collection<EntityType> entityTypes, @Nullable final OperationType operation, @Nullable SecurityZone initialZone) {
        this.operation = operation;
        setEntityTypes(entityTypes);
        this.initialZone = initialZone == null ? SecurityZoneUtil.NULL_ZONE : initialZone;
        reloadZones();
        setSelectedZone(initialZone);
    }

    /**
     * @see #configure(java.util.Collection, com.l7tech.gateway.common.security.rbac.OperationType, com.l7tech.objectmodel.SecurityZone)
     */
    public void configure(@Nullable final EntityType entityType, @Nullable final OperationType operation, @Nullable SecurityZone initialZone) {
        configure(Collections.singletonList(entityType), operation, initialZone);
    }

    /**
     * Setting entity types filters the presented zone list to only those zones which permit all of the specified
     * entity types.
     *
     * @param entityTypes entity types to filter, or null to show zone list unfiltered by zone entity type restrictions.
     */
    private void setEntityTypes(@Nullable final Collection<EntityType> entityTypes) {
        if (entityTypes != null) {
            this.entityTypes = new ArrayList<EntityType>(entityTypes);
        } else {
            this.entityTypes = null;
        }
    }

    /**
     * Reload the list of zones from the server.
     */
    public void reloadZones() {
        zoneLoadAttempted = true;
        final Object oldSelection = getSelectedItem();
        loadedZones = new ArrayList<>();
        if (operation == null || operation != OperationType.READ) {
            // all readable zones
            final Set<SecurityZone> readableZones = SecurityZoneUtil.getSecurityZones();
            Set<SecurityZone> zones = new HashSet<>(readableZones.size() + 1);
            zones.add(SecurityZoneUtil.NULL_ZONE);
            zones.addAll(readableZones);

            final Set<SecurityZone> invalidZones = new HashSet<>();
            for (final SecurityZone zone : zones) {
                if (!SecurityZoneUtil.isZoneValidForOperation(zone, entityTypes, operation, Registry.getDefault().getSecurityProvider().getUserPermissions())) {
                    invalidZones.add(zone);
                }
            }
            zones.removeAll(invalidZones);

            loadedZones.addAll(zones);

        } else if (initialZone != null) {
            loadedZones.add(initialZone);
        }

        setModel(new DefaultComboBoxModel<>(loadedZones.toArray(new SecurityZone[loadedZones.size()])));
        if (oldSelection != null) {
            setSelectedItem(oldSelection);
        } else if (!loadedZones.isEmpty()) {
            // nothing was selected, select the first option
            setSelectedIndex(0);
        }
    }

    private void hideOrDisable() {
        if (loadedZones.isEmpty() && hideIfNoZones) {
            setVisible(false);
        }
        if (OperationType.READ == operation) {
            setEnabled(false);
        }
    }

    @Override
    public void paint(Graphics g) {
        if (!zoneLoadAttempted) {
            reloadZones();
        }
        hideOrDisable();
        super.paint(g);
    }

    /**
     * @return the selected security zone, or null if the selected zone is "&lt;None&gt;".
     */
    @Nullable
    public SecurityZone getSelectedZone() {
        SecurityZone ret = (SecurityZone) getSelectedItem();
        if (SecurityZoneUtil.NULL_ZONE == ret)
            ret = null;
        return ret;
    }

    public void setSelectedZone(@Nullable SecurityZone zone) {
        if (zone == null){
            zone = SecurityZoneUtil.NULL_ZONE;
        }
        if (loadedZones.contains(zone)) {
            setSelectedItem(zone);
        }
    }

    /**
     * @return true if the widget will setVisible(false) on itself when the zone list is reloaded
     *         if there are no security zones present (or readable by the current admin).
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
