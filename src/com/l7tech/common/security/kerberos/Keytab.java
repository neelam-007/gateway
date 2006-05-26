package com.l7tech.common.security.kerberos;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.HexUtils;

/**
 * Parser for Kerberos Keytab files.
 *
 * <p><b>The Kerberos Keytab Binary File Format</b></p>
 *
 * <p><code>Michael B Allen <mba2000 ioplex.com><br/>
 * Last updated: Thu May  4 13:16:23 EDT 2006</code></p>
 *
 * <p>The MIT keytab binary format is not a standard format, nor is it
 * documentated anywhere in detail. The format has evolved and may continue
 * to. It is however understood by several Kerberos implementations including
 * Heimdal and of course MIT and keytab files are created by the ktpass.exe
 * utility from Windows. So it has established itself as the defacto format
 * for storing Kerberos keys.</p>
 *
 * <p>The following C-like structure definitions illustrate the MIT keytab
 * file format. All values are in network byte order. All text is ASCII.</p>
 *
 * <pre>
 *   keytab {
 *       uint16_t file_format_version;                    // 0x502
 *       keytab_entry entries[*];
 *   };
 *
 *   keytab_entry {
 *       int32_t size;
 *       uint16_t num_components;   //subtract 1 if version 0x501
 *       counted_octet_string realm;
 *       counted_octet_string components[num_components];
 *       uint32_t name_type;       // not present if version 0x501
 *       uint32_t timestamp;
 *       uint8_t vno8;
 *       keyblock key;
 *       uint32_t vno; // only present if >= 4 bytes left in entry
 *   };
 *
 *   counted_octet_string {
 *       uint16_t length;
 *       uint8_t data[length];
 *   };
 *
 *   keyblock {
 *       uint16_t type;
 *       counted_octet_string;
 *   };
 *  </pre>
 *
 * <p>The keytab file format begins with the 16 bit file_format_version which
 * at the time this document was authored is 0x502. The format of older
 * keytabs is described at the end of this document.</p>
 *
 * <p>The file_format_version is immediately followed by an array of
 * keytab_entry structures which are prefixed with a 32 bit size indicating
 * the number of bytes that follow in the entry. Note that the size should be
 * evaluated as signed. This is because a negative value indicates that the
 * entry is in fact empty (e.g. it has been deleted) and that the negative
 * value of that negative value (which is of course a positive value) is
 * the offset to the next keytab_entry. Based on these size values alone
 * the entire keytab file can be traversed.</p>
 *
 * <p>The size is followed by a 16 bit num_components field indicating the
 * number of counted_octet_string components *minus one* that constitute
 * the name. A counted_octet_string is simply an array of bytes prefixed
 * with a 16 bit length. For the name components, the counted_octet_string
 * bytes are ASCII encoded text with no zero terminator.</p>
 *
 * <p>Following the components array is the 32 bit name_type (e.g. 1 is
 * KRB5_NT_PRINCIPAL, 2 is KRB5_NT_SRV_INST, 5 is KRB5_NT_UID, etc).</p>
 *
 * <p>In practice the name_type is almost certainly 1 meaning
 * KRB5_NT_PRINCIPAL. For this type of entry the components array consists
 * of the realm, service and name (in that order) although if num_components
 * is 1, the service component is not present. For example, the service
 * principal HTTP/quark.foo.net@FOO.NET would be encoded with name components
 * "FOO.NET" followed by "HTTP" followed by "quark.foo.net".</p>
 *
 * <p>The 32 bit timestamp indicates the time the key was established for that
 * principal. The value represents the number of seconds since Jan 1, 1970.</p>
 *
 * <p>The 8 bit vno8 field is the version number of the key. This value is
 * overridden by the vno field if it is present.</p>
 *
 * <p>The keyblock structure consists of a 16 bit value indicating the keytype
 * (e.g. 3 is des-cbc-md5, 23 is arcfour-hmac-md5, 16 is des3-cbc-sha1,
 * etc). This is followed by a counted_octet_string containing the key.</p>
 *
 * <p>The last field of the keytab_entry structure is optional. If the size of
 * the keytab_entry indicates that there are at least 4 bytes remaining,
 * a 32 bit value representing the key version number is present. This
 * value superceeds the 8 bit value preceeding the keyblock.</p>
 *
 * <p>Keytabs with a file_format_version of 0x501 are different in three ways:</p>
 *
 * <ol>
 *  <li> All integers are in host byte order [1].</li>
 *  <li> The num_components field is 1 too large (i.e. after decoding,
 *     decrement by 1).</li>
 *  <li> The 32 bit name_type field is not present.</li>
 * </ol>
 *
 * <p>[1] The file_format_version field should really be treated as two
 *    separate 8 bit quantities representing the major and minor version
 *    number respectively.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class Keytab implements Serializable {

    //- PUBLIC

    /**
     * Create a Keytab from the given file.
     *
     * @param keytabFile The Keytab file.
     * @throws IllegalArgumentException if keytabFile is null
     * @throws IOException if the file cannot be read
     * @throws KerberosException if the Keytab is not valid
     */
    public Keytab(File keytabFile) throws IOException, KerberosException {
        if (keytabFile == null) throw new IllegalArgumentException("keytabFile must not be null");
        InputStream in = null;
        try {
            in = new FileInputStream(keytabFile);
            init(HexUtils.slurpStream(in));
        }
        finally {
            ResourceUtils.closeQuietly(in);
        }
    }

    /**
     * Create a keytab from the given bytes.
     *
     * @param keytabBytes The Keytab data.
     * @throws IllegalArgumentException if keytabBytes is null
     * @throws KerberosException if the Keytab is not valid
     */
    public Keytab(byte[] keytabBytes) throws KerberosException {
        if (keytabBytes == null) throw new IllegalArgumentException("keytabBytes must not be null");
        init(keytabBytes);
    }

    /**
     * Get the major version number for the Keytab file.
     *
     * @return 5 which is the only major version supported.
     */
    public int getVersionMajor() {
        return versionMajor;
    }

    /**
     * Get the minor version number for the Keytab file.
     *
     * @return The minor version (versions 1 & 2 supported)
     */
    public int getVersionMinor() {
        return versionMinor;
    }

    /**
     * Get the name of the entry.
     *
     * @return The entry name
     */
    public String[] getKeyName() {
        return keyName;
    }

    /**
     * Get the realm of the entry.
     *
     * @return The entry realm
     */
    public String getKeyRealm() {
        return keyRealm;
    }

    /**
     * Get the timestamp for the entry.
     *
     * <p>This is the number of millis since the epoch. This will be zero if no
     * timestamp is present for the entry.</p>
     *
     * @return The entry timestamp
     */
    public long getKeyTimestamp() {
        return keyTimestamp;
    }

    /**
     * Get the entry version number.
     *
     * @return The version number.
     */
    public long getKeyVersionNumber() {
        return keyVersionNumber;
    }

    /**
     * Console util for listing Keytab data.
     *
     * @throws Exception usually...
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage:\n\tKeytab <keytab-file>\n");
            return;
        }

        Keytab keytab = new Keytab(new File(args[0]));
        System.out.println("Keytab:- ");
        System.out.println("Major Version: " + keytab.getVersionMajor());
        System.out.println("Minor Version: " + keytab.getVersionMinor());

        System.out.println("\nEntry:- ");
        System.out.println("Realm  : " + keytab.getKeyRealm());
        System.out.println("Name   : " + Arrays.asList(keytab.getKeyName()));
        System.out.println("Date   : " + (keytab.getKeyTimestamp()==0 ? "<none>" : new Date(keytab.getKeyTimestamp()).toString()));
        System.out.println("Version: " + keytab.getKeyVersionNumber());
    }

    //- PRIVATE

    private static final long serialVersionUID = 1L;

    private transient boolean networkOrder;    
    private int versionMajor;
    private int versionMinor;
    private String keyRealm;
    private String[] keyName;
    private long keyTimestamp;
    private long keyVersionNumber;

    /**
     *
     */
    private void init(byte[] data) throws KerberosException {
        networkOrder = true;

        if (data.length < 2) throw new KerberosException("No version found.");
        versionMajor = readUnsignedChar(data, 0);
        versionMinor = readUnsignedChar(data, 1);

        if (versionMinor == 5 && versionMajor==1) {
            // detect non-network order for v1 keytab
            versionMajor = 5;
            versionMinor = 1;
            networkOrder = false;
        }

        if (versionMajor != 5) throw new KerberosException("Major version '"+versionMajor+"' not known.");
        if (versionMinor != 1 &&
            versionMinor != 2) throw new KerberosException("Minor version '"+versionMinor+"' not known.");

        int currentOffset = 2;
        int nextEntryOffset = 2;
        while (nextEntryOffset < data.length) {
            currentOffset = nextEntryOffset;
            if (currentOffset+4 > data.length) break;

            // entry size
            long entrySize = readUnsignedInt(data, currentOffset);
            currentOffset += 4;
            nextEntryOffset = currentOffset + (int)(entrySize & Integer.MAX_VALUE);
            if (nextEntryOffset < 0) break; // something wrong,
            if (entrySize > Integer.MAX_VALUE) {
                // Then the entry is deleted, just skip it
                continue;
            }

            if (validEntry()) throw new KerberosException("Only single-entry Keytabs are supported.");

            // components
            if (currentOffset+2 > data.length) break;
            int components = readUnsignedShort(data, currentOffset);
            currentOffset += 2;
            if (versionMinor == 1) components -= 1;

            // realm
            if (currentOffset+2 > data.length) break;
            int realmLength = readUnsignedShort(data, currentOffset);
            currentOffset += 2;

            if (currentOffset + realmLength > data.length) break;
            char[] realmChars = new char[realmLength];
            for (int c=0; c<realmChars.length; c++)
                realmChars[c] = (char)readUnsignedChar(data, currentOffset+c);
            keyRealm = new String(realmChars);
            currentOffset += realmLength;

            // name
            if (components > 10) break;
            String[] name = new String[components];
            for (int n=0; n<components; n++) {
                if (currentOffset+2 > data.length) break;
                int nameLength = readUnsignedShort(data, currentOffset);
                currentOffset += 2;

                if (currentOffset + nameLength > data.length) break;
                char[] nameChars = new char[nameLength];
                for (int c=0; c<nameChars.length; c++)
                    nameChars[c] = (char)readUnsignedChar(data, currentOffset+c);
                currentOffset += nameLength;
                name[n] = new String(nameChars);
            }
            keyName = name;

            // skip name type (if present)
            if (versionMinor != 1) currentOffset += 4;

            // timestamp
            if (currentOffset+4 > data.length) break;
            keyTimestamp = readUnsignedInt(data, currentOffset) * 1000L; // convert from secs to millis
            currentOffset += 4;

            // short vno
            if (currentOffset+1 > data.length) break;
            keyVersionNumber = readUnsignedChar(data, currentOffset);
            currentOffset += 1;

            // skip keyblock
            if (currentOffset+2 > data.length) break;
            currentOffset += 2; // skip key type

            if (currentOffset+2 > data.length) break;
            int keyBlockLength = readUnsignedShort(data, currentOffset);
            currentOffset += 2;
            currentOffset += keyBlockLength; // skip key data

            // long vno (if present)
            if (currentOffset+4 <= data.length &&
                currentOffset+4 <= nextEntryOffset) {
                keyVersionNumber = readUnsignedInt(data, currentOffset);
            }
        }

        checkEntry();
    }

    /**
     *
     */
    private void checkEntry() throws KerberosException {
        if (!validEntry()) throw new KerberosException("No entry found.");
    }

    /**
     *
     */
    private boolean validEntry() {
        boolean valid = false;

        if (keyRealm != null) {
            if (keyName.length > 0) {
                boolean nameValid = true;
                for (int n=0; n<keyName.length; n++) {
                    if (keyName[n] == null) {
                        nameValid = false;
                        break;
                    }
                }

                if (nameValid) {
                    if (keyVersionNumber > 0) valid = true;
                }
            }
        }

        return valid;
    }

    /**
     *
     */
    private int readUnsignedChar(byte[] data, int offset) {
        if(data.length < (offset+1)) throw new IllegalArgumentException("Not enough data to read short!");

        return (data[offset+0]&0xFF);
    }

    /**
     *
     */
    private int readUnsignedShort(byte[] data, int offset) {
        if(data.length < (offset+2)) throw new IllegalArgumentException("Not enough data to read short!");

        if (networkOrder)
            return (data[offset+1]&0xFF)
                | ((data[offset+0]&0xFF) <<  8);
        else
            return (data[offset+0]&0xFF)
                | ((data[offset+1]&0xFF) <<  8);
    }

    /**
     *
     */
    private long readUnsignedInt(byte[] data, int offset) {
        if(data.length < (offset+4)) throw new IllegalArgumentException("Not enough data to read int!");

        if (networkOrder)
            return (data[offset+3]&0xFFL)
                | ((data[offset+2]&0xFFL) <<  8)
                | ((data[offset+1]&0xFFL) << 16)
                | ((data[offset+0]&0xFFL) << 24);
        else
            return (data[offset+0]&0xFFL)
                | ((data[offset+1]&0xFFL) <<  8)
                | ((data[offset+2]&0xFFL) << 16)
                | ((data[offset+3]&0xFFL) << 24);
    }
}
