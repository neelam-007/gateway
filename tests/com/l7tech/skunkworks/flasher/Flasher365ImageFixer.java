package com.l7tech.skunkworks.flasher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Fixes 3.6.5 flasher images
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 17, 2007<br/>
 */
public class Flasher365ImageFixer {
    public static void main(String[] args) throws Exception {
        String pathOfBadSQL = "/Users/flascell/tmp/test_poc_dump_exported/dbdump_restore.sql";
        File badOne = new File (pathOfBadSQL);
        File renamed = new File (pathOfBadSQL + ".beforefixed");
        badOne.renameTo(renamed);

        FileInputStream fis = new FileInputStream(pathOfBadSQL + ".beforefixed");
        FileOutputStream fos = new FileOutputStream(pathOfBadSQL);
        byte tmp[] = new byte[1];
        int read = fis.read(tmp);
        while (read > 0) {
            if (tmp[0] == '\r') {
                System.out.println("\\r detected!");
                fos.write("\\r".getBytes());
            } else {
                fos.write(tmp);
            }
            read = fis.read(tmp);
        }
        fis.close();
        fis.close();
    }
}
