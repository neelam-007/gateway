/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.ibm.xml.dsig.*;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ISO8601Date;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.HibernatePersistenceManager;
import com.l7tech.objectmodel.PersistenceContext;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.sql.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Simple command line utility to export signed audit records.
 */
public class AuditExporter {
    private static final String SQL = "select * from audit_main left ou" +
            "ter join audit_admin on audit_main.objectid=audit_admin.objectid left outer join audit_message on audit_main." +
            "objectid=audit_message.objectid left outer join audit_system on audit_main.objectid=audit_system.objecti" +
            "d";
    private static final String SIG_XML = "<audit:AuditMetadata xmlns:audit=\"http://l7tech.com/ns/2004/Oct/08/audit\" />";
    private static final char DELIM = ':';
    private static final Pattern badCharPattern = Pattern.compile("([^\\040-\\0176]|\\\\|\\" + DELIM + ")");

    private AuditExporter() {}

    static String quoteMeta(String raw) {
        return badCharPattern.matcher(raw).replaceAll("\\\\$1");
    }

    static class MessageDigestOutputStream extends OutputStream {
        private final MessageDigest digest;
        private final OutputStream wrapped;

        /**
         * Creates a new OutputStream that wraps the specified outputstream in a layer that computes a hash as it goes.
         * @param wrapped the OutputStream to wrap.  All output is passed through to this OutputStream.
         * @param digest the MessageDigest algorithm to use to hash the output as it goes by.  When you are finished
         *               writing, call digest() to retrieve the hash.
         */
        MessageDigestOutputStream(OutputStream wrapped, MessageDigest digest) {
            this.wrapped = wrapped;
            this.digest = digest;
            digest.reset();
        }

        /**
         * Completes the digest, and returns the digest bytes.  This causes the digest to be reset --
         * anything further written to the OutputStream will be included in a new, unrelated digest.
         * @return the digest bytes.
         */
        public byte[] digest() {
            return digest.digest();
        }

        public void write(byte b[], int off, int len) throws IOException {
            digest.update(b, off, len);
            wrapped.write(b, off, len);
        }

        public void flush() throws IOException {
            wrapped.flush();
        }

        public void close() throws IOException {
            wrapped.close();
        }

        public void write(int b) throws IOException {
            digest.update((byte)b);
            wrapped.write(b);
        }
    }

    interface ExportedInfo {
        long getLowestId();
        long getHighestId();
        long getEarliestTime();
        long getLatestTime();
        byte[] getSha1Hash();
        byte[] getMd5Hash();
    }

    /**
     * Exports the audit records from the database to the specified OutputStream, in UTF-8 format.
     * The audits are exported as a colon-delimited text file with colon as the record delimiter and "\n" as the
     * field delimiter..
     * The row contains the column names; subsequent rows contain a dump of all the audit records.
     * No signature or other metadata is emitted.
     * @param rawOut the OutputStream to which the colon-delimited dump will be written.
     * @return the time in milliseconds of the most-recent audit record exported.
     */
    private static ExportedInfo exportAllAudits(OutputStream rawOut) throws SQLException, IOException, HibernateException {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            MessageDigestOutputStream sha1Out = new MessageDigestOutputStream(rawOut, sha1);
            MessageDigestOutputStream md5Out = new MessageDigestOutputStream(sha1Out, md5);
            PrintStream out = new PrintStream(md5Out, false, "UTF-8");
            HibernatePersistenceManager.initialize();

            Session session = ((HibernatePersistenceContext)PersistenceContext.getCurrent()).getAuditSession();
            conn = session.connection();
            st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
            rs = st.executeQuery(SQL);
            if (rs == null) throw new SQLException("Unable to obtain audits with query: " + SQL);
            ResultSetMetaData md = rs.getMetaData();

            int timecolumn = 3;
            int columns = md.getColumnCount();
            for (int i = 1; i <= columns; ++i) {
                final String columnName = quoteMeta(md.getColumnName(i));
                if ("time".equalsIgnoreCase(columnName))
                    timecolumn = i;
                out.print(columnName);
                if (i < columns) out.print(DELIM);
            }
            out.print("\n");

            long lowestId = Long.MAX_VALUE;
            long highestId = Long.MIN_VALUE;
            long lowestTime = Long.MAX_VALUE;
            long highestTime = Long.MIN_VALUE;
            while (rs.next()) {
                for (int i = 1; i <= columns; ++i) {
                    String data = rs.getString(i);
                    if (data != null) {
                        if (i == timecolumn) {
                            long millis = rs.getLong(i);
                            if (millis > highestTime)
                                highestTime = millis;
                            if (millis < lowestTime)
                                lowestTime = millis;
                        } else if (i == 1) {
                            long id = rs.getLong(i);
                            if (id > highestId)
                                highestId = id;
                            if (id < lowestId)
                                lowestId = id;
                        }
                        out.print(quoteMeta(data));
                    }
                    if (i < columns) out.print(DELIM);
                }
                out.print("\n");
            }

            out.flush();

            final long finalLowestId = lowestId;
            final long finalHighestId = highestId;
            final long finalLowestTime = lowestTime;
            final long finalHighestTime = highestTime;
            final byte[] sha1Digest = sha1Out.digest();
            final byte[] md5Digest = md5Out.digest();

            return new ExportedInfo() {
                public long getLowestId() {
                    return finalLowestId;
                }

                public long getHighestId() {
                    return finalHighestId;
                }

                public long getEarliestTime() {
                    return finalLowestTime;
                }

                public long getLatestTime() {
                    return finalHighestTime;
                }

                public byte[] getSha1Hash() {
                    return sha1Digest;
                }

                public byte[] getMd5Hash() {
                    return md5Digest;
                }
            };

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        } finally {
            try {
                if (rs != null) rs.close();
            } finally {
                try {
                    if (st != null) st.close();
                } finally {
                    if (conn != null) conn.close();
                }
            }
        }
    }

    public static Element createEnvelopedSignature(Element elementToSign,
                                               X509Certificate senderSigningCert,
                                               PrivateKey senderSigningKey)
            throws SignatureException, SignatureStructureException, XSignatureException
    {
        String signaturemethod = null;
        if (senderSigningKey instanceof RSAPrivateKey)
            signaturemethod = SignatureMethod.RSA;
        else if (senderSigningKey instanceof DSAPrivateKey)
            signaturemethod = SignatureMethod.DSA;
        else if (senderSigningKey instanceof SecretKey)
            signaturemethod = SignatureMethod.HMAC;
        else {
            throw new SignatureException("PrivateKey type not supported " +
                                               senderSigningKey.getClass().getName());
        }

        // Create signature template and populate with appropriate transforms. Reference is to SOAP Envelope
        TemplateGenerator template = new TemplateGenerator(elementToSign.getOwnerDocument(),
                                                           XSignature.SHA1, Canonicalizer.EXCLUSIVE, signaturemethod);
        template.setPrefix("ds");

        // Add enveloped signature of entire document
        final Element root = elementToSign;
        String rootId = root.getAttribute("Id");
        if (rootId == null || rootId.length() < 1) {
            rootId = "root";
            root.setAttribute("Id", rootId);
        }
        Reference rootRef = template.createReference("#" + rootId);
        rootRef.addTransform(Transform.ENVELOPED);
        rootRef.addTransform(Transform.C14N_EXCLUSIVE);
        template.addReference(rootRef);

        // Get the signature element
        Element sigElement = template.getSignatureElement();

        // Include KeyInfo element in signature and embed cert into subordinate X509Data element
        KeyInfo keyInfo = new KeyInfo();
        keyInfo.setKeyValue(senderSigningCert.getPublicKey());
        KeyInfo.X509Data x5data = new KeyInfo.X509Data();
        x5data.setCertificate(senderSigningCert);
        x5data.setParameters(senderSigningCert, true, true, true);
        keyInfo.setX509Data(new KeyInfo.X509Data[] { x5data });
        keyInfo.insertTo(sigElement);

        SignatureContext sigContext = new SignatureContext();
        sigContext.setIDResolver(new IDResolver() {
            public Element resolveID(Document document, String s) {
                return s.equals("root") ? root : null;
            }
        });
        sigContext.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws IOException {
                throw new FileNotFoundException("No external ref should have been present");
            }
        });
        sigContext.setResourceShower(new ResourceShower() {
            public void showSignedResource(Element element, int i, String s, String s1, byte[] bytes, String s2) {

            }
        });
        Element signedSig = sigContext.sign(sigElement, senderSigningKey);
        return signedSig;
    }

    private static class CausedSignatureException extends SignatureException {
        public CausedSignatureException() {
        }

        public CausedSignatureException(String msg) {
            super(msg);
        }

        public CausedSignatureException(Throwable cause) {
            super();
            initCause(cause);
        }

        public CausedSignatureException(String msg, Throwable cause) {
            super(msg);
            initCause(cause);
        }
    }

    private static void addElement(Element parent, String indent, String ns, String p, String name, String value) {
        parent.appendChild(XmlUtil.createTextNode(parent, indent));
        Element e = XmlUtil.createAndAppendElementNS(parent, name, ns, p);
        e.appendChild(XmlUtil.createTextNode(e, value));
        parent.appendChild(XmlUtil.createTextNode(parent, "\n"));
    }

    /**
     * Export all audit events from the database to the specified OutputStream as a Zip file, including a signature.
     * @param fileOut OutputStream to which the Zip file will be written.
     * @return the time in milliseconds of the most-recent audit record exported.
     */
    public static long exportAuditsAsZipFile(OutputStream fileOut,
                                             X509Certificate signingCert,
                                             PrivateKey signingKey)
            throws IOException, SQLException, HibernateException, SignatureException
    {
        final long startTime = System.currentTimeMillis();
        ZipOutputStream zipOut = null;
        try {
            zipOut = new ZipOutputStream(fileOut);
            final String dateString = ISO8601Date.format(new Date(startTime));
            zipOut.setComment(BuildInfo.getBuildString() + " - Exported Audit Records - Created " + dateString);
            ZipEntry ze = new ZipEntry("audit.dat");
            zipOut.putNextEntry(ze);
            ExportedInfo exportedInfo = exportAllAudits(zipOut);
            long highestTime = exportedInfo.getLatestTime();
            zipOut.flush();
            final long endTime = System.currentTimeMillis();
            final String endTimeString = ISO8601Date.format(new Date(endTime));

            // Create XML signature
            Document d = null;
            d = XmlUtil.stringToDocument(SIG_XML);
            Element auditMetadata = d.getDocumentElement();
            String ns = auditMetadata.getNamespaceURI();
            String p = auditMetadata.getPrefix();
            auditMetadata.appendChild(XmlUtil.createTextNode(auditMetadata, "\n"));
            final String i1 = "    ";
            addElement(auditMetadata, i1, ns, p, "exportProcessStarting", dateString);
            addElement(auditMetadata, i1, ns, p, "exportProcessStartingMillis", String.valueOf(startTime));
            auditMetadata.appendChild(XmlUtil.createTextNode(auditMetadata, "\n"));
            addElement(auditMetadata, i1, ns, p, "exportProcessFinishing", endTimeString);
            addElement(auditMetadata, i1, ns, p, "exportProcessFinishingMillis", String.valueOf(endTime));
            auditMetadata.appendChild(XmlUtil.createTextNode(auditMetadata, "\n" + i1));

            Element ead = XmlUtil.createAndAppendElementNS(auditMetadata, "ExportedAuditData", ns, p);
            ead.setAttribute("filename", "audit.dat");
            ead.appendChild(XmlUtil.createTextNode(auditMetadata, "\n"));
            auditMetadata.appendChild(XmlUtil.createTextNode(auditMetadata, "\n"));

            final String i2 = "        ";
            addElement(ead, i2, ns, p, "lowestAuditRecordId", String.valueOf(exportedInfo.getLowestId()));
            addElement(ead, i2, ns, p, "earliestAuditRecordDate",
                       ISO8601Date.format(new Date(exportedInfo.getEarliestTime())));
            addElement(ead, i2, ns, p, "earliestAuditRecordDateMillis", String.valueOf(exportedInfo.getEarliestTime()));
            ead.appendChild(XmlUtil.createTextNode(ead, "\n"));
            addElement(ead, i2, ns, p, "highestAuditRecordId", String.valueOf(exportedInfo.getHighestId()));
            addElement(ead, i2, ns, p, "latestAuditRecordDate",
                       ISO8601Date.format(new Date(exportedInfo.getLatestTime())));
            addElement(ead, i2, ns, p, "latestAuditRecordDateMillis", String.valueOf(exportedInfo.getLatestTime()));
            ead.appendChild(XmlUtil.createTextNode(ead, "\n"));
            addElement(ead, i2, ns, p, "sha1Digest", HexUtils.hexDump(exportedInfo.getSha1Hash()));
            addElement(ead, i2, ns, p, "md5Digest", HexUtils.hexDump(exportedInfo.getMd5Hash()));
            ead.appendChild(XmlUtil.createTextNode(ead, i1));

            Element signature = createEnvelopedSignature(auditMetadata,
                                                         signingCert,
                                                         signingKey);
            auditMetadata.appendChild(signature);

            zipOut.putNextEntry(new ZipEntry("sig.xml"));
            XmlUtil.nodeToOutputStream(d, zipOut);
            return highestTime;
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        } catch (SignatureStructureException e) {
            throw new CausedSignatureException(e);
        } catch (XSignatureException e) {
            throw new CausedSignatureException(e);
        } finally {
            if (zipOut != null) zipOut.close();
        }
    }
}
