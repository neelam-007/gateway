package com.l7tech.security.prov.luna;

import com.l7tech.util.SyspropUtil;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 *
 */
public class DefaultLunaPinFinder implements Callable<char[]> {
    @Override
    public char[] call() throws IOException {
        String val = SyspropUtil.getString("com.l7tech.lunaPin", null);
        if (val == null) throw new IOException("To use Luna, either set the system property com.l7tech.lunaPin to the Luna client password (in the format XXXX-XXXX-XXXX-XXXX), or set the system property com.l7tech.lunaPinFinder to the class name of a Callable<char[]> implementation in the Gateway's classpath");
        return val.toCharArray();
    }
}
