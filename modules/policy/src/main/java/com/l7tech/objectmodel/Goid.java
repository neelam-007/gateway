package com.l7tech.objectmodel;

import com.l7tech.util.ArrayUtils;
import org.apache.commons.codec.binary.Base64;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Goid Object represents a global object id for the gateways entities.
 *
 * @author Victor Kazakov
 */
public class Goid implements Serializable {

    //This is used to convert a goid to and from string representations.
    private static final Base64 base64 = new Base64(-1, null, true);
    //The bytes of the goid
    private byte[] goid;

    /**
     * Creates a new goid from two long values
     *
     * @param high The high long will be the first 8 bytes of the goid
     * @param low  The low long will be the last 8 bytes of the goid
     */
    public Goid(long high, long low) {
        goid = ArrayUtils.concat(ByteBuffer.allocate(8).putLong(high).array(), ByteBuffer.allocate(8).putLong(low).array());
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
            throw new IllegalArgumentException("Cannot create a goid from a byte array that is not 128 bytes long.");
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
    public Goid(String goid) {
        byte[] goidFromString = base64.decode(goid);
        if (goidFromString.length != 16) {
            throw new IllegalArgumentException("Cannot create a goid from this String, it does not decode to a 128 byte array.");
        }
        this.goid = goidFromString;
    }

    /**
     * Creates a new goid from another goid. The id's will be identical
     *
     * @param goid The goid to clone.
     */
    public Goid(Goid goid) {
        this(goid.getBytes());
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
    public boolean equals(Object o) {
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

    @Override
    public String toString() {
        return base64.encodeToString(goid);
    }
}
