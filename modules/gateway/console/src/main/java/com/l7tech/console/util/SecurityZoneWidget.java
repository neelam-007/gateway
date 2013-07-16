package com.l7tech.console.util;

import com.l7tech.console.security.SecurityProvider;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.ZoneableEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Widget that allows selection of a security zone.
 * <p/>
 * Users should call either {@link #reloadZones} or {@link #configure(java.util.Collection, com.l7tech.gateway.common.security.rbac.OperationType, com.l7tech.objectmodel.SecurityZone)}
 * before the widget is displayed.
 */
public class SecurityZoneWidget extends JPanel {
    private static final Logger logger = Logger.getLogger(SecurityZoneWidget.class.getName());
    private static final String MAX_CHAR_NAME_DISPLAY = "max.char.name.display";
    private static ResourceBundle RESOURCES = ResourceBundle.getBundle(SecurityZoneWidget.class.getName());
    private Collection<EntityType> entityTypes = null;
    private boolean zoneLoadAttempted = false;
    private boolean hideIfNoZones = true;
    private OperationType operation;
    private SecurityZone initialZone;
    private java.util.List<SecurityZone> loadedZones = Collections.emptyList();
    private JComboBox<SecurityZone> zonesComboBox = new JComboBox<>();
    private JLabel securityZoneLabel = new JLabel("Security Zone:");
    private ZoneableEntity specificEntity;

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
                    value = SecurityZoneUtil.getSecurityZoneName((SecurityZone) value, SecurityZoneUtil.getIntFromResource(RESOURCES, MAX_CHAR_NAME_DISPLAY));
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
        this.initialZone = initialZone == null ? SecurityZoneUtil.getNullZone() : initialZone;
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
     * Configure the widget for a specific ZoneableEntity. Configuration will determine which SecurityZones are available to the user.
     *
     * @param operation the operation being performed on the ZoneableEntity.
     * @param entity    the ZoneableEntity being operated on.
     */
    public void configure(@NotNull final OperationType operation, @NotNull final ZoneableEntity entity) {
        this.specificEntity = entity;
        configure(EntityType.findTypeByEntity(entity.getClass()), operation, entity.getSecurityZone());
    }

    /**
     * Reload the list of zones from the server.
     */
    public void reloadZones() {
        zoneLoadAttempted = true;
        final Object oldSelection = zonesComboBox.getSelectedItem();
        loadedZones = new ArrayList<>();
        final SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();

        if (operation == null) {
            // show all readable
            loadedZones.addAll(SecurityZoneUtil.getSortedZonesForOperationAndEntityType(null, entityTypes));
        } else if (operation == OperationType.READ) {
            // show the zone set on the entity if possible
            if (initialZone.equals(SecurityZoneUtil.getNullZone()) || SecurityZoneUtil.getSortedReadableSecurityZones().contains(initialZone)) {
                loadedZones.add(initialZone);
            }
        } else if (operation == OperationType.UPDATE) {
            if (specificEntity != null) {
                // check security provider for each readable zone
                final EntityType type = EntityType.findTypeByEntity(specificEntity.getClass());

                // test each zone to see if it can be set on the entity
                final SecurityZone originalZone = specificEntity.getSecurityZone();
                specificEntity.setSecurityZone(null);
                if (securityProvider.hasPermission(new AttemptedUpdate(type, specificEntity))) {
                    loadedZones.add(SecurityZoneUtil.getNullZone());
                }
                for (final SecurityZone zone : SecurityZoneUtil.getSortedReadableSecurityZones()) {
                    if (zone.permitsEntityType(type)) {
                        specificEntity.setSecurityZone(zone);
                        if (securityProvider.hasPermission(new AttemptedUpdate(type, specificEntity))) {
                            loadedZones.add(zone);
                        }
                    }
                }
                specificEntity.setSecurityZone(originalZone);
            } else {
                // guess by analyzing permissions
                loadedZones.addAll(SecurityZoneUtil.getSortedZonesForOperationAndEntityType(operation, entityTypes));
            }
        } else {
            // guess by analyzing permissions
            loadedZones.addAll(SecurityZoneUtil.getSortedZonesForOperationAndEntityType(operation, entityTypes));
        }

        if (initialZone != null && !initialZone.equals(SecurityZoneUtil.getNullZone()) && !loadedZones.contains(initialZone)) {
            // user can't read the currently set zone
            loadedZones.add(0, SecurityZoneUtil.getCurrentUnavailableZone());
        }

        zonesComboBox.setModel(new DefaultComboBoxModel<>(loadedZones.toArray(new SecurityZone[loadedZones.size()])));
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
        if (SecurityZoneUtil.getNullZone().equals(ret)) {
            ret = null;
        }
        if (SecurityZoneUtil.getCurrentUnavailableZone().equals(ret)) {
            // don't change the current zone
            ret = initialZone;
        }
        return ret;
    }

    public void setSelectedZone(@Nullable SecurityZone zone) {
        if (zone == null) {
            zone = SecurityZoneUtil.getNullZone();
        }
        if (loadedZones.contains(zone)) {
            zonesComboBox.setSelectedItem(zone);
        } else {
            // selected zone is not available
            zonesComboBox.setSelectedItem(SecurityZoneUtil.getCurrentUnavailableZone());
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

    /**
     * Also enables/disables child components.
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        zonesComboBox.setEnabled(enabled);
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
