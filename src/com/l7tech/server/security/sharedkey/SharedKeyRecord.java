package com.l7tech.server.security.sharedkey;

import java.io.Serializable;

/**
 * Hibernate abstraction of a row in the shared_keys table.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 18, 2007<br/>
 */
public class SharedKeyRecord implements Serializable {
    public static final String GENERIC_KEY_NAME = "GenSharedSSGSymmKey";
    private String name;
    private String b64edKey;


    public SharedKeyRecord() {
        name = GENERIC_KEY_NAME;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getB64edKey() {
        return b64edKey;
    }

    public void setB64edKey(String b64edKey) {
        this.b64edKey = b64edKey;
    }
}
