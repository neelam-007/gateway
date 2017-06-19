package com.l7tech.external.assertions.quickstarttemplate.server.parser;

import org.jetbrains.annotations.NotNull;

//import java.util.HashMap;
import java.util.Map;

/**
 * Holds assertion support information
 */
public class AssertionSupport {
//    final private String assertionRegistryExternalName;
    final private String className;   // the fully qualified name of the desired class
    final private Map<String, String> nameMap;

    AssertionSupport(@NotNull String className, @NotNull Map<String, String> nameMap) {   // , @NotNull String assertionRegistryExternalName
//        this.assertionRegistryExternalName = assertionRegistryExternalName;
        this.className = className;
        this.nameMap = nameMap;
    }

//    @NotNull
//    public String getAssertionRegistryExternalName() {
//        return assertionRegistryExternalName;
//    }

    @NotNull
    public String getClassName() {
        return className;
    }

    @NotNull
    public Map<String, String> getNameMap() {
        return nameMap;
    }
}
