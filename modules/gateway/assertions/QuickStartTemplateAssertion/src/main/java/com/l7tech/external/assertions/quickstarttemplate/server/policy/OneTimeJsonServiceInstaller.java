package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.l7tech.external.assertions.quickstarttemplate.server.parser.QuickStartParser;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceManager;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link QuickStartJsonServiceInstaller} that bootstraps services only once during lifecycle.</br?
 * This is to address a edge case during Gateway boot when the module is loaded once without a license
 * and a second time with licence causing the installation to be invoked twice.
 */
public class OneTimeJsonServiceInstaller extends QuickStartJsonServiceInstaller {

    private final AtomicBoolean bootstrapCompleted = new AtomicBoolean(false);

    public OneTimeJsonServiceInstaller(
            @NotNull final QuickStartServiceBuilder serviceBuilder,
            @NotNull final ServiceManager serviceManager,
            @NotNull final PolicyVersionManager policyVersionManager,
            @NotNull final QuickStartParser parser
    ) {
        super(serviceBuilder, serviceManager, policyVersionManager, parser);
    }

    @Override
    public void installJsonServices() {
        if (bootstrapCompleted.compareAndSet(false, true)) {
            super.installJsonServices();
        }
    }
}
