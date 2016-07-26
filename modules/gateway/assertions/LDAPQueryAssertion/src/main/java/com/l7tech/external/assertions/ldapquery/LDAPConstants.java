package com.l7tech.external.assertions.ldapquery;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LDAPConstants{

    public static final String SCOPE_SUBTREE = "SUBTREE";
    public static final String SCOPE_ONELEVEL = "ONELEVEL";
    public static final String SCOPE_OBJECT= "OBJECT";

    private static final Map<String,String> SCOPE = new HashMap<String,String>();

    public static Map<String,String> SCOPEREF(){
        if(0 != SCOPE.size()){
            return Collections.unmodifiableMap(SCOPE);
        }
        SCOPE.put(SCOPE_SUBTREE, "0");
        SCOPE.put(SCOPE_ONELEVEL, "1");
        SCOPE.put(SCOPE_OBJECT, "2");
        SCOPE.put("0", SCOPE_SUBTREE);
        SCOPE.put("1", SCOPE_ONELEVEL);
        SCOPE.put("2",SCOPE_OBJECT);
        return Collections.unmodifiableMap(SCOPE);
    }
}