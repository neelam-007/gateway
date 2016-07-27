package com.l7tech.external.assertions.ldapquery;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds Mapping between GUI and Logical SCOPES.
 * Could contain other LDAP related Constants and Utilities.
 *
 * <p/>
 * <p/>
 * <br/><br/>
 * CA TECHNOLOGIES, INC<br/>
 * User: jaibh01<br/>
 * Date: July 26, 2016<br/>

 *
 */
public class LDAPConstants{

    public static final String SUBTREE_SCOPE = "SUBTREE";
    public static final String ONELEVEL_SCOPE = "ONELEVEL";
    public static final String OBJECT_SCOPE= "OBJECT";

    //Map holding Bi Directional Data for easy retrieval, without dealing with Iterators.
    //Example : GUI Option Index <==> LOGICAL SCOPE
    private static final Map<String,String> SCOPE = new HashMap<String,String>(){
        {
            put(SUBTREE_SCOPE, "0");
            put(ONELEVEL_SCOPE, "1");
            put(OBJECT_SCOPE, "2");
            put("0", SUBTREE_SCOPE);
            put("1", ONELEVEL_SCOPE);
            put("2", OBJECT_SCOPE);
        }
    };

    public static Map<String,String> SCOPEREF(){
        return Collections.unmodifiableMap(SCOPE);
    }
}