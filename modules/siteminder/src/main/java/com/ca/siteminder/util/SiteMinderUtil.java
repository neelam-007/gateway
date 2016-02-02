package com.ca.siteminder.util;

import com.ca.siteminder.SiteMinderContext;
import com.ca.siteminder.SiteMinderCredentials;
import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.gateway.common.siteminder.SiteMinderFipsModeOption;
import com.l7tech.gateway.common.siteminder.SiteMinderHost;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.FileUtils;
import com.l7tech.util.Pair;
import netegrity.siteminder.javaagent.UserCredentials;
import netegrity.siteminder.javaagent.Attribute;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/20/13
 */
public abstract class SiteMinderUtil {
    private static final Logger logger = Logger.getLogger(SiteMinderUtil.class.getName());
    private static final char[] HEXADECIMAL_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final String DEFAULT_SDK_PATH = "/opt/CA/sdk/bin64";
    private static final String DEFAULT_SMREGHOST_PROGRAM = "smreghost";
    private static final String SYSPROP_SMREGHOST_PROGRAM = "com.l7tech.server.smreghost.program";
    private static final String DEFAULT_SHAREDSECRETCONVERT_PROGRAM = "smsharedsecretconvert";
    private static final String SYSPROP_SDK_PATH = "com.l7tech.server.smreghost.path";
    private static final String SYSPROP_SHAREDSECRETCONVERT_PROGRAM = "com.l7tech.server.sharedsecretconvert.program";
    private static final String TEMP_DIR = "SMHOST";

    private SiteMinderUtil() {}

    /**
     * Extract the most specific value from an LDAPv3 name.
     * <p/>
     * <p>This will fail if the most specific name component is multi-valued.</p>
     * <p/>
     * <p>OIDs and HEX values are not supported (will cause ParseException).</p>
     * <p/>
     * <ul>
     * <li><code>cn=test\, \09\C4\8702,dc=layer7-tech,dc=com</code> gives <code>test, \t\u010402</code></li>
     * <li><code>cn="test, 02",dc=layer7-tech,dc=com</code> gives <code>test, 02</code></li>
     * <li><code>uid=test02,dc=layer7-tech,dc=com</code> gives <code>test02</code></li>
     * <li><code>uid=test02+cn=Test 02,dc=layer7-tech,dc=com</code> will throw an exception</li>
     * </ul>
     *
     * @param distinguishedName The LDAPv3 DN (must not be null)
     * @return The value
     * @throws java.text.ParseException if the name cannot be parsed
     */
    public static String getMostSpecificAttributeValue(final String distinguishedName) throws ParseException {
        StringBuilder value = new StringBuilder();
        char[] dn = distinguishedName.toCharArray();
        int length = dn.length;
        int position = 0;

        try {
            // eat space
            while (dn[position] == ' ') {
                position++;
            }

            // parse first attribute name
            if (!((dn[position] >= 'a' && dn[position] <= 'z') ||
                    (dn[position] >= 'A' && dn[position] <= 'Z')))
                throw new ParseException("Expected name [a-zA-Z]", position);

            while ((dn[position] >= 'a' && dn[position] <= 'z') ||
                    (dn[position] >= 'A' && dn[position] <= 'Z') ||
                    (dn[position] >= '0' && dn[position] <= '9') ||
                    dn[position] == '-') {
                position++;
            }

            // eat space
            while (dn[position] == ' ') {
                position++;
            }

            // parse =
            if (dn[position] != '=') throw new ParseException("Expected '='.", position);
            position++;

            // eat space
            while (position < length && dn[position] == ' ') {
                position++;
            }

            // parse value
            if (position < length && dn[position] == '"') {
                // process quoted
                while (position < length) {
                    position++;
                    while (dn[position] != '\\' && dn[position] != '"') {
                        value.append(dn[position]);
                        position++;
                    }
                    if (dn[position] == '"') {
                        position++;
                        break;
                    } else if (dn[position + 1] == '"') {
                        position += 2;
                        value.append('"');
                    } else {
                        throw new ParseException("Unexpected escaped character in quoted string '" + dn[position + 1] + "'.", position);
                    }
                }

            } else if (position < length && dn[position] == '#') {
                throw new ParseException("Hexadecimal values not supported.", position);
            } else {
                // process unquoted
                while (position < length) {
                    while (position < length &&
                            dn[position] != ',' &&
                            dn[position] != '=' &&
                            dn[position] != '+' &&
                            dn[position] != '"' &&
                            dn[position] != '\\' &&
                            dn[position] != '<' &&
                            dn[position] != '>' &&
                            dn[position] != ';') {
                        value.append(dn[position]);
                        position++;
                    }
                    if (position == length || dn[position] != '\\') {
                        break;
                    } else if (dn[position + 1] == ',' || dn[position + 1] == '=' || dn[position + 1] == '+' || dn[position + 1] == '"' ||
                            dn[position + 1] == ';' || dn[position + 1] == '<' || dn[position + 1] == '>' || dn[position + 1] == '\\') {
                        value.append(dn[position + 1]);
                        position += 2;
                    } else {
                        // hex pair(s)
                        int count = 0;
                        while (dn[position + (count * 3)] == '\\' &&
                                !(dn[position + 1] == ',' || dn[position + 1] == '=' || dn[position + 1] == '+' || dn[position + 1] == '"' ||
                                        dn[position + 1] == ';' || dn[position + 1] == '<' || dn[position + 1] == '>' || dn[position + 1] == '\\')) {
                            count++;
                        }
                        byte[] utf8Bytes = new byte[count];
                        for (int n = 0; n < count; n++) {
                            utf8Bytes[n] |= Integer.parseInt(new String(dn, position + (n * 3) + 1, 1), 16) << 4;
                            utf8Bytes[n] |= Integer.parseInt(new String(dn, position + (n * 3) + 2, 1), 16);
                        }
                        value.append(new String(utf8Bytes, "UTF-8"));
                        position += (count * 3);
                    }
                }
            }

            // eat space
            while (position < length && dn[position] == ' ') {
                position++;
            }

            // Check for the end of this name (if there's a '+' this is a multivalued attribute and we can't use it)
            if (position < length && dn[position] != ',' && dn[position] != ';')
                throw new ParseException("Expected ',' or ';'", position);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new ParseException("Unexpected end of name.", position);
        } catch (NumberFormatException nfe) {
            throw new ParseException("Invalid Hexadecimal value '" + nfe.toString() + "'.", position);
        } catch (UnsupportedEncodingException uee) {
            throw new ParseException("Could not decode UTF-8 text.", position);
        }

        return value.toString();
    }

    public static String hexDump(byte[] binaryData, int off, int len) {
        if (binaryData == null) throw new NullPointerException();
        if (off < 0 || len < 0 || off + len > binaryData.length) throw new IllegalArgumentException();

        char[] buffer = new char[len * 2];

        for (int i = 0; i < len; i++) {
            int low = (binaryData[off + i] & 0x0f);
            int high = ((binaryData[off + i] & 0xf0) >> 4);
            buffer[i*2] = HEXADECIMAL_DIGITS[high];
            buffer[i*2 + 1] = HEXADECIMAL_DIGITS[low];
        }

        return new String(buffer);
    }

    public static String safeNull(String s) {
        return s == null ? "" : s;
    }

    private static Pattern NULL_CHAR = Pattern.compile("\\u0000$");

    /**
     * converts SiteMinder Attribute value to int
     */
    public static int convertAttributeValueToInt(Attribute attribute) {
        int attrVal = -1;//default
        if(attribute == null || attribute.value == null) return -1;

        String sVal = new String(attribute.value);//convert to string
        sVal = chopNull(sVal);
        try{
            attrVal = Integer.parseInt(sVal);
        } catch (NumberFormatException nfe) {
            logger.log(Level.FINE, "Invalid attribute value: " + sVal);
        }
        return attrVal;
    }

    public static int safeByteArrToInt( byte[] bytes ) {
        int number = 0;
        try {
            number = ByteBuffer.wrap(bytes).getInt();
        } catch (IllegalArgumentException iae ) {
            logger.log( Level.FINE, "Unable to convert byte[] into int value: " + new String( bytes ) );
        } catch ( BufferUnderflowException bufe ){
            logger.log( Level.FINE, "BufferOverFlow encountered when converting byte[] into int value: " + new String( bytes ) );

        }
        return number;
    }

    public static String chopNull(String sVal) {
        Matcher m = NULL_CHAR.matcher(sVal);
        if(m.find()){
            //chop NULL value
            sVal = m.replaceAll("");
        }
        return sVal;
    }

    /**
     * If a certificate is present in the request, add it to the user credentials.
     *
     * @throws java.security.cert.CertificateEncodingException
     */
    public static boolean handleCertificate(X509Certificate cert, UserCredentials userCreds) throws CertificateEncodingException {
        if (cert != null && userCreds != null) {
            userCreds.certBinary = cert.getEncoded();
            return true;
        }
        return false;
    }

    /**
     * Register and retrieve SiteMinder host configuration.
     *
     * @param address Policy Server Address
     * @param username Username to login to PolicyServer
     * @param password password to login to PolicyServer
     * @param hostname register hostname
     * @param hostconfig host configuration
     * @param fipsMode fibs mode
     * @return the registered SiteMinder host configuration
     * @throws IOException
     */
    public static SiteMinderHost regHost(String address,
                                         String username,
                                         String password,
                                         String hostname,
                                         String hostconfig,
                                         SiteMinderFipsModeOption fipsMode) throws IOException {

        File program;
        File tmpDir = null;

        try {
            tmpDir = FileUtils.createTempDirectory(TEMP_DIR, null, null, false);
            String smHostConfig = tmpDir.getAbsolutePath() + File.separator + "smHost.conf";

            logger.log(Level.FINE, "registering CA Single Sign-On agent...");

            String sdkPath = ConfigFactory.getProperty(SYSPROP_SDK_PATH, DEFAULT_SDK_PATH);

            String smreghost = ConfigFactory.getProperty(SYSPROP_SMREGHOST_PROGRAM, DEFAULT_SMREGHOST_PROGRAM);

            String[] params = {
                    "-i", address,
                    "-u", username,
                    "-p", password,
                    "-hn", hostname,
                    "-hc", hostconfig,
                    "-f", smHostConfig,
                    "-cf", fipsMode.getName(),
                    "-o"
            };

            execSmRegProgram(sdkPath + File.separator + smreghost, params);

            ////////////////////////////////////////////////////////////////////////////////////////////
            // Invoke shared secret convertion utility

            logger.log(Level.FINE, "converting shared secret...");

            String sharedConvertUtil = ConfigFactory.getProperty(SYSPROP_SHAREDSECRETCONVERT_PROGRAM, DEFAULT_SHAREDSECRETCONVERT_PROGRAM);

            execSmRegProgram(sdkPath + File.separator + sharedConvertUtil, new String[]{"-f", smHostConfig});

            ///////////////////////////////////////////////////////////////////////////////////////////

            return new SiteMinderHost(smHostConfig);

        } finally {
            if (tmpDir != null && tmpDir.exists()) {
                FileUtils.deleteDir(tmpDir);
            }
        }
    }

    private static ProcResult execSmRegProgram(String programPath, String[] params) throws  IOException {
        if (programPath == null || programPath.length() < 1)
            return null;
        File program = new File(programPath);
        if(!program.exists() || !program.canExecute()) {
            throw new IOException("Unable to find " + program);
        }

        logger.log(Level.FINE, "executing program: " + program);

        return ProcUtils.exec(program, params, null, false);
    }


    /**
     * Converts SiteMinder UserCredentials to String format
     * this is a convenience method to complement a lack of proper toString method in UserCredentials class from SM Agent API SDK
     * @param creds  UserCredentials
     * @return  String representation of UserCredentials object
     */
    public static String getCredentialsAsString(final UserCredentials creds) {
        if(creds == null) return null; //there is no point to continue

        String s = null;
        if(creds.name != null && !creds.name.isEmpty()) {
            s = creds.name;
        }
        else if (creds.certBinary != null && creds.certBinary.length > 0){
            s = "<binary cert>";
        }
        else if (creds.certIssuerDN != null && !creds.certIssuerDN.isEmpty()){
            s = creds.certIssuerDN;
        }
        else if (creds.certUserDN != null && !creds.certUserDN.isEmpty()){
            s = creds.certUserDN;
        }
        return s;
    }

    /**
     * Converts SiteMinderCredentials to String format
     * @param credentials  UserCredentials
     * @return  String representation of UserCredentials object
     */
    public static String getCredentialsAsString(final SiteMinderCredentials credentials) {
       return getCredentialsAsString(credentials.getUserCredentials());
    }

    /**
     * returns null if not found or the value as an Object
     * The caller will have to handle any required data type conversion
     */
    public static Object getAttribute(List<Pair<String, Object>> attributes, String id) {
        for(Pair<String, Object> attr: attributes) {
            if( attr.left.equals(id) ){
                return attr.getValue();
            }
        }
        return  null;
    }

    public static List<SiteMinderContext.Attribute> removeDuplicateAttributes ( List<SiteMinderContext.Attribute> attrList ) {
        Set<SiteMinderContext.Attribute> attributes = new HashSet<>();
        attributes.addAll(attrList);
        attrList.clear();
        attrList.addAll(attributes);
        return attrList;
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    public static byte[] safeIntToByteArr( int num ) {
        byte[] bytes = new byte[]{};
        try {
            bytes = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).putInt( num ).array();
        } catch (IllegalArgumentException iae ) {
            logger.log( Level.FINE, "Unable to allocate buffer for int value: " + num );
        } catch ( BufferOverflowException bof ){
            logger.log( Level.FINE, "BufferOverFlow encountered when converting int value: " + num + "byte[]" );

        }
        return bytes;
    }

    public static byte[] getAttrValueByName( List<SiteMinderContext.Attribute> attrList, String name ) {
        Iterator<SiteMinderContext.Attribute> values = attrList.iterator();
        while( values.hasNext() ){
            SiteMinderContext.Attribute attr = values.next();
            if( name.equals( attr.getName() )) {
                return ( (String) attr.getValue() ).getBytes();
            }
        }
        return null;
    }
}
