package com.l7tech.objectmodel;

import com.l7tech.util.HexUtils;
import com.l7tech.util.XmlSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Goid Object represents a global object id for the gateways entities.
 *
 * @author Victor Kazakov
 */
@XmlSafe
public final class Goid implements Comparable<Goid>, Serializable {
    //This is the default Goid
    public static final Goid DEFAULT_GOID = new Goid(0, -1);

    private long high = 0;
    private long low = -1;

    /**
     * This is needed for serialization
     */
    @SuppressWarnings("UnusedDeclaration")
    @XmlSafe
    private Goid() {
    }

    /**
     * Creates a new goid from two long values.
     * <p/>
     * This method is intended to be used for unit tests and should generally <b>not be used in production</b>
     * because the Goid type may be widened in the future to be larger than 128 bits.
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
    @XmlSafe
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
     * @param goid A string representation of a goid, possibly in compressed form as returned by {@link #toCompressedString()}.
     * @throws IllegalArgumentException This is thrown if the given string does not represent a goid
     */
    @XmlSafe
    public Goid( @NotNull String goid ) {
        final String goidHex;
        if ( goid.length() == 32 ) {
            // Bypass decompression for performance in common case
            goidHex = goid;
        } else {
            goidHex = decompressString( goid );
        }

        byte[] goidFromString;
        try {
            goidFromString = HexUtils.unHexDump( goidHex );
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
        // Encode as call to constructor-from-string using the value returned from toString().
        return new PersistenceDelegate() {
            @Override
            protected Expression instantiate(Object oldInstance, Encoder out) {
                return new Expression(oldInstance, Goid.class, "new", new Object[]{oldInstance.toString()});
            }
        };
    }

    /**
     * Returns the 8 high bytes of the goid as a long.
     * <p/>
     * This method should generally <b>not be used in production</b>
     * because the Goid type may be widened in the future to be larger than 128 bits.
     *
     * @return The high 8 bytes of the goid as a long.
     */
    public long getHi() {
        return high;
    }

    /**
     * Returns the low 8 bytes of the goid as a long
     * <p/>
     * This method should generally <b>not be used in production</b>
     * because the Goid type may be widened in the future to be larger than 128 bits.
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

    static final Pattern PAT_LEADING_ZEROES = Pattern.compile( "^00+" );
    static final Pattern PAT_LEADING_ONES = Pattern.compile( "^ff+", Pattern.CASE_INSENSITIVE );

    /**
     * Convert the Goid into a string in compressed format.
     * <p/>
     * The compressed format is designed for representing default or built-in goids rather than
     * random ones.  Runs of arbitrarily many "0" or "f" characters leading a 16-character block may be replaced
     * with "z" or "n" characters respectively.
     * <p/>
     * Examples:
     * <pre>
     *     Uncompressed                     Compressed                       Notes
     *     ================================ ================================ ======================================================
     *     abcdef0123456789abcdef0123456789 abcdef0123456789abcdef0123456789 Uncompressible because no runs are present
     *     abcd000000000000abcd000000000000 abcd000000000000abcd000000000000 Uncompressible because runs are at end of each segment
     *     aaaaaaaaaaaaaa12777777777777777b aaaaaaaaaaaaaa12777777777777777b Uncompressible because only runs of 0 and f are supported
     *     00000000000000000000000000000000 zz                               Run of 16 zeroes, followed by run of 16 zeros
     *     00000000000000000000000000000123 zz123                            Run of 16 zeroes, followed by run of (16 - "123".length()) = 13 zeros, followed by "123"
     *     ffffffffffffffffffffffffffffffff nn                               Run of 16 "f"s ("(n)egatives"), followed by run of 16 "f"s
     *     fffffffffffabc12000000000000000b nabc12zb                         Run of (16 - "abc12".length()) = 11 "f"s followed by "abc12",
     *                                                                           followed by run of (16 - "b".length()) = 15 zeros followed by "b"
     *     0000000000000000ffffffffffffffff zn                               Run of zeros, run of "f"s -- this is Goid.DEFAULT_GOID
     *     0000000000000000fffffffffffffffe zne                              Run of zeros, run of "fs", then "e" -- this is new Goid(0, -2), the Internal Identity Provider ID
     * </pre>
     * See GoidTest for more examples.
     *
     * @return the compressed string form of this Goid, which will be the same as {@link #toHexString()} unless runs of leading "0"s or "f"s are present.
     */
    public String toCompressedString() {
        return compressString( new StringBuilder( toHexString() ) );
    }

    static String compressString( StringBuilder s ) {
        StringBuilder out = new StringBuilder();

        while ( s.length() >= 16 ) {
            CharSequence a = s.subSequence( 0, 16 );
            s = s.delete( 0, 16 );
            a = PAT_LEADING_ONES.matcher( a ).replaceFirst( "n" );
            a = PAT_LEADING_ZEROES.matcher( a ).replaceFirst( "z" );
            out.append( a );
        }
        out.append( s );

        return out.toString();
    }

    static final Pattern PAT_TRAILING_HEX = Pattern.compile( "[0-9a-f]+$", Pattern.CASE_INSENSITIVE );
    static final String[] ZEROS;
    static final String[] ONES;
    static {
        StringBuilder sbz = new StringBuilder();
        StringBuilder sbf = new StringBuilder();
        String[] z = new String[17];
        String[] f = new String[17];
        for ( int i = 0; i <= 16; ++ i ) {
            z[i] = sbz.toString();
            sbz.append( "0" );

            f[i] = sbf.toString();
            sbf.append( "f" );
        }

        ZEROS = z;
        ONES = f;
    }

    static String decompressString( @NotNull final String goidString ) {
        StringBuilder sb = new StringBuilder();
        String string = goidString;

        while ( string.length() > 0 ) {
            Matcher m = PAT_TRAILING_HEX.matcher( string );
            if ( m.find() ) {
                string = m.replaceFirst( "" );
                sb.insert( 0, m.group( 0 ) );
            } else if ( string.endsWith( "n" ) || string.endsWith( "N" ) ) {
                string = string.substring( 0, string.length() - 1 );
                int neededNybbles = 16 - sb.length() % 16;
                sb.insert( 0, ONES[neededNybbles] );
            } else if ( string.endsWith( "z" ) || string.endsWith( "Z" ) ) {
                string = string.substring( 0, string.length() - 1 );
                int neededNybbles = 16 - sb.length() % 16;
                sb.insert( 0, ZEROS[neededNybbles] );
            } else {
                throw new IllegalArgumentException( "Invalid Goid (unrecognized suffix): " + goidString );
            }
        }

        return sb.toString();
    }
}
