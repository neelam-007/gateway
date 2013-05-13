package com.l7tech.console.security.rbac;

import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * A reusable panel, suitable for use with OkCancelDialog, that shows the SecurityZoneWidget.
 * <p/>
 * Since ValidatedPanel can't return null as a valid value,
 * this panel will return {@link SecurityZoneUtil#NULL_ZONE}
 * as the value if the user selects "none" as the zone.
 */
public class SecurityZoneValidatedPanel extends ValidatedPanel<SecurityZone> {
    private SecurityZoneWidget zoneWidget;

    public SecurityZoneValidatedPanel(EntityType entityType, SecurityZone zone, @Nullable OperationType operation) {
        init();
        zoneWidget.configure(entityType, operation, zone);
        checkSyntax();
    }

    @Override
    protected SecurityZone getModel() {
        if (zoneWidget == null)
            return null;
        SecurityZone zone = zoneWidget.getSelectedZone();
        if (zone == null)
            zone = SecurityZoneUtil.NULL_ZONE;
        return zone;
    }

    @Override
    protected void initComponents() {
        zoneWidget = new SecurityZoneWidget();
        setMinimumSize(new Dimension(260, -1));
        setLayout(new BorderLayout());
        add(zoneWidget, BorderLayout.CENTER);
        zoneWidget.addActionListener(syntaxListener());
    }

    @Override
    public void focusFirstComponent() {
        if (zoneWidget != null)
            zoneWidget.requestFocusInWindow();
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
        return zone == null || zone == SecurityZoneUtil.NULL_ZONE;
    }
}
