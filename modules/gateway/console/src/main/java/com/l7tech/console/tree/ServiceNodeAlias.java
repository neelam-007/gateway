package com.l7tech.console.tree;

import com.l7tech.console.util.EntitySaver;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.console.action.*;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Comparator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.logging.Level;

public class ServiceNodeAlias extends ServiceNode{
    public ServiceNodeAlias(ServiceHeader e) throws IllegalArgumentException {
        super(e);
    }

    public ServiceNodeAlias(ServiceHeader e, Comparator c) {
        super(e, c);
    }

    @Override
    public Action[] getActions() {
        final Collection<Action> actions = new ArrayList<Action>();

        actions.add(new EditPolicyAction(this));
        if (getEntityHeader().isSoap()) actions.add(new EditServiceUDDISettingsAction(this));
        actions.add(new DeleteServiceAliasAction(this));
        actions.add(new PolicyRevisionsAction(this));
        final PublishedServiceAlias alias = getAlias();
        try {
            if (alias != null && (alias.getSecurityZone() != null || !Registry.getDefault().getRbacAdmin().findAllSecurityZones().isEmpty())) {
                actions.add(new ConfigureSecurityZoneAction<>(alias, new EntitySaver<PublishedServiceAlias>() {
                    @Override
                    public PublishedServiceAlias saveEntity(@NotNull final PublishedServiceAlias entity) throws SaveException {
                        try {
                            final Goid goid = Registry.getDefault().getServiceManager().saveAlias(entity);
                            entity.setGoid(goid);
                        } catch (final UpdateException | VersionException e) {
                            throw new SaveException("Could not save service alias: " + e.getMessage(), e);
                        }
                        return entity;
                    }
                }));
            }
        } catch (final FindException e) {
            logger.log(Level.WARNING, "Unable to check security zones: " + ExceptionUtils.getMessage(e), e);
        }
        actions.add(new RefreshTreeNodeAction(this));
        Action secureCut = ServicesAndPoliciesTree.getSecuredAction(ServicesAndPoliciesTree.ClipboardActionType.CUT);
        Action secureCopy = ServicesAndPoliciesTree.getSecuredAction(ServicesAndPoliciesTree.ClipboardActionType.COPY, EntityType.SERVICE_ALIAS);
        if(secureCut != null){
            actions.add(secureCut);
        }
        if(secureCopy != null){
            actions.add(secureCopy);
        }
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public String getName() {
        return getEntityHeader().getDisplayName()+" alias"; 
    }

    @Override
    protected String iconResource(boolean open) {
        ServiceHeader header = getEntityHeader();
        if (header == null) {
            return "com/l7tech/console/resources/services_disabled16.png";
        }
        else if (header.isDisabled()) {
            if (!header.isSoap()) {
                return "com/l7tech/console/resources/xmlObject_disabled16Alias.png";
            } else {
                return "com/l7tech/console/resources/services_disabled16Alias.png";
            }
        } else {
            if(header.isSoap()) {
                return "com/l7tech/console/resources/services16Alias.png";
            } else {
                return "com/l7tech/console/resources/xmlObject16Alias.gif";
            }
        }
    }

    private PublishedServiceAlias getAlias() {
        PublishedServiceAlias alias = null;
        final ServiceHeader header = getEntityHeader();
        try {
            alias = Registry.getDefault().getServiceManager().findAliasByEntityAndFolder(header.getGoid(), header.getFolderGoid());
        } catch (final FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve service alias: " + e.getMessage(), ExceptionUtils.getDebugException(e));
        }
        return alias;
    }
}
