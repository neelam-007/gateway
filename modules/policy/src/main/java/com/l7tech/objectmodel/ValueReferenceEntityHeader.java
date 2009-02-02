package com.l7tech.objectmodel;

import com.l7tech.util.HexUtils;

import javax.xml.bind.annotation.XmlRootElement;
import java.nio.charset.Charset;

/**
 * @author jbufu
 */
@XmlRootElement
public class ValueReferenceEntityHeader extends ExternalEntityHeader {

    private final static String OWNER_TYPE = "ownerType";

    public ValueReferenceEntityHeader() {
    }

    public ValueReferenceEntityHeader(ExternalEntityHeader owner, String propertyName) {
        super(HexUtils.encodeBase64(propertyName.getBytes(Charset.forName("UTF-8"))) + ":" + owner.getExternalId(), owner);
        setName(getName() + " : " + propertyName);
        addProperty(OWNER_TYPE, owner.getType().name());
        this.type = EntityType.VALUE_REFERENCE;
    }

    public ValueReferenceEntityHeader(ExternalEntityHeader other) {
        super(other.getExternalId(), other);
        setExtraProperties(other.getExtraProperties());
    }

    public EntityType getOwnerType() {
        return EntityType.valueOf(getProperty(OWNER_TYPE));
    }

    public String getOwnwerId() {
        return getExternalId().substring(getExternalId().indexOf(":")+1);
    }

    public String getPropertyName() {
        return HexUtils.decodeUtf8(HexUtils.decodeBase64(getExternalId().substring(0, getExternalId().indexOf(":"))));
    }
}
