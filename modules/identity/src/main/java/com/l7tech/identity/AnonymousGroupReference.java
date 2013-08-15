/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.identity;

import com.l7tech.objectmodel.AnonymousEntityReference;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;

/**
 * @author alex
 */
public class AnonymousGroupReference extends AnonymousEntityReference implements Group {
    private final GroupBean groupBean;

    public AnonymousGroupReference(String uniqueId, Goid providerOid, String name) {
        super(Group.class, uniqueId, name);
        this.groupBean = new GroupBean(providerOid, null);
    }

    @Migration(mapName = NONE, mapValue = NONE, export = false, resolver = PropertyResolver.Type.ID_PROVIDER_CONFIG)
    public Goid getProviderId() {
        return groupBean.getProviderId();
    }

    public boolean isEquivalentId(Object thatId) {
        return thatId != null && thatId.equals(uniqueId);
    }

    public String getId() {
        return groupBean.getId();
    }

    public String getDescription() {
        return groupBean.getDescription();
    }

    public GroupBean getGroupBean() {
        return groupBean;
    }
}
