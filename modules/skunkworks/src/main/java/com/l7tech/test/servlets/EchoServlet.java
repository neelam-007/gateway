package com.l7tech.test.servlets;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Disk-based echo servlet.  Spools entire post into a temp file; then reads it back and returns it.
 * If it can't find a disk-based spoolDir, uses a memory-based spool instead.
 */
public class EchoServlet extends HttpServlet {
    protected static final Logger logger = Logger.getLogger(EchoServlet.class.getName());

    File spoolDir;

    public void init(ServletConfig config) throws ServletException {
        String spoolPath = config.getInitParameter("spoolPath");
        spoolDir = new File(spoolPath);
        if (!(spoolPath != null && spoolPath.length() > 0 && spoolDir.exists() && spoolDir.isDirectory() && spoolDir.canWrite() && spoolDir.canRead())) {
            logger.log(Level.WARNING, "spoolPath not found or not a writable directory - requests will be limited to what can fit into memory");
            spoolDir = null;
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.setStatus(200);
        PrintStream out = new PrintStream(resp.getOutputStream());
        out.println("Ready to echo POST requests to responses at this URL.");
        if (spoolDir == null) {
            out.println("spool directory missing, not configured, or not writable -- using memory spooling instead");
        } else {
            out.println("spooling files to " + spoolDir.getAbsolutePath());
        }
    }

    protected void doPost(HttpServletRequest hreq, HttpServletResponse hresp) throws ServletException, IOException {
        String ctype = hreq.getContentType();

        File file = spoolDir == null ? null : File.createTempFile("echo", null, spoolDir);
        ByteArrayOutputStream baos = null;
        OutputStream fos = null;
        InputStream fis = null;
        try {
            fos = file == null ? (baos = new ByteArrayOutputStream()) : new FileOutputStream(file);
            copyStream(hreq.getInputStream(), fos);
            fos.close();
            fos = null;

            fis = baos != null ? new ByteArrayInputStream(baos.toByteArray()) : new FileInputStream(file);
            hresp.setStatus(200);
            hresp.setContentType(ctype);
            copyStream(fis, hresp.getOutputStream());
            fis.close();
            fis = null;
        } finally {
            if (fos != null) try { fos.close(); } catch (IOException e) { /* ignore */ }
            if (fis != null) try { fis.close(); } catch (IOException e) { /* ignore */ }
            if (file != null) file.delete();
        }
    }

    /**
     * Copy all of the in, right up to EOF, into out.  Does not flush or close either stream.
     *
     * @param in  the InputStream to read.  Must not be null.
     * @param out the OutputStream to write.  Must not be null.
     * @return the number bytes copied
     * @throws IOException if in could not be read, or out could not be written
     */
    public static long copyStream(InputStream in, OutputStream out) throws IOException {
        if (in == null || out == null) throw new NullPointerException("in and out must both be non-null");
        byte[] buf = new byte[8192];
        int got;
        long total = 0;
        while ((got = in.read(buf)) > 0) {
            out.write(buf, 0, got);
            total += got;
        }
        return total;
    }
}
