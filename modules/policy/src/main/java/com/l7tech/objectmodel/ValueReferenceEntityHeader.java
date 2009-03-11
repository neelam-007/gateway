package com.l7tech.objectmodel;

import com.l7tech.util.HexUtils;
import com.l7tech.util.TextUtils;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

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
        Map<String,String> ownerProperties = owner.getExtraProperties();
        if (ownerProperties != null)
            extraProperties = new HashMap<String,String>(ownerProperties);
        setProperty(OWNER_TYPE, owner.getType().name());
        this.type = EntityType.VALUE_REFERENCE;
    }

    protected String computeMappingKey(String sourceValue) {
        StringBuilder idBuilder = new StringBuilder();
        idBuilder.append( getOwnerType() );
        idBuilder.append( ':' );
        idBuilder.append( getOwnerId() );
        idBuilder.append( ':' );
        idBuilder.append( getValueType() );
        idBuilder.append( ':' );
        idBuilder.append( HexUtils.encodeBase64( HexUtils.getMd5Digest( HexUtils.encodeUtf8(sourceValue) ) ) );
        return idBuilder.toString();
    }

    public ValueReferenceEntityHeader(ExternalEntityHeader other) {
        super(other.getExternalId(), other);
        setExtraProperties(new HashMap<String, String>(other.getExtraProperties()));
    }

    @XmlTransient
    public EntityType getOwnerType() {
        return EntityType.valueOf(getProperty(OWNER_TYPE));
    }

    @XmlTransient
    public String getOwnerId() {
        return getExternalId().substring(getExternalId().indexOf(":")+1);
    }

    @XmlTransient
    public String getPropertyName() {
        return HexUtils.decodeUtf8(HexUtils.decodeBase64(getExternalId().substring(0, getExternalId().indexOf(":"))));
    }

    @XmlTransient
    public ExternalEntityHeader getOwnerHeader() {
        return new ExternalEntityHeader(getOwnerId(), getOwnerType(), getStrId(), getName(), getDescription(), getVersion());
    }

    @Override
    public String getDisplayNameWithScope() {
        return super.getDisplayNameWithScope() +
               (getValueType() != null ? ", " + getValueType().getName() + " " + TextUtils.truncStringMiddleExact( getDisplayValue(), 128 ) : "");
    }
}
