/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;


import com.l7tech.util.Either;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Header objects are used to refer to objects in find methods
 * of all managers
 *
 * @author flascelles
 */
@XmlRootElement
@XmlSeeAlso({IdentityHeader.class,GuidEntityHeader.class, ValueReferenceEntityHeader.class})
public class EntityHeader extends EntityHeaderRef {
    
    public EntityHeader(String id, EntityType type, String name, String description) {
        this(id, type, name, description, null);
    }

    public EntityHeader(String id, EntityType type, String name, String description, Integer version) {
        super(type, id);
        this.name = name;
        this.description = description;
        this.version = version;
    }

    @Deprecated // oid's are being replaced by goids
    public EntityHeader(long oid, EntityType type, String name, String description, Integer version) {
        this(Long.toString(oid), type, name, description, version);
    }

    @Deprecated // oid's are being replaced by goids
    public EntityHeader(long oid, EntityType type, String name, String description) {
        this(Long.toString(oid), type, name, description, null);
    }

    public EntityHeader(Goid goid, EntityType type, String name, String description, Integer version) {
        this(goid.toString(), type, name, description, version);
    }

    public EntityHeader(Goid goid, EntityType type, String name, String description) {
        this(goid.toString(), type, name, description, null);
    }

    public EntityHeader() {
        type = EntityType.ANY;
        description = "";
    }

    /**
     * Create an ID string from the specified GOID hex string or OID.
     *
     * @param goid a GOID, or null.  If specified, oid is ignored.
     * @param oid an OID, or null.  Ignored if goidStr is specified.
     * @return an ID string to use for an entity reference, encoding the specified GOID or OID, or null.
     */
    @Nullable
    public static String toIdStr( @Nullable Goid goid, @Nullable Long oid) {
        if ( goid != null )
            return goid.toString();
        if ( oid != null )
            return oid.toString();
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    @Deprecated // oid's are being replaced by goids
    @XmlAttribute
    public void setOid( long oid ) {
        strId = Long.toString(oid);
    }

    @Deprecated // oid's are being replaced by goids
    public long getOid() {
        if (strId == null || strId.isEmpty()) return PersistentEntity.DEFAULT_OID;
        try {
            return Long.parseLong(strId);
        } catch (Exception e) {
            return PersistentEntity.DEFAULT_OID;
        }
    }

    /**
     * Get the ID string as either a GOID or an OID, depending on its formt.
     *
     * @return a Goid if the string is a valid GOID hex string; otherwise, an OID if the string is a valid long;
     *         otherwise the default Goid.  Never null.
     */
    @NotNull
    public Either<Long,Goid> getEitherGoidOrId(){
        if (strId == null || strId.trim().length() < 1)
            return Either.right(new Goid(GoidEntity.DEFAULT_GOID));

        try {
            return Either.<Long,Goid>right(new Goid(strId));
        } catch (Exception e) {
            /* FALLTHROUGH and treat as OID */
        }

        try {
            return Either.<Long,Goid>left(Long.parseLong(strId));
        } catch (Exception e) {
            return Either.right(new Goid(GoidEntity.DEFAULT_GOID));
        }
    }

    /**
     * Returns true if and only if the specified other entity header contains an non-null non-default ID (OID or GOID)
     * that is the same format and the same value as this entity header's non-null on-default ID.
     *
     * @param other the other header to examine.
     * @return true if both this header and the other header contain a non-null non-default ID string,
     *         and both strings are in the same format (GOID or OID) and encode the same ID value.
     */
    public boolean equalsId(@NotNull EntityHeader other) {
        Either<Long, Goid> a = getEitherGoidOrId();
        Either<Long, Goid> b = other.getEitherGoidOrId();

        return a.isLeft() == b.isLeft() && (a.isLeft()
            ? !a.left().equals(PersistentEntity.DEFAULT_OID) && a.left().equals(b.left())
            : !a.right().equals(GoidEntity.DEFAULT_GOID) && a.right().equals(b.right()));
    }

    /**
     * Returns true if and only if this entity header contains a non-null non-default ID (OID or GOID)
     * that matches one of the specified IDs.
     *
     * @param goid a GOID to check.  If a non-null non-default GOID is specified, any oid argument will be ignored completely (even if the GOID doesn't match).
     * @param oid an OID to check.  Will be ignored if a goid argument is provided.
     *
     * @return true if this entity header contains a non-null non-default ID matching the specified GOID (if provided) or OID.
     */
    public boolean equalsId( @Nullable Goid goid, @Nullable Long oid ) {
        if ( goid != null && !GoidEntity.DEFAULT_GOID.equals(goid) ) {
            Either<Long, Goid> id = getEitherGoidOrId();
            return id.isRight() && id.right().equals(goid);
        }
        if ( oid != null && !oid.equals(PersistentEntity.DEFAULT_OID) ) {
            Either<Long, Goid> id = getEitherGoidOrId();
            return id.isLeft() && id.left().equals(oid);
        }
        return false;
    }

    /**
     * Returns true if and only if this entity header contains a non-null non-default ID (OID or GOID)
     * that matches one of the specified IDs.
     *
     * @param goidString a GOID to check, encoded as a hex string.  If a valid non-default GOID is specified, any oid argument will be ignored completely (even if the GOID doesn't match).
     * @param oid an OID to check.  Will be ignored if a goid argument is provided.
     *
     * @return true if this entity header contains a non-null non-default ID matching the specified GOID (if provided) or OID.
     */
    public boolean equalsId( @Nullable String goidString, @Nullable Long oid ) {
        Goid goid = null;
        try {
            if (goidString != null)
                goid = new Goid(goidString);
        } catch (Exception e) {
            /* FALLTHROUGH and leave GOID as null */
        }
        return equalsId( goid, oid );
    }

    @XmlAttribute
    @XmlJavaTypeAdapter(GoidAdapter.class)
    public void setGoid( Goid goid ) {
        strId = goid.toString();
    }

    public Goid getGoid() {
        if (strId == null || strId.isEmpty()) return GoidEntity.DEFAULT_GOID;
        try {
            return new Goid(strId);
        } catch (Exception e) {
            return GoidEntity.DEFAULT_GOID;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityHeader)) return false;
        EntityHeader theotherone = (EntityHeader)obj;
        if (theotherone.type != this.type) return false;
        if (getStrId() == null) {
            return theotherone.getStrId() == null;
        }
        return getStrId().equals(theotherone.getStrId());
    }

    public String toStringVerbose() {
        return "EntityHeader. Name=" + getName() + ", oid=" + getStrId() + ", description=" + description +
               ", type = " + type.toString();
    }

    /**
     * Get an end-user visisble string representation of this header.
     *
     * @return A user friendly identifier for this header.
     */
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        int result;
        result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (strId != null ? strId.hashCode() : 0);
        return result;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private static final long serialVersionUID = -1752153501322477805L;

    protected String name;
    private String description;
    Integer version;

    @Override
    public int compareTo(Object o) {
        if (o == null) throw new NullPointerException();
        EntityHeader other = (EntityHeader)o;
        if (strId != null && other.strId != null) {
            if (strId.equals(other.strId)) return 0;
        }
        if ( name.equals(((EntityHeader)o).name) ) {
            return type.compareTo(((EntityHeader)o).type);
        }
        return name.compareTo(((EntityHeader)o).name);
    }
}
