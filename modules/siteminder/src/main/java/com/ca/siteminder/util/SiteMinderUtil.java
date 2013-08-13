package com.ca.siteminder.util;

import com.l7tech.common.io.ProcUtils;
import com.l7tech.gateway.common.siteminder.SiteMinderHost;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.FileUtils;
import netegrity.siteminder.javaagent.UserCredentials;
import netegrity.siteminder.javaagent.Attribute;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
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
    private static final String DEFAULT_SMREGHOST_PROGRAM = "/opt/CA/sdk/bin64/smreghost";
    private static final String SYSPROP_SMREGHOST_PROGRAM = "com.l7tech.server.smreghost.program";

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
        StringBuffer value = new StringBuffer();
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
     * @param attribute
     * @return
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
     * @param cert
     * @param userCreds
     * @throws java.security.cert.CertificateEncodingException
     * @throws CertificateEncodingException
     */
    public static boolean handleCertificate(X509Certificate cert, UserCredentials userCreds) throws CertificateEncodingException {
        boolean success = false;
        if (cert != null) {
            sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
            String base64Cert = null;
            base64Cert = encoder.encode(cert.getEncoded());
            userCreds.certBinary = base64Cert.getBytes();
            userCreds.certIssuerDN = cert.getIssuerDN().toString();
            userCreds.certUserDN = cert.getSubjectDN().toString();
            success = true;
        }
        return success;
    }

    /**
     * Register and retrieve siteminder host configuration.
     *
     * @param address Policy Server Address
     * @param username Username to login to PolicyServer
     * @param password password to login to PolicyServer
     * @param hostname register hostname
     * @param hostconfig host configuration
     * @param fipsMode fibs mode
     * @return
     * @throws IOException
     */
    public static SiteMinderHost regHost(String address,
                                         String username,
                                         String password,
                                         String hostname,
                                         String hostconfig,
                                         Integer fipsMode) throws IOException {

        File tmpDir = null;
        File program = null;
        try {
            tmpDir = FileUtils.createTempDirectory("SMHOST", null, null, false);
            String smHostConfig = tmpDir.getAbsolutePath() + File.separator + "smHost.conf";

            program = getSmRegHost();
            if (program == null)
                throw new IOException("Unable to find smreghost");

            logger.log(Level.FINE, "registering SiteMinder agent with program: " + program);

            ProcUtils.exec(null,program, new String[]{ "-i", address, "-u", username, "-p", password, "-hn", hostname, "-hc",  hostconfig, "-f", smHostConfig, "-cf", getFipsMode(fipsMode), "-o"}, (byte[]) null, false);

            return new SiteMinderHost(smHostConfig);

        } finally {
            if (tmpDir != null && tmpDir.exists()) {
                FileUtils.deleteDir(tmpDir);
            }
        }
    }


    /** @return the program to be run whenever the firewall rules change, or null to take no such action. */
    private static File getSmRegHost() {
        File defaultProgram = new File(DEFAULT_SMREGHOST_PROGRAM);
        String program = ConfigFactory.getProperty(SYSPROP_SMREGHOST_PROGRAM, defaultProgram.getAbsolutePath());
        if (program == null || program.length() < 1)
            return null;
        File file = new File(program);
        return file.exists() && file.canExecute() ? file : null;
    }

    public static String getFipsMode(int fipsMode) {
        switch(fipsMode) {
            case 0:
                return "UNSET";
            case 1:
                return "COMPAT";
            case 2:
                return "MIGRATE";
            case 3:
                return "ONLY";
        }
        return null;
    }
}
