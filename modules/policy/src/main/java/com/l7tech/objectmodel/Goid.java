package com.l7tech.objectmodel;

import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.DefaultPersistenceDelegate;
import java.beans.PersistenceDelegate;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Goid Object represents a global object id for the gateways entities.
 *
 * @author Victor Kazakov
 */
public final class Goid implements Comparable<Goid>, Serializable {
    //This is the default Goid
    public static final Goid DEFAULT_GOID = new Goid(0, -1);

    private long high = 0;
    private long low = -1;

    /**
     * This is needed for serialization
     */
    @SuppressWarnings("UnusedDeclaration")
    private Goid() {
    }

    /**
     * Creates a new goid from two long values
     *
     * @param high The high long will be the first 8 bytes of the goid
     * @param low  The low long will be the last 8 bytes of the goid
     */
    public Goid(long high, long low) {
        this.high = high;
        this.low = low;
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
        ByteBuffer buffer = ByteBuffer.wrap(goid);
        this.high = buffer.getLong();
        this.low = buffer.getLong();
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
        ByteBuffer buffer = ByteBuffer.wrap(goidFromString);
        this.high = buffer.getLong();
        this.low = buffer.getLong();
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
        return high;
    }

    /**
     * Returns the low 8 bytes of the goid as a long
     *
     * @return The low 8 bytes of the goid as a long
     */
    public long getLow() {
        return low;
    }

    /**
     * Returns the bytes of this goid
     *
     * @return The bytes of this goid
     */
    public byte[] getBytes() {
        return ByteBuffer.allocate(16).putLong(high).putLong(low).array();
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Goid goid = (Goid) o;

        return high == goid.high && low == goid.low;
    }

    @Override
    public int hashCode() {
        int result = (int) (high ^ (high >>> 32));
        result = 31 * result + (int) (low ^ (low >>> 32));
        return result;
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
        return HexUtils.hexDump(getBytes());
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
        return PersistentEntity.DEFAULT_GOID.equals(goid);
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
