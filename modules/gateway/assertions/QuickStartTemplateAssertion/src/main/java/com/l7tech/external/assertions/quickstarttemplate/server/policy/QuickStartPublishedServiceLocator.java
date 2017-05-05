package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.service.ServiceManager;
import org.jetbrains.annotations.NotNull;

public class QuickStartPublishedServiceLocator {

    @NotNull
    private final ServiceManager serviceManager;

    public QuickStartPublishedServiceLocator(@NotNull final ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public PublishedService findByGoid(@NotNull Goid goid) throws FindException {
        return serviceManager.findByPrimaryKey(goid);
    }

}
