package com.l7tech.kerberos;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosKey;

import com.l7tech.util.ResourceUtils;
import com.l7tech.util.HexUtils;

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
        keyData = new ArrayList<KeyData>();
        InputStream in = null;
        try {
            in = new FileInputStream(keytabFile);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            HexUtils.copyStream(in, out);
            init(out.toByteArray());
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
        keyData = new ArrayList<KeyData>();
        init(keytabBytes);
    }

    /**
     * Create a keytab for the given principal and credentials.
     *
     * <p>This will create a Keytab with the kvno 1 and with
     * RC4 and DES keys.</p>
     *
     * @param keytabFile The Keytab file.
     * @param principal The principal name (http/server.domain.com@DOMAIN.COM)
     * @param password The users credentials
     * @param desOnly True for DES only key
     */
    public Keytab(File keytabFile, String principal, String password, boolean desOnly) throws KerberosException {
        if (keytabFile == null) throw new IllegalArgumentException("keytabFile must not be null");
        keyData = new ArrayList<KeyData>();

        networkOrder = true;        
        versionMajor = 5;
        versionMinor = 2;

        keyName = parseName(principal);
        keyRealm = keyName[0];

        long timestamp = System.currentTimeMillis();
        if (!desOnly) {
            KeyData keyDataItem = new KeyData();
            keyDataItem.timestamp = timestamp;
            keyDataItem.type = 23;
            keyDataItem.version = 1;
            keyData.add(keyDataItem);
        }

        KeyData keyDataItem = new KeyData();
        keyDataItem.timestamp = timestamp;
        keyDataItem.type = 3;
        keyDataItem.version = 1;
        keyData.add(keyDataItem);

        FileOutputStream out = null;
        try {
            if (keytabFile.exists())
                throw new KerberosException("File exists '"+keytabFile.getAbsolutePath()+"'.");

            out = new FileOutputStream(keytabFile);
            init(this, principal, password, desOnly, out);
        }
        catch(Exception e) {
            throw new KerberosException("Error writing keytab.", e);
        }
        finally {
            ResourceUtils.closeQuietly(out);
        }
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
        long timestamp = 0;

        if (!keyData.isEmpty()) {
            timestamp = keyData.get(0).timestamp;
        }

        return timestamp;
    }

    /**
     * Get the entry version number.
     *
     * @return The version number.
     */
    public long getKeyVersionNumber() {
        long version = 0;

        if (!keyData.isEmpty()) {
            version = keyData.get(0).version;
        }

        return version;
    }

    /**
     * Get the key types present in the Keytab.
     *
     * @return The key types
     */
    public String[] getKeyTypes() {
        List<String> types = new ArrayList<String>();

        for( KeyData keyDataEntry : keyData ) {
            try {
                String typeDesc = KerberosConstants.ETYPE_NAMES[keyDataEntry.type];
                if( typeDesc != null ) {
                    types.add( typeDesc );
                } else {
                    types.add( "unknown [" + keyDataEntry.type + "]" );
                }
            }
            catch( ArrayIndexOutOfBoundsException aioobe ) {
                types.add( "unknown [" + keyDataEntry.type + "]" );
            }
        }

        return types.toArray(new String[types.size()]);
    }

    /**
     * Console util for listing Keytab data.
     *
     * @throws Exception usually...
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1 && args.length != 3) {
            System.out.println("Usage:\n\tKeytab <keytab-file> [<principal> <password>]\n");
            return;
        }

        Keytab keytab;
        if (args.length == 1) {
            keytab = new Keytab(new File(args[0]));
        } else {
            keytab = new Keytab(new File(args[0]), args[1], args[2], false);
        }

        System.out.println("Keytab:- ");
        System.out.println("Major Version: " + keytab.getVersionMajor());
        System.out.println("Minor Version: " + keytab.getVersionMinor());

        System.out.println("\nEntry:- ");
        System.out.println("Realm  : " + keytab.getKeyRealm());
        System.out.println("Name   : " + Arrays.asList(keytab.getKeyName()));
        System.out.println("Date   : " + (keytab.getKeyTimestamp()==0 ? "<none>" : new Date(keytab.getKeyTimestamp()).toString()));
        System.out.println("Version: " + keytab.getKeyVersionNumber());
        System.out.println("Types  : " + Arrays.asList(keytab.getKeyTypes()));
    }

    //- PRIVATE

    private static final long serialVersionUID = 1L;

    private transient boolean networkOrder;    
    private int versionMajor;
    private int versionMinor;
    private String keyRealm;
    private String[] keyName;
    private final List<KeyData> keyData;

    /**
     * 
     */
    private void init(Keytab keytab, String username, String password, boolean desOnly, OutputStream out) throws IOException {
        KerberosPrincipal principal = new KerberosPrincipal(username, KerberosPrincipal.KRB_NT_PRINCIPAL);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // keytab version number
        baos.write(5);
        baos.write(2);

        for (int i=0; i<2; i++) {
            if (desOnly) i++;
            String algorithm = i==0 ? "ArcFourHmac" : "DES";
            int type =  i==0 ? 23 : 3;

            ByteArrayOutputStream entry = new ByteArrayOutputStream();

            // name
            entry.write(0);
            entry.write(2); // since 2 means 3
            String[] name = keytab.getKeyName();
            for (String part : name) {
                byte[] partBytes = part.getBytes("UTF-8");
                int length = partBytes.length;
                entry.write((length>>8)&0xFF);
                entry.write(length&0xFF);
                entry.write(partBytes);
            }

            // type 1 (principal)
            entry.write(0);
            entry.write(0);
            entry.write(0);
            entry.write(1);

            // timestamp
            int time = (int)(keytab.getKeyTimestamp() / 1000L);
            entry.write((time>>>24)&0xFF);
            entry.write((time>>>16)&0xFF);
            entry.write((time>>> 8)&0xFF);
            entry.write(time&0xFF);

            // vno
            entry.write(1);

            // key type
            entry.write(0);
            entry.write(type);

            // key data
            KerberosKey key = new KerberosKey(principal, password.toCharArray(), algorithm);
            byte[] keyBytes = key.getEncoded();
            entry.write((keyBytes.length>>8)&0xFF);
            entry.write(keyBytes.length&0xFF);
            entry.write(keyBytes);

            byte[] entryData = entry.toByteArray();
            baos.write(0);
            baos.write(0);
            baos.write((entryData.length>>8)&0xFF);
            baos.write(entryData.length&0xFF);
            baos.write(entryData);
        }

        out.write(baos.toByteArray());
    }

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

        int currentOffset;
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
            if (keyName == null) {
                keyName = name;
            }
            else {
                checkPrincipal(keyName, name);
            }

            // skip name type (if present)
            if (versionMinor != 1) currentOffset += 4;

            // now the key info
            KeyData entryKeyData = new KeyData();

            // timestamp
            if (currentOffset+4 > data.length) break;
            entryKeyData.timestamp = readUnsignedInt(data, currentOffset) * 1000L; // convert from secs to millis
            currentOffset += 4;

            // short vno
            if (currentOffset+1 > data.length) break;
            entryKeyData.version = readUnsignedChar(data, currentOffset);
            currentOffset += 1;

            // skip keyblock
            if (currentOffset+2 > data.length) break;
            entryKeyData.type = readUnsignedShort(data, currentOffset);
            currentOffset += 2; // skip key type

            if (currentOffset+2 > data.length) break;
            int keyBlockLength = readUnsignedShort(data, currentOffset);
            currentOffset += 2;
            currentOffset += keyBlockLength; // skip key data

            // long vno (if present)
            if (currentOffset+4 <= data.length &&
                currentOffset+4 <= nextEntryOffset) {
                entryKeyData.version = readUnsignedInt(data, currentOffset);
            }

            checkKey(entryKeyData);
            keyData.add(entryKeyData);
        }

        checkEntry();
    }

    private String[] parseName(String fullName) throws KerberosException {
        String[] name;
        String[] parts = fullName.split("[/@]");

        if (parts.length == 3) {
            name = new String[]{parts[2].toUpperCase(), parts[0], parts[1]};       
        }
        else if (parts.length == 2) {
            name = new String[]{parts[1], "http", parts[0]};
        }
        else if (parts.length == 1) {
            String realm = parts[0].substring(parts[0].indexOf('.')+1).toUpperCase();
            name = new String[]{realm, "http", parts[0]};
        }
        else {
            throw new KerberosException("Unsupported name format, number of parts is " + parts.length + ".");
        }

        return name;
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
    private void checkPrincipal(String[] name1, String[] name2) throws KerberosException {
        if (!Arrays.equals(name1, name2))
            throw new KerberosException("Keytab contains entries for multiple principals (not supported; " +
                    "principals are "+Arrays.asList(name1)+", "+Arrays.asList(name2)+").");
    }

    /**
     *
     */
    private void checkKey(KeyData data) throws KerberosException {
        if (data.version <= 0) throw new KerberosException("Invalid key version '"+data.version+"'.");
        if (data.type <= 0 || data.type > 23) throw new KerberosException("Invalid key etype '"+data.type+"'.");

        if (!keyData.isEmpty() && data.version != getKeyVersionNumber()) {
            throw new KerberosException("Mismatched key version numbers ('"+data.version+"', '"+getKeyVersionNumber()+"')");
        }
    }

    /**
     *
     */
    private boolean validEntry() {
        boolean valid = false;

        if (keyRealm != null) {
            if (keyName.length > 0) {
                boolean nameValid = true;
                for( String aKeyName : keyName ) {
                    if( aKeyName == null ) {
                        nameValid = false;
                        break;
                    }
                }

                if (nameValid && !keyData.isEmpty()) {
                    valid = true;
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

        return (data[offset]&0xFF);
    }

    /**
     *
     */
    private int readUnsignedShort(byte[] data, int offset) {
        if(data.length < (offset+2)) throw new IllegalArgumentException("Not enough data to read short!");

        if (networkOrder)
            return (data[offset+1]&0xFF)
                | ((data[offset]&0xFF) <<  8);
        else
            return (data[offset]&0xFF)
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
                | ((data[offset]&0xFFL) << 24);
        else
            return (data[offset]&0xFFL)
                | ((data[offset+1]&0xFFL) <<  8)
                | ((data[offset+2]&0xFFL) << 16)
                | ((data[offset+3]&0xFFL) << 24);
    }

    /**
     *
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        networkOrder = true;
    }    

    private static class KeyData implements Serializable {
        private long timestamp;
        private long version;
        private int type;
    }
}
