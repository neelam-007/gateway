package com.l7tech.console.security.rbac;

import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.ZoneableEntity;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * A reusable panel, suitable for use with OkCancelDialog, that shows the SecurityZoneWidget.
 * <p/>
 * Since ValidatedPanel can't return null as a valid value,
 * this panel will return {@link SecurityZoneUtil#getNullZone()}
 * as the value if the user selects "none" as the zone.
 */
public class SecurityZoneValidatedPanel extends ValidatedPanel<SecurityZone> {
    private SecurityZoneWidget zoneWidget;

    public SecurityZoneValidatedPanel(@NotNull final ZoneableEntity entity) {
        init();
        zoneWidget.configure(entity);
        checkSyntax();
    }

    @Override
    protected SecurityZone getModel() {
        if (zoneWidget == null)
            return null;
        SecurityZone zone = zoneWidget.getSelectedZone();
        if (zone == null)
            zone = SecurityZoneUtil.getNullZone();
        return zone;
    }

    @Override
    protected void initComponents() {
        zoneWidget = new SecurityZoneWidget();
        setMinimumSize(new Dimension(260, -1));
        setLayout(new BorderLayout());
        add(zoneWidget, BorderLayout.CENTER);
        zoneWidget.addComboBoxActionListener(syntaxListener());
    }

    @Override
    public void focusFirstComponent() {
        if (zoneWidget != null)
            zoneWidget.requestFocusInWindow();
    }

    /**
     * @return true if there is at least one zone available which is not the 'null zone'.
     */
    public boolean hasZones() {
        return zoneWidget.hasZones();
    }

    @Override
    protected void doUpdateModel() {
    }

    /**
     * Check if the specified zone returns a null security zone.
     *
     * @param zone the zone to check.
     * @return true if the zone is null or is SecurityZoneWidget.NULL_ZONE
     */
    public static boolean isNull(SecurityZone zone) {
        return zone == null || zone.equals(SecurityZoneUtil.getNullZone());
    }
}
