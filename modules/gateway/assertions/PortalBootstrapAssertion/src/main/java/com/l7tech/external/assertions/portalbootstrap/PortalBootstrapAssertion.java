package com.l7tech.external.assertions.portalbootstrap;

import com.l7tech.external.assertions.portalbootstrap.server.PortalBootstrapExtensionInterfaceImpl;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * Invisible support assertion for Portal bootstrap.
 */
public class PortalBootstrapAssertion extends Assertion implements UsesVariables {

    private String enrollmentUrl;

    public String getEnrollmentUrl() {
        return enrollmentUrl;
    }

    public void setEnrollmentUrl(String enrollmentUrl) {
        this.enrollmentUrl = enrollmentUrl;
    }


    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(enrollmentUrl);
    }

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
                ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<>(PortalBootstrapExtensionInterface.class, null, new PortalBootstrapExtensionInterfaceImpl());
                return Collections.singletonList( binding );
            }
        } );

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
