/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov.luna;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.IOUtils;
import com.l7tech.util.HexUtils;

import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java-based interface to the Luna Certificate Management Utility.  Multiple threads may safely share a single
 * LunaCmu instance without any additional synchronization.
 */
public class LunaCmu {
    private static final Logger logger = Logger.getLogger(LunaCmu.class.getName());

    /** The system property that holds the path to the Luna cmu exectuable. */
    public static final String PROPERTY_CMU_PATH = "lunaCmuPath";
    private static final String DEFAULT_CMU_PATH_WINDOWS = "C:/Program Files/LunaSA/cmu.exe";
    private static final String DEFAULT_CMU_PATH_UNIX = "/usr/lunasa/bin/cmu";
    private static final Random random = new Random();

    private final String cmuPath;
    private final File cmuFile;
    private final boolean isWindows;

    /**
     * Create a LunaCmu instance ready to perform cmu operations.  This requires the cmu binary *and* a connection
     * via the Luna API.
     *
     * @see #PROPERTY_CMU_PATH
     * @throws LunaCmuException if the Luna cmu utility was not found (see system property: {@link #PROPERTY_CMU_PATH})
     * @throws LunaTokenNotLoggedOnException if the Luna token manager is not currently logged into a partition
     * @throws ClassNotFoundException if the Luna classes are not in the current classpath
     * @throws ClassNotFoundException if the Luna class version is not compatible with this code
     */
    public LunaCmu() throws LunaCmuException, ClassNotFoundException, LunaTokenNotLoggedOnException {
        final String osname = System.getProperty("os.name");
        isWindows = (osname != null && osname.indexOf("Windows") >= 0);
        final String defaultCmuPath =  isWindows ? DEFAULT_CMU_PATH_WINDOWS : DEFAULT_CMU_PATH_UNIX;
        cmuPath = System.getProperty(PROPERTY_CMU_PATH, defaultCmuPath);
        if (cmuPath == null || cmuPath.length() < 1)
            throw new LunaCmuException("Unable to find Luna Certificate Management Utility (cmu): System property " + PROPERTY_CMU_PATH + " is not valid");
        cmuFile = new File(cmuPath);
        if (!cmuFile.exists() || !cmuFile.isFile())
            throw new LunaCmuException("Unable to find Luna Certificate Management Utility (cmu) at path: "
                                               + cmuPath + ".  Set system property " + PROPERTY_CMU_PATH + " to override.");

        probe();

        logger.finer("Found Luna cmu utility at path: " + cmuPath);
    }

    /**
     * Assert that this LunaCmu instance is connected to a cmu binary and ready to perform cmu functions.
     * If this method returns, the Luna cmu binary was invoked and its output was recognizable.
     * <p>
     * It should not normally be necessary to call this directly since it is called within the LunaCmu constructor.
     *
     * @see #PROPERTY_CMU_PATH
     * @throws LunaCmuException if the LunaCmu utility was not probed successfully.
     * @throws LunaTokenNotLoggedOnException if the Luna token manager is not currently logged into a partition
     * @throws ClassNotFoundException if the Luna classes are not in the current classpath
     * @throws ClassNotFoundException if the Luna class version is not compatible with this code
     */
    public void probe() throws LunaCmuException, ClassNotFoundException, LunaTokenNotLoggedOnException {
        // Ping the CMU with a help request
        String got = new String(exec(new String[] { "?" }, null));
        final String wanted = "Certificate Management Utility";
        if (got.indexOf(wanted) < 0)
            throw new LunaCmuException("Unrecognized output from Luna Certificate Management Utility (cmu): possible unsupported version?  Invocation of cmu -? failed to produce the string: " + wanted);
        if (!LunaProber.isPartitionLoggedIn())
            throw new LunaTokenNotLoggedOnException();
    }

    /**
     * Get all the objects available via this cmu instance.
     *
     * @return an array of {@link CmuObject} instances.  Will never be null, but may be empty if there are no objects
     *         in the current partition.
     * @throws LunaCmuException if the list could not be obtained.
     */
    public CmuObject[] list() throws LunaCmuException {
        byte[] got = exec(new String[] { "list", "-display", "index,handle,class,keyType,label,value" }, null);

        Pattern lineParser = Pattern.compile("^\\s*(\\d+)\thandle=(\\d+)\tclass=(\\S+?)\tkeytype=(\\S+?)\tlabel=(.*?)\tvalue=(.*?)\\s*$");

        List found = new ArrayList();
        BufferedReader br = new BufferedReader(new StringReader(new String(got)));
        try {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                final Matcher matcher = lineParser.matcher(line);
                if (matcher.matches()) {
                    long index = Long.parseLong(matcher.group(1));
                    long handle = Long.parseLong(matcher.group(2));
                    String type = matcher.group(3);
                    String keyType = matcher.group(4);
                    String label = matcher.group(5);
                    String value = matcher.group(6);
                    found.add(new CmuObject(index, handle, type, keyType, label, value));
                } else if (line.trim().length() > 0) // ignore blank lines, choke on anything else unparseable
                    throw new LunaCmuException("Unparsable line from Luna Certificate Manage Utility (cmu) list command: " + line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen at this point
        } catch (NumberFormatException e) {
            throw new LunaCmuException("Invalid output from Luna Certificate Management Utility (cmd): " + e.getMessage(), e);
        }

        return (CmuObject[])found.toArray(new CmuObject[0]);
    }

    /**
     * Delete the specified object from this partition.  If the object in question was already deleted,
     * this method returns without taking action.
     *
     * @param obj the object to delete, perhaps obtained from {@link #list}.  May not be null.
     * @throws LunaCmuException if there was a problem invoking the cmu utility
     */
    public void delete(CmuObject obj) throws LunaCmuException {
        byte[] out = exec(new String[] { "delete", "-handle=" + obj.getHandle(), "-force" }, null);

        String mess = new String(out);

        if (mess.equals("Object not found"))
            return; // this is fine

        if (mess.trim().length() > 0)
            throw new LunaCmuException("Luna Certificate Management Utility failed to delete object handle " + obj.getHandle() + ": " + mess);
    }

    /**
     * Generates a new 1024-bit RSA key pair which allows signing and verification.  The private key will be
     * returned.  The public key will be deleted.  (The private key the public key's info.)
     *
     * @param label the label to use for the private key.  May not be null or empty.
     *              The public key will be assigned the same label with "--publicKey" appended.
     * @return the CmuObject for the private key.  Will never be null.
     */
    public CmuObject generateRsaKeyPair(String label) throws LunaCmuException {
        String labelPublic = label + "--publicKey";

        byte[] out = exec(new String[] { "gen",
                                         "-modulusBits=1024",
                                         "-publicExp=3",
                                         "-encrypt=T",
                                         "-decrypt=T",
                                         "-sign=T",
                                         "-verify=T",
                                         "-wrap=T",
                                         "-unwrap=T",
                                         "-id=" + makeShortUuid(),
                                         "-labelPublic=" + labelPublic,
                                         "-labelPrivate=" + label },
                          null);
        checkForErrorMessage(out, "create RSA key pair labeled " + label);

        final Integer privateKeyHandle;
        try {
            privateKeyHandle = LunaProber.locateKeyHandleByAlias(label);
            if (privateKeyHandle == null)
                throw new LunaCmuException("Unable to locate the newly generated private key with alias: " + label);

            LunaProber.destroyKeyByAlias(labelPublic);

        } catch (ClassNotFoundException e) {
            throw new LunaCmuException("Unable to locate the newly generated private key: " + e.getMessage(), e);
        } catch (LunaProber.KeyNotFoundException e) {
            throw new LunaCmuException("Unable to locate the newly generated public key: " + e.getMessage(), e);
        }

        return new CmuObject(0, privateKeyHandle.intValue(), "privateKey", "RSA", label, "n/a");
    }


    /**
     * Create a new CA cert with a DN constructed from the specified components and using the specified RSA private key object.
     * The private key object must allow signing and verification.
     * <p>
     * Note that at least one DN component
     * must be specified, and that if you are only going to use one, CN is the one to use.
     * <p>
     * The certificate will be valid from yesterday to the date 20 years from today.  It will be configured
     * with a key usage allowing certificate to be signed.
     * <p>
     * The CA cert will always have the serial number 0x1001.
     * <p>
     * Note that the Luna KeyStore implementation requires that a certificate for a key entry use the same label as its
     * private key, but with "--cert0" appended.
     *
     * @param privateKey the CmuObject handle for the private key to use.  Must be an RSA private key that allow sign and verify.
     * @param label  the label to use.  For the cert to be visible to the Luna KeyStore implementation, it must be named
     *               keylabel--cert0 where "keylabel" is the label of the RSA private key object.  Leave this parameter null
     *               to default to keylabel--cert0.
     * @param CN the common name (CN) portion of the DN, or null to omit.  This should be specified.
     * @return a CmuObject handle to the newly created certificate.  Never null.
     * @throws LunaCmuException if the certificate could not be created
     */
    public CmuObject generateCaCert(CmuObject privateKey, String label, String CN) throws LunaCmuException {
        return generateLocalCert(privateKey, "keycertsign", 20, null, null, null, null, null, CN, label);
    }

    /**
     * Create a new CA cert with a DN constructed from the specified components and using the specified RSA private key object.
     * The private key object must allow signing and verification.
     * <p>
     * Note that at least one DN component
     * must be specified, and that if you are only going to use one, CN is the one to use.
     * <p>
     * The certificate will be valid from yesterday to the date 20 years from today.  It will be configured
     * with a key usage allowing certificate to be signed.
     * <p>
     * The CA cert will always have the serial number 0x1001.
     * <p>
     * Note that the Luna KeyStore implementation requires that a certificate for a key entry use the same label as its
     * private key, but with "--cert0" appended.
     *
     * @param privateKey the CmuObject handle for the private key to use.  Must be an RSA private key that allow sign and verify.
     * @param label  the label to use.  For the cert to be visible to the Luna KeyStore implementation, it must be named
     *               keylabel--cert0 where "keylabel" is the label of the RSA private key object.  Leave this parameter null
     *               to default to keylabel--cert0.
     * @param CN the common name (CN) portion of the DN, or null to omit.  This should be specified.
     * @return a CmuObject handle to the newly created certificate.  Never null.
     * @throws LunaCmuException if the certificate could not be created
     */
    public CmuObject generateSslCert(CmuObject privateKey, String label, String CN) throws LunaCmuException {
        return generateLocalCert(privateKey, "digitalsignature,nonrepudiation,keyencipherment", 5, null, null, null, null, null, CN, label);
    }

    private CmuObject generateLocalCert(CmuObject privateKey, final String keyusage, final int expiryYears, String C, String S, String L, String O, String OU, String CN, String label) throws LunaCmuException {
        List args = new ArrayList();
        args.add("selfSignCertificate");
        args.add("-privatehandle=" + privateKey.getHandle());

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.add(Calendar.DATE, -1);
        String now = format(cal);
        cal.add(Calendar.DATE, 1);
        cal.add(Calendar.YEAR, expiryYears);
        String then = format(cal);

        args.add("-startDate=" + now);
        args.add("-endDate=" + then);
        args.add("-serialNumber=1001");
        args.add("-keyusage=" + keyusage);

        List dnArgs = makeDnArgs(C, S, L, O, OU, CN);

        args.addAll(dnArgs);

        if (label == null) label = privateKey.getLabel() + "--cert0";
        args.add("-id=" + makeShortUuid());
        args.add("-label=" + label);

        CmuObject[] objBefore = list();
        byte[] out = exec((String[])args.toArray(new String[0]), null);
        checkForErrorMessage(out, "create self-signed CA certificate");

        // Find all new objects
        CmuObject[] objAfter = list();
        Set setAfter = new HashSet(Arrays.asList(objAfter));
        setAfter.removeAll(new HashSet(Arrays.asList(objBefore)));

        // Find new objects of type certificate that match our label
        CmuObject found = null;
        for (Iterator i = setAfter.iterator(); i.hasNext();) {
            CmuObject obj = (CmuObject)i.next();
            if (obj.getType().equals("certificate") && obj.getLabel().equals(label)) {
                if (found != null)
                    throw new LunaCmuException("Luna Certificate Management Utility found multiple new certificates with the label " + label);
                found = obj;
            }
        }

        if (found == null)
            throw new LunaCmuException("Luna Certificate Management Utility did not find the newly generated certificate with label " + label);

        return found;
    }

    private List makeDnArgs(String C, String S, String L, String O, String OU, String CN) throws LunaCmuException {
        List dnArgs = new ArrayList();
        if (C != null) dnArgs.add("-C=" + quoteMaybe(C));
        if (S != null) dnArgs.add("-S=" + quoteMaybe(S));
        if (L != null) dnArgs.add("-L=" + quoteMaybe(L));
        if (O != null) dnArgs.add("-O=" + quoteMaybe(O));
        if (OU != null) dnArgs.add("-OU=" + quoteMaybe(OU));
        if (CN != null) dnArgs.add("-CN=" + quoteMaybe(CN));
        if (dnArgs.size() < 1) throw new LunaCmuException("To create a new certificate, at least one DN component must be specified.");
        return dnArgs;
    }

    /**
     * Create a new Certificate Signing Request using an already-created private key object.
     * 
     * @param privateKey a handle to the private key to use for the new certificate request.  Must have Signing capability.
     * @param CN the common name (CN) portion of the DN, or null to omit.  This should be specified.
     * @return the bytes of the new Certificate Signing Request.  Never null.
     * @throws LunaCmuException if there was a problem invoking cmu to perform the operation.
     */
    public byte[] requestCertificate(final CmuObject privateKey,
                                     String CN)
            throws LunaCmuException
    {
        File csrFile = null;
        FileInputStream fis = null;
        try {
            csrFile = File.createTempFile("lcsr", makeShortUuid() + ".tmp");
            csrFile.deleteOnExit();

            List args = new ArrayList();
            args.add("requestCertificate");
            args.add("-privatehandle=" + privateKey.getHandle());
            args.add("-outputfile=" + quoteMaybe(csrFile.getPath()));
            args.add("-binary");
            args.addAll(makeDnArgs(null, null, null, null, null, CN));
            byte[] out = exec((String[])args.toArray(new String[0]), null);
            checkForErrorMessage(out, "create certificate signing request");

            fis = new FileInputStream(csrFile);
            return IOUtils.slurpStream(fis);

        } catch (IOException e) {
            throw new LunaCmuException("Unable to save CSR to temporary file: " + e.getMessage(), e);
        } finally {
            if (fis != null) try { fis.close(); } catch (IOException e) {}
            if (csrFile != null) csrFile.delete();
        }
    }

    private String quoteMaybe(String path) {
        return isWindows ? ("\"" + path + "\"") : path;
    }

    private static String format(Calendar cal) {
        StringBuffer sb = new StringBuffer();
        final int year = cal.get(Calendar.YEAR);
        if (year < 1000) sb.append("0"); // not likely, but it bugged me to leave this unhandled
        if (year < 100) sb.append("0");
        if (year < 10) sb.append("0");
        sb.append(year);

        final int month = cal.get(Calendar.MONTH) + 1;
        if (month < 10) sb.append("0");
        sb.append(month);

        int day = cal.get(Calendar.DAY_OF_MONTH);
        if (day < 10) sb.append("0");
        sb.append(day);

        return sb.toString();
    }

    /**
     * Run the Luna cmu command with the specified arguments, optionally piping the specified byte array
     * into the cmu utility's standard input.
     *
     * @param args the argument array.  May be empty but not null.  Note that the cmu utility itself will return
     *        an error message if no arguments are provided.
     * @param stdin  a byte array to pass into the cmu utility's stdin.  May be null or empty to suppress this.
     * @return  the bytes that the cmu utility wrote to its stdout before exiting.
     * @throws LunaCmuException if there is an IOException while invoking the Luna cmu utility
     */
    private byte[] exec(String[] args, byte[] stdin) throws LunaCmuException {
        try {
            return doExec(args, stdin);
        } catch (IOException e) {
            throw new LunaCmuException("Unable to invoke Luna Certificate Management Utility (cmu): " + e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new LunaCmuException("Unable to invoke Luna Certificate Management Utility (cmu): " + e.getMessage(), e);
        }
    }

    private byte[] doExec(String[] args, byte[] stdin) throws IOException, InterruptedException, LunaCmuException {
        String[] cmdArray = new String[args.length + 1];
        cmdArray[0] = cmuFile.getPath();
        for (int i = 0; i < args.length; i++)
            cmdArray[i + 1] = args[i];

        logger.finest("Running Luna cmu utility");
        Process proc = Runtime.getRuntime().exec(cmdArray);

        final OutputStream os = proc.getOutputStream();
        if (stdin != null) {
            logger.finest("Sending " + stdin.length + " bytes of input into Luna cmu utility");
            os.write(stdin);
            os.flush();
        }
        os.close();

        logger.finest("Reading output from Luna cmu utilitiy");
        byte[] slurped = IOUtils.slurpStream(proc.getInputStream());
        logger.finest("Read " + slurped.length + " bytes of output from Luna cmu utilitiy");

        int status = proc.waitFor();
        logger.finest("Luna cmu utility exited status code " + status);
        if (status != 0)
            throw new LunaCmuException("Luna cmu utility exited with status " + status + ".  Output: " + new String(slurped));

        return slurped;
    }

    /**
     * Finds the first Certificate object in the HSM that has matches the specified label or
     * "label--cert0".
     *
     * @param label  the label to match.  Must not be null or empty.
     * @return  a handle to the certificate object in question.
     */
    public CmuObject findCertificateByHandle(String label) throws LunaCmuException {
        CmuObject[] objs = list();
        for (int i = 0; i < objs.length; i++) {
            CmuObject obj = objs[i];
            if ("certificate".equals(obj.getType()) && label.equals(obj.getLabel()) || (label + "--cert0").equals(obj.getLabel())) {
                return obj;
            }
        }
        throw new LunaCmuException("Unable to find a certificate object matching the label " + label + " or " + label + "--cert0");
    }

    /**
     * Create a signed client certificate from the CSR, using the specified CA cert object to do the signing.
     * The newly signed certificate will have the key usage set to allow digitalSignature, keyEncipherment, and
     * nonRepudiation.
     *
     * @param csr        the CSR to process.  Must be a valid PKCS#10 certification request.  It is assumed that
     *                   either the caller has already validated the content of the CSR, or that the caller intends
     *                   to validate the resulting certificate instead before deciding whether to give it out or
     *                   destroy it.
     * @param caCert     a handle to the CA certificate object in the CMU.  This certificate object must have the
     *                   keyCertSign key usage capability.  Must not be null.
     * @param daysValid  number of days in the future to set the new cert's expiry date.  Must be nonnegative.
     * @param serialNum  the serial number to use for the cert.
     * @param label      label to use for the cert, if it is to be left installed inside the HSM, or null to
     *                   REFRAIN from leaving the new cert stored in the HSM.  This should usually be set to null
     *                   to avoid filling up the (very low) partition object limit with signed client certs.
     *                   Only specify a label if you really do want to save the newly signed cert inside the HSM.
     *                   <p/>
     *                   One possible reason to save the cert is if its private key itself lives inside the HSM.
     *                   In this case the label should be set to "keylabel--cert0" for the Luna KeyStore implementation
     *                   to be able to find it.
     * @return the newly signed X509Certificate object.  Never null.  Caller should validate the content of the cert
     *         before deciding whether to hand it out or destroy it.
     * @throws LunaCmuException  if there was a problem getting the Luna CMU to do it's thing.
     */
    public X509Certificate certify(byte[] csr, CmuObject caCert, int daysValid, long serialNum, String label) throws LunaCmuException {
        if (daysValid < 0) throw new IllegalArgumentException("Days valid must be nonnegative");
        File csrFile = null;
        FileOutputStream fos = null;
        CmuObject newCertObject = null;
        boolean deleteAfterExport = label == null;
        try {
            csrFile = File.createTempFile("lcsr", makeShortUuid() + ".tmp");
            csrFile.deleteOnExit();
            fos = new FileOutputStream(csrFile);
            fos.write(csr);
            fos.flush();
            fos.close();
            fos = null;

            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            cal.add(Calendar.DATE, -1);
            String now = format(cal);
            cal.add(Calendar.DATE, 1);
            cal.add(Calendar.DATE, daysValid);
            String then = format(cal);

            // Create unique label for locating the new cert afterwards
            if (label == null || label.length() < 1)
                label = "CLCRT" + makeUuid();

            CmuObject[] before = list();
            final String[] args = new String[] { "certify",
                                                         "-input=" + quoteMaybe(csrFile.getPath()),
                                                         "-handle=" + caCert.getHandle(),
                                                         "-startDate=" + now,
                                                         "-endDate=" + then,
                                                         "-serialNumber=" + Long.toHexString(serialNum),
                                                         "-id=" + makeShortUuid(),
                                                         "-label=" + label,
                                                         "-keyusage=digitalsignature,nonrepudiation,keyencipherment"
                        };
            byte[] out = exec(args, null);
            checkForErrorMessage(out, "certify CSR");

            // Find the new CmuObjects
            Set setAfter = new HashSet(Arrays.asList(list()));
            setAfter.removeAll(Arrays.asList(before));
            for (Iterator i = setAfter.iterator(); i.hasNext();) {
                CmuObject obj = (CmuObject)i.next();
                if ("certificate".equals(obj.getType()) && label.equals(obj.getLabel())) {
                    newCertObject = obj;
                    break; // won't bother checking for duplicates here since it's so unlikely and there'd be nothing useful we could do anyway other than pick one to delete
                }
            }
            if (newCertObject == null)
                throw new LunaCmuException("Luna Certificate Management Utility was unable to certify CSR: could not find the newly created certificate inside the HSM with label: " + label);

            return exportCertificate(newCertObject);
        } catch (IOException e) {
            throw new LunaCmuException("Unable to record CSR to temporary file: " + e.getMessage(), e);
        } finally {
            if (fos != null) try { fos.close(); } catch (IOException e) {}
            if (csrFile != null) csrFile.delete();
            if (deleteAfterExport && newCertObject != null) try { delete(newCertObject); } catch (LunaCmuException e) {}
        }
    }

    private String makeShortUuid() {
        return makeUuid().substring(0, 16);
    }

    /**
     * Export the specified cert object into a Java X509Certificate instance.
     *
     * @param certObj  the handle to the cert object to export.  Must be a valid certificate object.
     * @return  the java X509Certificate object.
     */
    public X509Certificate exportCertificate(CmuObject certObj) throws LunaCmuException {
        File exportFile = null;
        FileInputStream fis = null;
        try {
            byte[] out;
            // See if we can decode the cert from existing information
            String value = certObj.getValue();
            if (value != null && value.length() > 0 && !"n/a".equals(value)) {
                try {
                    return CertUtils.decodeCert(HexUtils.unHexDump(value));
                } catch (IOException e) {
                    // Bad hex code. Fall through and do the whole export process
                } catch (CertificateException e) {
                    // Bad DER. Fall through and do the whole export process
                }
            }

            exportFile = File.createTempFile("lcrt", makeShortUuid() + ".tmp");
            exportFile.deleteOnExit();

            out = exec(new String[]{"export",
                                    "-handle=" + certObj.getHandle(),
                                    "-binary",
                                    "-outputFile=" + quoteMaybe(exportFile.getPath())
                                   }, null);
            checkForErrorMessage(out, "export newly signed certificate");

            fis = new FileInputStream(exportFile);
            return CertUtils.decodeCert(IOUtils.slurpStream(fis));
        } catch (IOException e) {
            throw new LunaCmuException("Unable to export certificate to temporary file: " + e.getMessage(), e);
        } catch (CertificateException e) {
            throw new LunaCmuException("Unable to decode exported newly signed certificate: " + e.getMessage(), e);
        } finally {
            if (fis != null) try { fis.close(); } catch (IOException e) {}
            if (exportFile != null) exportFile.delete();
        }
    }

    /** @return a new 16 byte (32 character) random hex string */
    private String makeUuid() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexUtils.hexDump(bytes);
    }

    private void checkForErrorMessage(byte[] out, String operation) throws LunaCmuException {
        String mess = new String(out);
        if (mess.trim().length() > 0)
            throw new LunaCmuException("Luna Certificate Management Utility failed to " + operation + ": " + mess);
    }

    public class CmuObject {
        private final long index;
        private final long handle;
        private final String type;
        private final String keyType;
        private final String label;
        private final String value;

        public CmuObject(long index, long handle, String type, String keyType, String label, String value) {
            this.index = index;
            this.handle = handle;
            this.type = type;
            this.keyType = keyType;
            this.label = label;
            this.value = value;
        }

        public long getIndex() {
            return index;
        }

        public long getHandle() {
            return handle;
        }

        /** @return the class of this CMU object.  Note: not the Java class, but the type of the object in the HSM. */
        public String getType() {
            return type;
        }

        public String getKeyType() {
            return keyType;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CmuObject)) return false;

            final CmuObject cmuObject = (CmuObject)o;

            if (handle != cmuObject.handle) return false;

            return true;
        }

        public int hashCode() {
            return (int)(handle ^ (handle >>> 32));
        }

        public String toString() {
            return "(CmuObject: " + this.getHandle() + "," + this.getType() + "," + this.getLabel() + ")";
        }
    }

    public static class LunaCmuException extends Exception {
        public LunaCmuException() { super(); }
        public LunaCmuException(String message) { super(message); }
        public LunaCmuException(String message, Throwable cause) { super(message, cause); }
        public LunaCmuException(Throwable cause) { super(cause); }
    }

    public static class LunaTokenNotLoggedOnException extends Exception {
        private static final String DEFAULT_MESSAGE = "Luna partition is not logged in -- please log in manually";
        public LunaTokenNotLoggedOnException() { super(DEFAULT_MESSAGE); }
        public LunaTokenNotLoggedOnException(String message) { super(message == null ? DEFAULT_MESSAGE : message); }
        public LunaTokenNotLoggedOnException(String message, Throwable cause) { super(message == null ? DEFAULT_MESSAGE : message, cause); }
        public LunaTokenNotLoggedOnException(Throwable cause) { super(cause); }
    }
}
