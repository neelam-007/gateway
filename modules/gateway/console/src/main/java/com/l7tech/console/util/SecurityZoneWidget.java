package com.l7tech.console.util;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Widget that allows selection of a security zone.
 * <p/>
 * Users should call either {@link #reloadZones} or {@link #configure(java.util.Collection, com.l7tech.gateway.common.security.rbac.OperationType, com.l7tech.objectmodel.SecurityZone)}
 * before the widget is displayed.
 */
public class SecurityZoneWidget extends JPanel {
    private static final Logger logger = Logger.getLogger(SecurityZoneWidget.class.getName());
    private static final String ELLIPSIS = "...";
    private static final String MAX_CHAR_NAME_DISPLAY = "max.char.name.display";
    private static ResourceBundle RESOURCES = ResourceBundle.getBundle(SecurityZoneWidget.class.getName());
    private Collection<EntityType> entityTypes = null;
    private boolean zoneLoadAttempted = false;
    private boolean hideIfNoZones = true;
    private OperationType operation;
    private SecurityZone initialZone;
    private Map<Long, SecurityZone> loadedZones = Collections.emptyMap();
    private JComboBox<SecurityZone> zonesComboBox = new JComboBox<>();
    private JLabel securityZoneLabel = new JLabel("Security Zone:");

    public SecurityZoneWidget() {
        setLayout(new GridBagLayout());
        final GridBagConstraints top = new GridBagConstraints();
        top.fill = GridBagConstraints.HORIZONTAL;
        final GridBagConstraints bottom = new GridBagConstraints();
        bottom.gridy = 1;
        bottom.weightx = 1;
        bottom.weighty = 1;
        bottom.fill = GridBagConstraints.HORIZONTAL;
        add(securityZoneLabel, top);
        add(zonesComboBox, bottom);
        zonesComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof SecurityZone) {
                    final SecurityZone securityZone = (SecurityZone) value;
                    final String name = securityZone.getName();
                    final Integer maxChars = getMaxCharsForName();
                    if (maxChars != null && name.length() > maxChars) {
                        value = name.substring(0, maxChars) + ELLIPSIS;
                    } else {
                        value = name;
                    }
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
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
     * Reload the list of zones from the server.
     */
    public void reloadZones() {
        zoneLoadAttempted = true;
        final Object oldSelection = zonesComboBox.getSelectedItem();
        final Map<Long, SecurityZone> readableZones = SecurityZoneUtil.getSortedSecurityZonesAsMap();
        // maintain order of insertion
        loadedZones = new LinkedHashMap<>();
        if (operation == null || operation != OperationType.READ) {
            if (initialZone != null && !initialZone.equals(SecurityZoneUtil.NULL_ZONE)) {
                // want current zone if not readable to be at the top - remove if necessary after other zones are loaded
                loadedZones.put(SecurityZoneUtil.CURRENT_UNAVAILABLE_ZONE.getOid(), SecurityZoneUtil.CURRENT_UNAVAILABLE_ZONE);
            }
            final Collection<Permission> userPermissions = Registry.getDefault().getSecurityProvider().getUserPermissions();
            if (SecurityZoneUtil.isZoneValidForOperation(SecurityZoneUtil.NULL_ZONE, entityTypes, operation, userPermissions)) {
                // want null zone above other zones
                loadedZones.put(SecurityZoneUtil.NULL_ZONE.getOid(), SecurityZoneUtil.NULL_ZONE);
            }
            // readable zones
            for (final SecurityZone readableZone : readableZones.values()) {
                if (SecurityZoneUtil.isZoneValidForOperation(readableZone,  entityTypes, operation, userPermissions)) {
                    loadedZones.put(readableZone.getOid(), readableZone);
                }
            }
            if (initialZone != null && loadedZones.containsKey(initialZone.getOid())) {
                // current zone is readable so remove the current unavailable zone
                loadedZones.remove(SecurityZoneUtil.CURRENT_UNAVAILABLE_ZONE.getOid());
            }
        } else if (initialZone != null) {
            // read only - no need to load all zones into the combo box
            if (readableZones.containsKey(initialZone.getOid())) {
                loadedZones.put(initialZone.getOid(), initialZone);
            } else {
                loadedZones.put(SecurityZoneUtil.CURRENT_UNAVAILABLE_ZONE.getOid(), SecurityZoneUtil.CURRENT_UNAVAILABLE_ZONE);
            }
        }

        zonesComboBox.setModel(new DefaultComboBoxModel<>(loadedZones.values().toArray(new SecurityZone[loadedZones.size()])));
        if (oldSelection != null) {
            zonesComboBox.setSelectedItem(oldSelection);
        } else if (!loadedZones.isEmpty()) {
            // nothing was selected, select the first option
            zonesComboBox.setSelectedIndex(0);
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
        SecurityZone ret = (SecurityZone) zonesComboBox.getSelectedItem();
        if (SecurityZoneUtil.NULL_ZONE == ret) {
            ret = null;
        }
        if (SecurityZoneUtil.CURRENT_UNAVAILABLE_ZONE == ret) {
            // don't change the current zone
            ret = initialZone;
        }
        return ret;
    }

    public void setSelectedZone(@Nullable SecurityZone zone) {
        if (zone == null) {
            zone = SecurityZoneUtil.NULL_ZONE;
        }
        if (loadedZones.containsKey(zone.getOid())) {
            zonesComboBox.setSelectedItem(zone);
        } else {
            // selected zone is not available
            zonesComboBox.setSelectedItem(SecurityZoneUtil.CURRENT_UNAVAILABLE_ZONE);
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

    public void addComboBoxActionListener(@NotNull final ActionListener actionListener) {
        zonesComboBox.addActionListener(actionListener);
    }

    @Nullable
    private Integer getMaxCharsForName() {
        Integer max = null;
        final String maxStr = RESOURCES.getString(MAX_CHAR_NAME_DISPLAY);
        try {
            max = Integer.valueOf(maxStr);
        } catch (final NumberFormatException e) {
            logger.log(Level.WARNING, "Invalid max chars for name: " + maxStr);
        }
        return max;
    }

    private void hideOrDisable() {
        if (loadedZones.isEmpty() && hideIfNoZones) {
            setVisible(false);
        }
        if (OperationType.READ == operation) {
            zonesComboBox.setUI(new BasicComboBoxUI() {
                @Override
                protected JButton createArrowButton() {
                    // remove arrow by creating a plain button
                    return new JButton() {
                        @Override
                        public int getWidth() {
                            return 0;
                        }
                    };
                }
            });
        }
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
}
