package com.l7tech.console.action;

import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.security.rbac.SecurityZoneValidatedPanel;
import com.l7tech.console.util.EntitySaver;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.AttemptedReadSpecific;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.ZoneableEntity;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An action that, when invoked, will configure the security zone for an entity.
 */
public class ConfigureSecurityZoneAction<ET extends ZoneableEntity> extends SecureAction {
    private static final Logger logger = Logger.getLogger(ConfigureSecurityZoneAction.class.getName());

    private ET entity;
    private final EntityType entityType;
    private final EntitySaver<ET> entitySaver;

    public ConfigureSecurityZoneAction(@NotNull ET entity, @NotNull EntitySaver<ET> saver) {
        super(null, UI_MANAGE_SECURITY_ZONES);
        this.entity = entity;
        this.entityType = EntityType.findTypeByEntity(entity.getClass());
        this.entitySaver = saver;
    }

    @Override
    public String getName() {
        return "Security Zone";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/RedYellowShield16.gif";
    }

    @Override
    public boolean isAuthorized() {
        final SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
        final EntityType type = EntityType.findTypeByEntity(entity.getClass());
        final boolean canRead = securityProvider.hasPermission(new AttemptedReadSpecific(type, entity));
        final boolean canUpdate = securityProvider.hasPermission(new AttemptedUpdate(type, entity));
        final boolean canReadAtLeastOneZone = !SecurityZoneUtil.getSortedReadableSecurityZones().isEmpty();
        final boolean zoneIsSetOnEntity = entity instanceof ZoneableEntity && entity.getSecurityZone() != null;

        boolean authorized = false;
        if (canUpdate && (canReadAtLeastOneZone || zoneIsSetOnEntity)) {
            // when updating, must be able to read at least one zone or a zone must be set on the entity
            authorized = true;
        } else if (canRead && zoneIsSetOnEntity) {
            // when reading, zone must be set on entity
            authorized = true;
        }
        return authorized;
    }

    @Override
    protected void performAction() {
        boolean canChange = Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(entityType, entity));
        final OkCancelDialog<SecurityZone> dlg = new OkCancelDialog<SecurityZone>(TopComponents.getInstance().getTopParent(),
                "Security Zone for " + entityType.getName(),
                true,
                new SecurityZoneValidatedPanel(entityType, entity.getSecurityZone(), canChange ? OperationType.UPDATE : OperationType.READ),
                !canChange);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.wasOKed()) {
                    try {
                        final SecurityZone value = dlg.getValue();
                        entity.setSecurityZone(SecurityZoneValidatedPanel.isNull(value) ? null : value);
                        entity = entitySaver.saveEntity(entity);
                    } catch (SaveException e) {
                        logger.log(Level.INFO, "Saver failed after zone change: " + ExceptionUtils.getMessage(e), e);
                    }
                }
            }
        });
    }
}
