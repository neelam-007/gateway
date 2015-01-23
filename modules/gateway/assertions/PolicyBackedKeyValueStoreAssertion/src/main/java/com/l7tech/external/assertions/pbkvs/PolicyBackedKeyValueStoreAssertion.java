package com.l7tech.external.assertions.pbkvs;

import com.l7tech.external.assertions.pbkvs.server.ModuleLoadListener;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.logging.Logger;

/**
 * Simple test assertion that exposes a policy-backed service.
 */
public class PolicyBackedKeyValueStoreAssertion extends Assertion implements SetsVariables, UsesVariables, UsesEntities {
    protected static final Logger logger = Logger.getLogger( PolicyBackedKeyValueStoreAssertion.class.getName() );

    public static final String OPERATION_GET = "get";
    public static final String OPERATION_PUT = "put";

    private String operation = "get";
    private String key = "key";
    private String value = "value";
    private String targetVariableName = "output";
    private String policyBackedServiceName = null;
    private Goid policyBackedServiceGoid = null;

    public String getOperation() {
        return operation;
    }

    public void setOperation( String operation ) {
        this.operation = operation;
    }

    public String getKey() {
        return key;
    }

    public void setKey( String key ) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    public String getTargetVariableName() {
        return targetVariableName;
    }

    public void setTargetVariableName( String targetVariableName ) {
        this.targetVariableName = targetVariableName;
    }

    public String getPolicyBackedServiceName() {
        return policyBackedServiceName;
    }

    public void setPolicyBackedServiceName( String policyBackedServiceName ) {
        this.policyBackedServiceName = policyBackedServiceName;
    }

    public Goid getPolicyBackedServiceGoid() {
        return policyBackedServiceGoid;
    }

    public void setPolicyBackedServiceGoid( Goid policyBackedServiceGoid ) {
        this.policyBackedServiceGoid = policyBackedServiceGoid;
    }

    @Override
    public String[] getVariablesUsed() {
        VariableUseSupport.VariablesUsed v = VariableUseSupport.expressions( key, value );
        return v.asArray();
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return targetVariableName == null ? new VariableMetadata[0] : new VariableMetadata[] {
                new VariableMetadata( targetVariableName, false, false, targetVariableName, true, DataType.STRING )
        };
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = PolicyBackedKeyValueStoreAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if ( Boolean.TRUE.equals( meta.get( META_INITIALIZED ) ) )
            return meta;

        // Set description for GUI
        meta.put( AssertionMetadata.SHORT_NAME, "Access Policy Backed Key Value Store" );
        meta.put( AssertionMetadata.LONG_NAME, "Get or Put a value from a policy backed key value store." );

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put( AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" } );
        meta.put( AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/polback16.gif" );

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put( AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto" );

        meta.put( AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.pbkvs.server.ModuleLoadListener" );

        meta.put( META_INITIALIZED, Boolean.TRUE );
        return meta;
    }

    @Override
    public EntityHeader[] getEntitiesUsed() {
        if ( policyBackedServiceGoid == null )
            return new EntityHeader[0];

        EntityHeader header = new EntityHeader();
        if ( policyBackedServiceName != null )
            header.setName( policyBackedServiceName );
        header.setGoid( policyBackedServiceGoid );
        return new EntityHeader[] { header };
    }

    @Override
    public void replaceEntity( EntityHeader oldEntityHeader, EntityHeader newEntityHeader ) {
        if ( EntityType.POLICY_BACKED_SERVICE.equals( oldEntityHeader.getType() ) &&
                oldEntityHeader.getType().equals( newEntityHeader.getType() ) &&
                Goid.equals( policyBackedServiceGoid, oldEntityHeader.getGoid() ) ) {
            policyBackedServiceName = newEntityHeader.getName();
            policyBackedServiceGoid = newEntityHeader.getGoid();
        }
    }
}
