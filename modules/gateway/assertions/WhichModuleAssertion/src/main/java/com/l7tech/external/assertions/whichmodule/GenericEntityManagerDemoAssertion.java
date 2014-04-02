package com.l7tech.external.assertions.whichmodule;

import com.l7tech.external.assertions.whichmodule.server.GenericEntityManagerDemoServerSupport;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.assertion.*;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.util.Collection;

/**
 * A hidden assertion that registers the demo generic entity extension interface and GUI action.
 */
public class GenericEntityManagerDemoAssertion extends Assertion  implements  UsesEntities {

    Goid genericEntityId;
    String genericEntityClass;

    public Goid getGenericEntityId() {
        return genericEntityId;
    }

    public void setGenericEntityId(Goid genericEntityId) {
        this.genericEntityId = genericEntityId;
    }

    public String getGenericEntityClass() {
        return genericEntityClass;
    }

    public void setGenericEntityClass(String genericEntityClass) {
        this.genericEntityClass = genericEntityClass;
    }

    @Override
    public EntityHeader[] getEntitiesUsed() {
        return genericEntityId == null
                ? new EntityHeader[0]
                : new EntityHeader[] { new GenericEntityHeader(genericEntityId.toString(),null, null,null,genericEntityClass) };
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if (newEntityHeader != null && EntityType.GENERIC.equals(newEntityHeader.getType()) && newEntityHeader instanceof GenericEntityHeader) {
            genericEntityId = ((GenericEntityHeader)newEntityHeader).getGoid();
            genericEntityClass = ((GenericEntityHeader)newEntityHeader).getEntityClassName();
        }
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] { "com.l7tech.external.assertions.whichmodule.console.ManageDemoGenericEntitiesAction" });

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
            @Override
            public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                return GenericEntityManagerDemoServerSupport.getInstance(appContext).getExtensionInterfaceBindings();
            }
        });

        return meta;
    }
}
