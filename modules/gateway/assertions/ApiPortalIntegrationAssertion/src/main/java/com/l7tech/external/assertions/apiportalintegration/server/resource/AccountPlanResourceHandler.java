package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.accountplan.AccountPlan;
import com.l7tech.external.assertions.apiportalintegration.server.accountplan.manager.AccountPlanManager;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * REST-like resource handler for AccountPlanResource.
 */
public class AccountPlanResourceHandler extends AbstractResourceHandler<AccountPlanResource, AccountPlan> {
    public static AccountPlanResourceHandler getInstance(@NotNull final ApplicationContext context) {
        if (instance == null) {
            instance = new AccountPlanResourceHandler(context);
        }
        return instance;
    }

    @Override
    public List<AccountPlanResource> get(@Nullable final Map<String, String> filters) throws FindException {
        return doGet(filters);
    }

    @Override
    public AccountPlanResource get(@NotNull final String id) throws FindException {
        throw new UnsupportedOperationException("Get single resource not supported for AccountPlanResource");
    }

    @Override
    public void delete(@NotNull final String id) throws DeleteException, FindException {
        doDelete(id);
    }

    @Override
    public List<AccountPlanResource> put(@NotNull final List<AccountPlanResource> resources, final boolean removeOmitted) throws ObjectModelException {
        final Date now = new Date();
        for (final AccountPlanResource resource : resources) {
            // overwrite any last update with today's date
            resource.setLastUpdate(now);
        }
        return doPut(resources, removeOmitted);
    }

    @Override
    public AccountPlanResource put(@NotNull final AccountPlanResource resource) {
        throw new UnsupportedOperationException("Put single resource not supported for AccountPlanResource.");
    }

    @Override
    public boolean doFilter(@NotNull final AbstractPortalGenericEntity entity, @Nullable final Map<String, String> filters) {
        return entity instanceof AccountPlan;
    }

    AccountPlanResourceHandler(@NotNull final ApplicationContext context) {
        super(AccountPlanManager.getInstance(context), AccountPlanResourceTransformer.getInstance());
    }

    AccountPlanResourceHandler(@NotNull final PortalGenericEntityManager<AccountPlan> manager,
                               @NotNull final ResourceTransformer<AccountPlanResource, AccountPlan> transformer) {
        super(manager, transformer);
    }

    private static AccountPlanResourceHandler instance;
}
