package com.l7tech.server.policy.bundle.ssgman.restman;

import com.l7tech.gateway.common.api.solutionkit.InstanceModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Apply version modifier to entities of a restman migration bundle.
 */
public class VersionModifier extends InstanceModifier {
    public VersionModifier(@NotNull final RestmanMessage restmanMessage, @Nullable final String versionModifier) {
        super(restmanMessage.getBundleReferenceItems(), versionModifier);
    }
}