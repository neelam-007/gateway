package com.l7tech.objectmodel;

import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.DefaultPersistenceDelegate;
import java.beans.PersistenceDelegate;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Goid Object represents a global object id for the gateways entities.
 *
 * @author Victor Kazakov
 */
public final class Goid implements Comparable<Goid>, Serializable {

    //The bytes of the goid. This should always be a 16 byte array
    private final byte[] goid;

    /**
     * This is needed for serialization
     */
    @SuppressWarnings("UnusedDeclaration")
    private Goid() {
        goid = null;
    }

    /**
     * Creates a new goid from two long values
     *
     * @param high The high long will be the first 8 bytes of the goid
     * @param low  The low long will be the last 8 bytes of the goid
     */
    public Goid(long high, long low) {
        goid = ByteBuffer.allocate(16).putLong(high).putLong(low).array();
    }

    /**
     * Creates a new goid from a byte array. The given byte array must be 16 bytes long. If it isn't an
     * IllegalArgumentException is thrown
     *
     * @param goid The bytes to create the goid from. This must be 16 bytes long
     * @throws IllegalArgumentException This is thrown if the given goid bytes are not 16 bytes long
     */
    public Goid(byte[] goid) {
        if (goid.length != 16) {
            throw new IllegalArgumentException("Cannot create a goid from a byte array that is not 16 bytes long.");
        }
        this.goid = Arrays.copyOf(goid, 16);
    }

    /**
     * Creates a new goid from a string representation of a goid. An IllegalArgumentException is thrown if a goid cannot
     * be retrieved from the given string
     *
     * @param goid A string representation of a goid.
     * @throws IllegalArgumentException This is thrown if the given string does not represent a goid
     */
    public Goid(@NotNull String goid) {
        byte[] goidFromString;

        try {
            goidFromString = HexUtils.unHexDump(goid);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot create goid from this String. Invalid hex data: " + goid);
        }

        if (goidFromString.length != 16) {
            throw new IllegalArgumentException("Cannot create a goid from this String, it does not decode to a 16 byte array.");
        }
        this.goid = goidFromString;
    }

    /**
     * Creates a new goid from another goid. The id's will be identical
     *
     * @param goid The goid to clone.
     */
    public Goid(@NotNull Goid goid) {
        this(goid.getBytes());
    }

    /**
     * Get a PersistenceDelegate to use for encoding instances of this class using XMLEncoder.
     *
     * @return a PersistenceDelegate for Goid instances.  Never null.
     */
    public static PersistenceDelegate getPersistenceDelegate() {
        // Encode as call to constructor-from-byte-array using the value returned from getBytes().
        return new DefaultPersistenceDelegate(new String[]{"bytes"});
    }

    /**
     * Returns the 8 high bytes of the goid as a long
     *
     * @return The high 8 bytes of the goid as a long.
     */
    public long getHi() {
        ByteBuffer buffer = ByteBuffer.wrap(Arrays.copyOfRange(goid, 0, 8));
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getLong();
    }

    /**
     * Returns the low 8 bytes of the goid as a long
     *
     * @return The low 8 bytes of the goid as a long
     */
    public long getLow() {
        ByteBuffer buffer = ByteBuffer.wrap(Arrays.copyOfRange(goid, 8, 16));
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getLong();
    }

    /**
     * Returns the bytes of this goid
     *
     * @return The bytes of this goid
     */
    public byte[] getBytes() {
        return Arrays.copyOf(goid, 16);
    }

    /**
     * Clones this goid returning a new instance.
     *
     * @return A clone of this goid
     */
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone"})
    @Override
    public Goid clone() {
        return new Goid(this.getBytes());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Goid goid1 = (Goid) o;

        //noinspection RedundantIfStatement
        if (!Arrays.equals(goid, goid1.goid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(goid);
    }

    /**
     * This will return the Hex String representation of the Goid
     *
     * @return The Hex String representation of the goid
     */
    @Override
    public String toString() {
        return toHexString();
    }

    /**
     * This will return the Hex String representation of the Goid
     *
     * @return The Hex String representation of the goid
     */
    public String toHexString() {
        return HexUtils.hexDump(goid);
    }

    /**
     * Wrap the specified OID in a Goid within the range reserved for a wrapped OID (for temporary use
     * within a Gateway upgraded from a pre-GOID database).
     * <p/>
     * <b>NOTE:</b> GOIDs created by this method are to be used for transitional purposes and
     * must not be persisted or externalized in any way -- doing so would defeat the purpose of using GOIDs.
     *
     * @param oid the objectid to wrap, or null to just return null.
     * @return a new Goid encoding this object ID with the WRAPPED_OID prefix, or null if oid was null.
     */
    public static Goid wrapOid( @Nullable Long oid) {
        return oid == null
            ? null
            : new Goid(GoidRange.WRAPPED_OID.getFirstHi(), oid);
    }

    /**
     * Wrap the elemetns of the specific OID array in Goid instances within the range reserved for wrapped OIDs
     * (for temporary use with a Gateway upgraded from a pre-GOID database).
     * <p/>
     * <b>NOTE:</b> GOIDs created by thsi method are to be used for transitional purposes and
     * must not be persisted or externalized in any way -- doing so would defeat the purpose of using GOIDs.
     *
     * @param oids the objectid array to wrap, or null to just return null.
     * @return an array of new Goid instances encoding the specified object ID with the WRAPPED_OID prefix, or null if oids was null.
     */
    public static Goid[] wrapOids( @Nullable Long[] oids) {
        if (oids == null)
            return null;
        Goid[] goids = new Goid[oids.length];
        for (int i = 0; i < oids.length; i++) {
            goids[i] = wrapOid(oids[i]);
        }
        return goids;
    }

    /**
     * Return a Goid from a string.
     *
     * @param goid The string representation of the goid.
     * @return The goid represented by the string
     * @throws IllegalArgumentException This is thrown if the string cannot be converted to a goid.
     */
    public static Goid parseGoid(@NotNull String goid) {
        return new Goid(goid);
    }

    /**
     * This is the static version of the toString method. It will return the hex string representation of the goid
     *
     * @param goid The goid to return the hex string representation of.
     * @return The hex string representation of the goid
     */
    public static String toString(@NotNull Goid goid) {
        return goid.toHexString();
    }

    /**
     * Compares two goids they are equal if their bytes are equal, or if they are both null.
     *
     * @param goid1 the first goid
     * @param goid2 the second goid
     * @return true iff both goid bytes are the same or if both goids are null.
     */
    public static boolean equals(@Nullable Goid goid1, @Nullable Goid goid2) {
        return goid1 == null ? goid2 == null : goid1.equals(goid2);
    }

    /**
     * Checks if the Goid given is equal to the default Goid.
     *
     * @param goid the goid to check to see if it is default
     * @return true if the goid is equal to the default goid, false otherwise.
     */
    public static boolean isDefault(@Nullable Goid goid) {
        return GoidEntity.DEFAULT_GOID.equals(goid);
    }

    /**
     * Compares this going to the one give. 0 is returned if both Goid's are equal. -1 is returned if this Goid is less
     * then the one given. 1 is returned otherwise.
     *
     * @param o The goid to compare to
     * @return -1 if this going is less then the one given, 0 if they are equal, 1 if this goid is greater then the one
     *         given.
     */
    @Override
    public int compareTo(@NotNull Goid o) {
        return (getHi() < o.getHi()) ? -1 : ((getHi() == o.getHi()) ? ((getLow() < o.getLow()) ? -1 : ((getLow() == o.getLow()) ? 0 : 1)) : 1);
    }
}
