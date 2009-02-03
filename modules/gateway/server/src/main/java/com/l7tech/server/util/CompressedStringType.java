package com.l7tech.server.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import com.l7tech.util.ResourceUtils;
import com.l7tech.util.IOUtils;

/**
 * A Hibernate UserType that stores Strings as (Compressed) a blob/byte[].
 *
 * <p>The compressed string contents are stored the following (MySQL specific)
 * way:</p>
 *
 * <p>Empty strings are stored as empty strings.</p>
 *
 * <p>Non-empty strings are stored as a four-byte length of the uncompressed
 * string (low byte first), followed by the compressed string. If the string
 * ends with space, an extra ?.? character is added to avoid problems with
 * endspace trimming should the result be stored in a CHAR or VARCHAR column.
 * (Use of CHAR or VARCHAR to store compressed strings is not recommended.
 * It is better to use a BLOB column instead.)</p>
 *
 * <p>This class does NOT add '.' to the string, so this is a minor
 * incompatibility with MySQL COMPRESS().</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class CompressedStringType implements UserType {

    //- PUBLIC

    public CompressedStringType() {
    }

    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (String) value;
    }

    public boolean equals(Object value1, Object value2) throws HibernateException {
        boolean equal = false;

        if (value1 == value2) {
            equal = true;
        }
        else if (value1 instanceof String &&
                 value2 instanceof String) {
            equal = value1.equals(value2);
        }

        return equal;
    }

    public int hashCode(Object value) throws HibernateException {
        return value==null ? 0 : value.hashCode();
    }

    public boolean isMutable() {
        return false;
    }

    public static String decompress(final byte[] blob) throws SQLException {
        String uncompressed = null;
        InputStream blobIn = null;
        try {
            blobIn = new ByteArrayInputStream(blob);
            byte[] lenBytes = new byte[4];
            int read = blobIn.read(lenBytes);
            if (read == 4) { // got length
                long length = bytesToLength(lenBytes);
                if (length > Integer.MAX_VALUE || length < 0) {
                    throw new HibernateException("Invalid compressed string length '" + length + "'.");
                }
                int dataLength = (int) length;
                InflaterInputStream infIn = new InflaterInputStream(blobIn);
                byte[] expandedData = new byte[dataLength];
                int dataRead = IOUtils.slurpStream(infIn, expandedData);
                if (dataRead != dataLength) {
                    throw new HibernateException("Incorrect amount of data read for compressed string "+dataRead+"!="+dataLength+".");
                }
                uncompressed = new String(expandedData, "UTF-8");
            }
            else {
                uncompressed = ""; // empty != null
            }
        }
        catch (IOException ioe) {
            throw (SQLException) new SQLException("Error reading blob").initCause(ioe);
        }
        finally {
            ResourceUtils.closeQuietly(blobIn);
        }

        return uncompressed;
    }

    public static byte[] compress(final String text) throws SQLException  {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(text.length());
        DeflaterOutputStream defOut = null;
        try {
            if (text.length() > 0) {
                byte[] dataBytes = text.getBytes("UTF-8");
                baos.write(lengthToBytes(dataBytes.length));
                defOut = new DeflaterOutputStream(baos);
                defOut.write(dataBytes);
                defOut.close();
            }
        }
        catch (IOException ioe) {
            throw (SQLException) new SQLException("Error creating blob.").initCause(ioe);
        }
        finally {
            ResourceUtils.closeQuietly(defOut);
        }

        return baos.toByteArray();
    }

    public Object nullSafeGet(ResultSet resultSet, String[] names, Object owner) throws HibernateException, SQLException {
        if (names==null || names.length!=1) throw new HibernateException("Expected single column mapping.");

        String uncompressed = null;
        byte[] blob = resultSet.getBytes(names[0]);
        if (blob != null) {
            uncompressed = decompress(blob);
        }

        return uncompressed;
    }

    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index) throws HibernateException, SQLException {
        if (value != null) {
            String text = (String) value;
            byte[] data = compress(text);
            preparedStatement.setBytes(index, data);
        }
        else {
            preparedStatement.setNull(index, Types.BLOB);
        }
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    public Class returnedClass() {
        return String.class;
    }

    public int[] sqlTypes() {
        return new int[]{ Types.BLOB };
    }

    //- PRIVATE

    private static byte[] lengthToBytes(long length) {
        byte[] lengthBytes = new byte[4];

        lengthBytes[0] |= (length & 0x000000FFL) >>  0;
        lengthBytes[1] |= (length & 0x0000FF00L) >>  8;
        lengthBytes[2] |= (length & 0x00FF0000L) >> 16;
        lengthBytes[3] |= (length & 0xFF000000L) >> 24;

        return lengthBytes;
    }

    private static long bytesToLength(byte[] lengthBytes) {
        long length = 0;

        if (lengthBytes.length == 4) {
            length |= (lengthBytes[0]&0xFFL) <<  0;
            length |= (lengthBytes[1]&0xFFL) <<  8;
            length |= (lengthBytes[2]&0xFFL) << 16;
            length |= (lengthBytes[3]&0xFFL) << 24;
        }

        return length;
    }

}
