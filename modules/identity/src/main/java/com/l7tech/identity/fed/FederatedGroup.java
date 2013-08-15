/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.identity.fed;

import java.util.Map;

import com.l7tech.identity.PersistentGroup;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.migration.MigrationMappingSelection.NONE;

import javax.xml.bind.annotation.XmlRootElement;
import javax.persistence.Table;
import javax.persistence.Entity;
import javax.persistence.Column;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

/**
 * A "physical" federated group.
 *
 * Physical groups only exist on the trusting SSG; their membership is maintained manually
 * by the administrator. By contrast, the membership of a given {@link FederatedUser} in a {@link VirtualGroup}
 * can change based on the user's and group's particular attributes.
 *
 * @author alex
 * @see VirtualGroup
 */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@Table(name="fed_group")
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
public class FederatedGroup extends PersistentGroup {
    public FederatedGroup() {
        this(IdentityProviderConfig.DEFAULT_GOID, null);
    }

    public FederatedGroup(Goid providerOid, String name) {
        super(providerOid, name);
    }

    public FederatedGroup(Goid providerOid, String name, Map<String, String> properties) {
        super(providerOid, name, properties);
    }

    @Override
    @Column(name="provider_goid", nullable=false)
    @Type(type = "com.l7tech.server.util.GoidType")
    @Migration(mapName = NONE, mapValue = NONE, export = false, resolver = PropertyResolver.Type.ID_PROVIDER_CONFIG)
    public Goid getProviderId() {
        return super.getProviderId();
    }

    public String toString() {
        return "com.l7tech.identity.fed.FederatedGroup." +
                "\n\tname=" + _name +
                "\n\tproviderId=" + getProviderId();
    }
}