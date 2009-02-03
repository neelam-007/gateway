package com.l7tech.server;

import javax.servlet.http.*;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.*;
import java.security.SecureRandom;
import java.util.TimerTask;
import java.util.Timer;

import org.apache.commons.collections.map.LRUMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.util.IOUtils;

/**
 * @author: ghuang
 */

/**
 * Receive a file content from the client and send a file back to the browser of the client.  The class resolves
 * an issue - untrusted applets don't have permission to write a file on local disk.
 */
public class FileDownloadServlet extends HttpServlet {

    /**
     * Process the file content and send back a file containing the file content.
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    private final int INITIAL_TASK_DELAY = 30000;  // after 30 seconds, run the timer task at the first time.
    private final int NORMAL_TASK_DELAY =  90000;  // in every 90-second, run the timer task after the first time.
    private final int FILE_EXPIRY_PERIOD = 60000;  // each file owns a 60-second of life time.
    private final SecureRandom random = new SecureRandom();
    private final LRUMap fileMap = new LRUMap(10);

    private WebApplicationContext applicationContext;
    private Timer timer;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }

        timer = (Timer)applicationContext.getBean("managedBackgroundTimer", Timer.class);
        timer.schedule(new TimerTask() {
            public void run() {
                removeExpiredFiles();
            }
        }, INITIAL_TASK_DELAY, NORMAL_TASK_DELAY);
    }
    
    /**
     * Receive a file from the client, who has uploaded the file.
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Get the file name
        String fileName = req.getParameter("filename");

        // Receive the file content
        byte[] fileContent = IOUtils.slurpStream(req.getInputStream(), 100000);

        // Form a key for the file
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        String key = HexUtils.hexDump(bytes);

        // Save the file info into the map
        synchronized (fileMap) {
            fileMap.put(key, new FileInfo(fileContent, fileName));
        }

        // Send back the file key
        resp.setContentType(ContentTypeHeader.TEXT_DEFAULT.getFullValue());
        resp.getWriter().write(key);
    }

    /**
     * Send back a file to the client, who is going to download the file.
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Get the file content
        String key = req.getParameter("key");
        FileInfo fileInfo;
        synchronized (fileMap) {
            fileInfo = (FileInfo) fileMap.get(key);
        }

        // Check if the file exists or not.
        if (fileInfo == null) {
            resp.sendError(404, "Not such file exists.");
            return;
        }

        // Set response
        resp.reset();
        resp.addHeader("Content-Disposition", "attachment; filename=\"" + fileInfo.fileName + "\"");
        resp.setContentType("application/octet-stream");

        // Send the file back to the client.
        OutputStream out = resp.getOutputStream();
        out.write(fileInfo.data);
        out.flush();
        out.close();
    }

    /**
     * Store file information such as file name, file content, and timestamp.
     */
    private static final class FileInfo {
        private final byte[] data;
        private final String fileName;
        private final long time;

        FileInfo(final byte[] data, final String name) {
            this.data = data;
            this.fileName = name;
            this.time = System.currentTimeMillis();
        }

        long getTime() {
            return time;
        }
    }

    /**
     * Remove those expired files in the map
     */
    private void removeExpiredFiles() {
        synchronized (fileMap) {
            if (fileMap.isEmpty()) return;

            Object currKey = fileMap.firstKey();
            while (currKey != null) {
                FileInfo fileInfo = (FileInfo)fileMap.get(currKey);
                long time = fileInfo.getTime();
                long currTime = System.currentTimeMillis();
                if ((currTime - time) >= FILE_EXPIRY_PERIOD) {
                    fileMap.remove(currKey);
                }

                if (fileMap.isEmpty()) {
                    return;
                } else {
                    currKey = fileMap.nextKey(currKey);
                }
            }
        }
    }
}