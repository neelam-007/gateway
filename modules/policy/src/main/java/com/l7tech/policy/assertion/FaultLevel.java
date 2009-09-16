package com.l7tech.policy.assertion;

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
public class FaultLevel extends Assertion implements UsesVariables {
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

    private final static String baseName = "Customize SOAP Fault Response";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<FaultLevel>(){
        @Override
        public String getAssertionName( final FaultLevel assertion, final boolean decorate) {
            return baseName;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"audit"});

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Override the detail level of the returned SOAP fault in case of policy failure.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/disconnect.gif");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.FaultLevelPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "Fault Response Properties");
        return meta;
    }

}
