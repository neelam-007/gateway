/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.util;

import java.io.*;
import java.util.*;

/**
 * Utilities for text mode programs.
 */
public class TextUtils {
    /**
     * Pad the specified string to the specified width by appending spaces.
     * If the input string is already at least as long as the desired width it will
     * be returned unmodified.
     *
     * @param str    The string to pad.  null is considered to be the same as the empty string.
     * @param width  the width to pad the string.  Must be nonnegative.
     * @return the padded string.  Never null.
     */
    public static String pad(String str, int width) {
        assert width >= 0;
        if (str == null) str = "";
        StringBuffer sb = new StringBuffer(str);
        for (int i = str.length(); i < width; ++i)
            sb.append(' ');
        return sb.toString();
    }

    /**
     * Attempt to mask a password being read from an inputstream by spamming the (hopefully) corresponding
     * outputstream with backspace followed by asterisk.  Code adapted from example at
     * http://java.sun.com/developer/technicalArticles/Security/pwordmask/
     * (accessed Feb 09, 2006).  Page contained the following license grant:
     * "Feel free to reuse, improve, and adapt the code in this article for your applications."
     *
     * @param in stream to be used (e.g. System.in)
     * @param out  the outputstream where, it is feared, the typed input characters may be echoed (e.g. System.out)
     * @param prompt The prompt to display to the user.
     * @return The password as entered by the user.  May be null or empty.
     */
    public static char[] getPassword(InputStream in, OutputStream out, String prompt) throws IOException {
        MaskingThread maskingthread = new MaskingThread(out, prompt);
        Thread thread = new Thread(maskingthread);
        thread.start();

        char[] lineBuffer;
        char[] buf;
        int i;

        buf = lineBuffer = new char[128];

        int room = buf.length;
        int offset = 0;
        int c;

        loop:   while (true) {
            c = in.read();
            thread.interrupt();
            switch (c) {
                case -1:
                case '\n':
                    break loop;

                case '\r':
                    int c2 = in.read();
                    if ((c2 != '\n') && (c2 != -1)) {
                        if (!(in instanceof PushbackInputStream)) {
                            in = new PushbackInputStream(in);
                        }
                        ((PushbackInputStream)in).unread(c2);
                    } else {
                        break loop;
                    }

                default:
                    if (--room < 0) {
                        buf = new char[offset + 128];
                        room = buf.length - offset - 1;
                        System.arraycopy(lineBuffer, 0, buf, 0, offset);
                        Arrays.fill(lineBuffer, ' ');
                        lineBuffer = buf;
                    }
                    buf[offset++] = (char) c;
                    break;
            }
        }
        maskingthread.stopMasking();
        if (offset == 0) {
            return null;
        }
        char[] ret = new char[offset];
        System.arraycopy(buf, 0, ret, 0, offset);
        Arrays.fill(buf, ' ');
        return ret;
    }

    /**
     * This class attempts to erase characters echoed to the console.
     */
    private static class MaskingThread implements Runnable {
        private final OutputStream out;
        /** @noinspection FieldCanBeLocal*/
        private volatile boolean stop;
        private char echochar = '*';

        /**
         * @param console the outputstream where, it is feared, the typed input characters may be echoed
         * @param prompt The prompt displayed to the user
         */
        public MaskingThread(OutputStream console, String prompt) throws IOException {
            this.out = console;
            console.write(prompt.getBytes());
        }

        /**
         * Begin masking until asked to stop.
         */
        public void run() {

            int priority = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            try {
                stop = true;
                while(stop) {
                    try {
                        out.write('\010');
                        out.write(echochar);
                        // attempt masking at this rate
                        Thread.sleep(400);
                    } catch (InterruptedException iex) {
                        // Continue
                    } catch (IOException e) {
                        // May as well give up masking
                        stopMasking();
                        break;
                    }
                }
            } finally { // restore the original priority
                Thread.currentThread().setPriority(priority);
            }
        }

        /**
         * Instruct the thread to stop masking.
         */
        public void stopMasking() {
            this.stop = false;
        }
    }
}
