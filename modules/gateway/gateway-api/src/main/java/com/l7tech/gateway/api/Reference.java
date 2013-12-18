package com.l7tech.gateway.api;

import com.l7tech.util.NonObfuscatable;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This represents a reference to a rest entity.
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "Reference")
@XmlType(name = "EntityReferenceType", propOrder = {"title", "id", "type", "links", "resourceList"})
public class Reference {
    private String id;
    private String type;
    private String title;
    private List<Link> links;
    private Object resource;

    Reference() {
    }

    @XmlElement(name = "id", required = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement(name = "type", required = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlElement(name = "title", required = true)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @XmlElement(name = "link")
    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    @XmlElementRefs({
            @XmlElementRef(type = ActiveConnectorMO.class),
            @XmlElementRef(type = TrustedCertificateMO.class),
            @XmlElementRef(type = ClusterPropertyMO.class),
            @XmlElementRef(type = CustomKeyValueStoreMO.class),
            @XmlElementRef(type = ResourceDocumentMO.class),
            @XmlElementRef(type = EmailListenerMO.class),
            @XmlElementRef(type = EncapsulatedAssertionMO.class),
            @XmlElementRef(type = FolderMO.class),
            @XmlElementRef(type = GenericEntityMO.class),
            @XmlElementRef(type = HttpConfigurationMO.class),
            @XmlElementRef(type = IdentityProviderMO.class),
            @XmlElementRef(type = JDBCConnectionMO.class),
            @XmlElementRef(type = JMSDestinationMO.class),
            @XmlElementRef(type = ListenPortMO.class),
            @XmlElementRef(type = PolicyAliasMO.class),
            @XmlElementRef(type = PolicyMO.class),
            @XmlElementRef(type = PolicyVersionMO.class),
            @XmlElementRef(type = ServiceMO.class),
            @XmlElementRef(type = RbacRoleMO.class),
            @XmlElementRef(type = StoredPasswordMO.class),
            @XmlElementRef(type = SecurityZoneMO.class),
            @XmlElementRef(type = ServiceAliasMO.class),
    })
    @XmlAnyElement(lax = true)
    @XmlElementWrapper(name = "resource", required = false)
    //This needs to be NonObfuscatable in order to avoid jaxb issues
    @NonObfuscatable
    private List<Object> getResourceList() {
        if (resource == null) {
            return null;
        }
        ArrayList<Object> list = new ArrayList<>(1);
        list.add(resource);
        return list;
    }

    private void setResourceList(List<Object> content) {
        this.resource = content != null && !content.isEmpty() ? content.get(0) : null;
    }

    public Object getResource() {
        return resource;
    }

    public void setResource(Object resource) {
        this.resource = resource;
    }
}
