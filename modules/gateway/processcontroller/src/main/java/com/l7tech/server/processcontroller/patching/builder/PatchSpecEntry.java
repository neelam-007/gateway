package com.l7tech.server.processcontroller.patching.builder;

import java.util.jar.JarOutputStream;
import java.io.IOException;

/**
 * A patch entry specification has a (JAR) entry name and knows how to add itself to a JAR OutputStream.
 *
 * @author jbufu
 */
public interface PatchSpecEntry {

    String getEntryName();

    void toJar(JarOutputStream jos) throws IOException;

    void setEntryName(String entryName);
}
