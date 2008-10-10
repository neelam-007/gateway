package com.l7tech.common.io;

import java.io.OutputStream;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.security.MessageDigest;
import java.security.DigestOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.HashMap;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

/**
 * Wraps an output stream with the following filter succession:
 * [message digest]* -> byte count -> zip -> byte count -> buffer -> original output stream.
 *
 * @author jbufu
 */
public class DigestZipOutputStream extends OutputStream {

    private OutputStream buffered;
    private ByteCountOutputStream zipcount;
    private ZipOutputStream zip;
    private ByteCountOutputStream rawcount;
    private OutputStream os;

    private HashMap<String, DigestOutputStream> digestStreams = new HashMap<String,DigestOutputStream>();

    public DigestZipOutputStream(OutputStream out, List<String> digests) throws NoSuchAlgorithmException {
        buffered = new BufferedOutputStream(out);
        zipcount = new ByteCountOutputStream(buffered);
        zip = new ZipOutputStream(zipcount);
        rawcount = new ByteCountOutputStream(zip);
        os = rawcount;
        for (String digestAlg : digests) {
            MessageDigest md = MessageDigest.getInstance(digestAlg);
            os = new DigestOutputStream(os, md);
            digestStreams.put(md.getAlgorithm(), (DigestOutputStream) os);
        }
    }

    // OutputStream implementation
    public void write(int b) throws IOException {
        os.write(b);
    }

    public void write(byte b[]) throws IOException {
	    os.write(b, 0, b.length);
    }

    public void write(byte b[], int off, int len) throws IOException {
        os.write(b, off, len);
    }

    public void flush() throws IOException {
        os.flush();
        buffered.flush();
    }

    public void close() throws IOException {
        os.close();
        buffered.close();
    }

    // digest
    public byte[] getDigest(String digestAlg) {
        DigestOutputStream stream = digestStreams.get(digestAlg);
        return stream == null ? null : stream.getMessageDigest().digest();
    }

    public void resetDigest(String digestAlg) {
        digestStreams.get(digestAlg).getMessageDigest().reset();
    }

    public void resetDigests(List<String> digestAlgs) {
        for (String digestAlg : digestAlgs)
            resetDigest(digestAlg);
    }

    // zip
    public void putNextEntry(ZipEntry e) throws IOException {
        zip.putNextEntry(e);
    }

    public void setComment(String comment) {
        zip.setComment(comment);
    }

    // counts
    public long getZippedByteCount() {
        return zipcount.getByteCount();
    }

    public long getRawByteCount() {
        return rawcount.getByteCount();
    }

    public double getCompressionRatio() {
        long zipBytes = zipcount.getByteCount();
        return zipBytes > 0 ? rawcount.getByteCount() / zipBytes : 1;
    }
}
