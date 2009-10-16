package com.l7tech.server.processcontroller.patching;

import java.io.*;

/**
 * @author jbufu
 */
public class HelloFileWorldPatch {

    public static void main(String[] args) {
        
        File hello = new File("/tmp/helloworld.txt");
        hello.deleteOnExit();
        OutputStream out = null;
        try {
            out = new FileOutputStream(hello);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        PrintWriter writer = new PrintWriter(out);
        writer.write("Hello file!\n");
        writer.close();
    }
}