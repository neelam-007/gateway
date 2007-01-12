package com.l7tech.test.servlets;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * Disk-based echo servlet.  Spools entire post into a temp file; then reads it back and returns it.
 */
public class EchoServlet extends HttpServlet {
    File spoolDir;

    public void init(ServletConfig config) throws ServletException {
        String spoolPath = config.getInitParameter("spoolPath");
        spoolDir = new File(spoolPath);
        if (!(spoolPath != null && spoolPath.length() > 0 && spoolDir.exists() && spoolDir.isDirectory() && spoolDir.canWrite() && spoolDir.canRead()))
            throw new ServletException("spoolPath is not a writable directory");
    }

    protected void doPost(HttpServletRequest hreq, HttpServletResponse hresp) throws ServletException, IOException {
        String ctype = hreq.getContentType();

        File file = File.createTempFile("echo", null, spoolDir);
        FileOutputStream fos = null;
        FileInputStream fis = null;
        try {
            fos = new FileOutputStream(file);
            copyStream(hreq.getInputStream(), fos);
            fos.close();
            fos = null;

            fis = new FileInputStream(file);
            hresp.setStatus(200);
            hresp.setContentType(ctype);
            copyStream(fis, hresp.getOutputStream());
            fis.close();
            fis = null;
        } finally {
            if (fos != null) try { fos.close(); } catch (IOException e) { /* ignore */ }
            if (fis != null) try { fis.close(); } catch (IOException e) { /* ignore */ }
            file.delete();
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
