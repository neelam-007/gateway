package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.PrivateKeyExportResult;
import com.l7tech.gateway.api.impl.PrivateKeyGenerateCsrResult;
import com.l7tech.gateway.api.impl.PrivateKeySignCsrResult;
import com.l7tech.util.NonObfuscatable;

import javax.xml.bind.annotation.*;

/**
 * This represents a reference to a rest entity.
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "Item")
@XmlType(name = "ItemReferenceType", propOrder = {"name", "id", "type", "date", "links", "resourceList"})
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Item<R> extends Reference<R> {

    Item() {
    }

    @XmlElementRefs({
            @XmlElementRef(type = ActiveConnectorMO.class),
            @XmlElementRef(type = AssertionSecurityZoneMO.class),
            @XmlElementRef(type = TrustedCertificateMO.class),
            @XmlElementRef(type = CassandraConnectionMO.class),
            @XmlElementRef(type = ClusterPropertyMO.class),
            @XmlElementRef(type = CustomKeyValueStoreMO.class),
            @XmlElementRef(type = ResourceDocumentMO.class),
            @XmlElementRef(type = EmailListenerMO.class),
            @XmlElementRef(type = EncapsulatedAssertionMO.class),
            @XmlElementRef(type = FirewallRuleMO.class),
            @XmlElementRef(type = FolderMO.class),
            @XmlElementRef(type = GenericEntityMO.class),
            @XmlElementRef(type = HttpConfigurationMO.class),
            @XmlElementRef(type = IdentityProviderMO.class),
            @XmlElementRef(type = InterfaceTagMO.class),
            @XmlElementRef(type = JDBCConnectionMO.class),
            @XmlElementRef(type = JMSDestinationMO.class),
            @XmlElementRef(type = ListenPortMO.class),
            @XmlElementRef(type = PolicyAliasMO.class),
            @XmlElementRef(type = PolicyMO.class),
            @XmlElementRef(type = PolicyVersionMO.class),
            @XmlElementRef(type = PrivateKeyMO.class),
            @XmlElementRef(type = PrivateKeyExportResult.class),
            @XmlElementRef(type = PrivateKeyGenerateCsrResult.class),
            @XmlElementRef(type = PrivateKeySignCsrResult.class),
            @XmlElementRef(type = RevocationCheckingPolicyMO.class),
            @XmlElementRef(type = ServiceMO.class),
            @XmlElementRef(type = RbacRoleMO.class),
            @XmlElementRef(type = SampleMessageMO.class),
            @XmlElementRef(type = SiteMinderConfigurationMO.class),
            @XmlElementRef(type = StoredPasswordMO.class),
            @XmlElementRef(type = SecurityZoneMO.class),
            @XmlElementRef(type = ServiceAliasMO.class),
            @XmlElementRef(type = DependencyListMO.class),
            @XmlElementRef(type = Mappings.class),
            @XmlElementRef(type = ItemsList.class),
            @XmlElementRef(type = Bundle.class),
            @XmlElementRef(type = UserMO.class),
            @XmlElementRef(type = GroupMO.class)
    })
    @XmlAnyElement(lax = true)
    @XmlElementWrapper(name = "Resource", required = false)
    //This needs to be NonObfuscatable in order to avoid jaxb issues
    @NonObfuscatable
    protected Object[] getResourceList() {
        if (getContent() == null) {
            return null;
        }
        return new Object[]{getContent()};
    }

    protected void setResourceList(Object[] content) {
        //noinspection unchecked
        setContent(content != null && content.length>0 ? (R)content[0] : null);
    }
}
