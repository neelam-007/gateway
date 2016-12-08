package com.l7tech.external.assertions.portalupgrade;

import com.l7tech.external.assertions.portalupgrade.server.PortalUpgradeExtensionInterfaceImpl;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * Invisible support assertion for Portal bootstrap.
 */
public class PortalUpgradeAssertion extends Assertion implements SetsVariables
{

    @Override
    public VariableMetadata[] getVariablesSet() {
      return new VariableMetadata[]{new VariableMetadata("portal.update.error")};
    }


    //
    // Metadata
    //
    private static final String META_INITIALIZED = PortalUpgradeAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if ( Boolean.TRUE.equals( meta.get( META_INITIALIZED ) ) )
            return meta;

        // Set description for GUI
        meta.put( AssertionMetadata.SHORT_NAME, "Portal Upgrade" );
        meta.put( AssertionMetadata.LONG_NAME, "Internal assertion for updateing Gateway with Portal server" );

        meta.put( AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME,
                "PortalUpgradeModuleLoadListener" );

        meta.put( AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] {
                "com.l7tech.external.assertions.portalupgrade.console.UpgradePortalAction",
        } );

        meta.put( AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<>(PortalUpgradeExtensionInterface.class, null, new PortalUpgradeExtensionInterfaceImpl());
                return Collections.singletonList( binding );
            }
        } );

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
