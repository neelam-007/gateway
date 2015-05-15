package com.l7tech.external.assertions.apiportalintegration.server;

/**
 * EncassGuid = The encapsulated assertion guid. EncassId = The encapsulated assertions id.
 * <p/>
 * The encass name will also be set to the encass Guid
 */
public class PortalManagedEncass extends AbstractPortalGenericEntity {
    private String encassId;
    private String encassGuid;
    private boolean hasRouting = false;
    private String parsedPolicyDetails;

    @Override
    public AbstractPortalGenericEntity getReadOnlyCopy() {
        final PortalManagedEncass readOnly = new PortalManagedEncass();
        copyBaseFields(this, readOnly);
        readOnly.setEncassId(this.getEncassId());
        readOnly.setEncassGuid(this.getEncassGuid());
        readOnly.setHasRouting(this.getHasRouting());
        readOnly.setParsedPolicyDetails(this.getParsedPolicyDetails());
        readOnly.lock();
        return readOnly;
    }

    public String getEncassId() {
        return encassId;
    }

    public void setEncassId(String encassId) {
        checkLocked();
        this.encassId = encassId;
    }

    public String getEncassGuid() {
        return encassGuid;
    }

    public void setEncassGuid(String encassGuid) {
        checkLocked();
        this.encassGuid = encassGuid;
        setName(encassGuid);
    }

    /**
     * Sets the name of the PortalManagedEncass. This will also set the EncassGuid. The name and Guid should always be
     * the same.
     *
     * @param name The encass guid
     */
    public void setName(String name) {
        super.setName(name);
        encassGuid = name;
    }

    public boolean getHasRouting() {
        return hasRouting;
    }

    public void setHasRouting(boolean hasRouting) {
        checkLocked();
        this.hasRouting = hasRouting;
    }

    public String getParsedPolicyDetails() {
        return parsedPolicyDetails;
    }

    public void setParsedPolicyDetails(String parsedPolicyDetails) {
        checkLocked();
        this.parsedPolicyDetails = parsedPolicyDetails;
    }
}
