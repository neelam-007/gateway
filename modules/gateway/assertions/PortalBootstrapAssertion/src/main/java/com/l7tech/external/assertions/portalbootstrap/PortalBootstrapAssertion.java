package com.l7tech.external.assertions.portalbootstrap;

import com.l7tech.external.assertions.portalbootstrap.server.PortalBootstrapManager;
import com.l7tech.policy.assertion.*;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Invisible support assertion for Portal bootstrap.
 */
public class PortalBootstrapAssertion extends Assertion {
    //
    // Metadata
    //
    private static final String META_INITIALIZED = PortalBootstrapAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if ( Boolean.TRUE.equals( meta.get( META_INITIALIZED ) ) )
            return meta;

        // Set description for GUI
        meta.put( AssertionMetadata.SHORT_NAME, "Portal Bootstrap" );
        meta.put( AssertionMetadata.LONG_NAME, "Internal assertion for enrolling Gateway with Portal server" );

        meta.put( AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME,
                "com.l7tech.external.assertions.portalbootstrap.server.PortalBootstrapModuleLoadListener" );

        meta.put( AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] {
                "com.l7tech.external.assertions.portalbootstrap.console.EnrollWithPortalAction"
        } );

        meta.put( AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<>(PortalBootstrapExtensionInterface.class, null, new PortalBootstrapExtensionInterface() {
                    @Override
                    public void enrollWithPortal( String enrollmentUrl ) throws IOException {
                        PortalBootstrapManager.getInstance().enrollWithPortal( enrollmentUrl );
                    }
                });
                return Collections.singletonList( binding );
            }
        } );

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
