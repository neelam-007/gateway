package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * This assertions allows the overriding of the PolicyEnforcementContext's SoapFaultLevel
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * @author flascell<br/>
 *
 * @see SoapFaultLevel
 */
public class FaultLevel extends Assertion implements PrivateKeyable, UsesVariables {
    private SoapFaultLevel data = new SoapFaultLevel();
    private boolean soap12 = false;

    public FaultLevel() {
    }

    public SoapFaultLevel getLevelInfo() {
        return data;
    }

    public void setLevelInfo(SoapFaultLevel levelInfo) {
        this.data = levelInfo;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return data.getVariablesUsed();
    }

    public boolean isSoap12() {
        return soap12;
    }

    public void setSoap12(boolean soap12) {
        this.soap12 = soap12;
    }

    @Override
    public String getKeyAlias() {
        return data.getKeyAlias();
    }

    @Override
    public void setKeyAlias( final String keyAlias ) {
        data.setKeyAlias( keyAlias );
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SSGKEY)
    public Goid getNonDefaultKeystoreId() {
        return data.getNonDefaultKeystoreId();
    }

    @Override
    public void setNonDefaultKeystoreId( final Goid nonDefaultKeystoreId ) {
        data.setNonDefaultKeystoreId( nonDefaultKeystoreId );
    }

    /**
     * @Deprecated Needed for backwards compatibility
     */
    @Deprecated
    public void setNonDefaultKeystoreId( final long nonDefaultKeystoreId ) {
        data.setNonDefaultKeystoreId(GoidUpgradeMapper.mapOid(EntityType.SSG_KEYSTORE, nonDefaultKeystoreId ));
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return data.isUsesDefaultKeyStore();
    }

    @Override
    public void setUsesDefaultKeyStore( final boolean usesDefaultKeyStore ) {
        data.setUsesDefaultKeyStore( usesDefaultKeyStore );
    }

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<FaultLevel>(){
        @Override
        public String getAssertionName( final FaultLevel assertion, final boolean decorate) {
            final String shortName = assertion.meta().get( SHORT_NAME );
            if (!decorate) return shortName;

            final StringBuilder nameBuilder = new StringBuilder(256);
            nameBuilder.append( shortName );
            if ( assertion.data != null ) {
                nameBuilder.append( " as " );                
                switch ( assertion.data.getLevel() ) {
                    case SoapFaultLevel.DROP_CONNECTION:
                        nameBuilder.append( "Drop Connection" );
                        break;
                    case SoapFaultLevel.GENERIC_FAULT:
                        nameBuilder.append( "Generic SOAP Fault" );
                        break;
                    case SoapFaultLevel.MEDIUM_DETAIL_FAULT:
                        nameBuilder.append( "Medium Detail" );
                        break;
                    case SoapFaultLevel.FULL_TRACE_FAULT:
                        nameBuilder.append( "Full Detail" );
                        break;
                    case SoapFaultLevel.TEMPLATE_FAULT:
                        nameBuilder.append( "Template Fault" );
                        break;
                }
            }
            return AssertionUtils.decorateName(assertion, nameBuilder);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"audit"});

        meta.put(SHORT_NAME, "Customize SOAP Fault Response");
        meta.put(DESCRIPTION, "Override the detail level of the returned SOAP fault in case of policy failure.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/disconnect.gif");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.FaultLevelPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "Fault Response Properties");

        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.FaultLevelAssertionValidator");

        return meta;
    }

    @Override
    public FaultLevel clone() {
        FaultLevel clone = (FaultLevel) super.clone();
        clone.data = new SoapFaultLevel( data );
        return clone;
    }
}
