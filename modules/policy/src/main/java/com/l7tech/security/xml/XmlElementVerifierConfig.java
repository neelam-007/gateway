package com.l7tech.security.xml;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.policy.assertion.UsesEntities;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.util.FullQName;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

/**
 * Holds settings for verifying a signed XML element, as within a policy assertion bean that needs to store such configuration.
 */
public class XmlElementVerifierConfig implements Serializable, UsesVariables, UsesEntities {
    public static final Set<FullQName> DEFAULT_ID_ATTRS = Collections.unmodifiableSet(new LinkedHashSet<FullQName>() {{
        add(new FullQName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", null, "Id"));
        add(new FullQName("http://schemas.xmlsoap.org/ws/2002/07/utility", null, "Id"));
        add(new FullQName("http://schemas.xmlsoap.org/ws/2003/06/utility", null, "Id"));
        add(new FullQName("http://schemas.xmlsoap.org/ws/2003/06/utility", null, "Id"));
        add(new FullQName("urn:oasis:names:tc:SAML:1.0:assertion", "local", "AssertionID"));
        add(new FullQName("urn:oasis:names:tc:SAML:2.0:assertion", "local", "ID"));
        add(new FullQName(null, null, "Id"));
        add(new FullQName(null, null, "id"));
        add(new FullQName(null, null, "ID"));
    }});

    private String verifyCertificateName;
    private Goid verifyCertificateOid;
    private String verifyCertificateVariableName;
    private boolean ignoreKeyInfo;
    private FullQName[] customIdAttrs;

    public String getVerifyCertificateName() {
        return verifyCertificateName;
    }

    public void setVerifyCertificateName(@Nullable String verifyCertificateName) {
        this.verifyCertificateName = verifyCertificateName;
    }

    public Goid getVerifyCertificateOid() {
        return verifyCertificateOid;
    }

    public void setVerifyCertificateOid(Goid verifyCertificateOid) {
        this.verifyCertificateOid = verifyCertificateOid;
    }

    public String getVerifyCertificateVariableName() {
        return verifyCertificateVariableName;
    }

    public void setVerifyCertificateVariableName(@Nullable String verifyCertificateVariableName) {
        this.verifyCertificateVariableName = verifyCertificateVariableName;
    }

    public boolean isIgnoreKeyInfo() {
        return ignoreKeyInfo;
    }

    public void setIgnoreKeyInfo(boolean ignoreKeyInfo) {
        this.ignoreKeyInfo = ignoreKeyInfo;
    }

    public FullQName[] getCustomIdAttrs() {
        return customIdAttrs;
    }

    public void setCustomIdAttrs(@Nullable FullQName[] customIdAttrs) {
        this.customIdAttrs = customIdAttrs;
    }

    @Override
    public String[] getVariablesUsed() {
        List<String> ret = new ArrayList<String>();
        if (verifyCertificateVariableName != null && verifyCertificateVariableName.length() > 0)
            ret.add(verifyCertificateVariableName);
        return ret.toArray(new String[ret.size()]);
    }

    @Override
    public EntityHeader[] getEntitiesUsed() {
        EntityHeader[] headers = new EntityHeader[0];

        if ( !GoidEntity.DEFAULT_GOID.equals(verifyCertificateOid) ) {
            headers = new EntityHeader[] {new EntityHeader(verifyCertificateOid, EntityType.TRUSTED_CERT, null, null)};
        }

        return headers;
    }

    @Override
    public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) {
        if( oldEntityHeader.getType() == EntityType.TRUSTED_CERT &&
                newEntityHeader.getType() == EntityType.TRUSTED_CERT &&
                verifyCertificateOid != null &&
                verifyCertificateOid.equals(oldEntityHeader.getGoid())) {
            verifyCertificateOid = newEntityHeader.getGoid();
        }
    }
}
