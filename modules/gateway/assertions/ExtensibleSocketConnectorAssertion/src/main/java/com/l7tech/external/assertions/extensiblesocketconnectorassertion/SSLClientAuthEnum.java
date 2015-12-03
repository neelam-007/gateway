package com.l7tech.external.assertions.extensiblesocketconnectorassertion;

import com.l7tech.util.XmlSafe;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 02/12/11
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlSafe
public enum SSLClientAuthEnum {
    DISABLED("Disabled"),
    OPTIONAL("Optional"),
    REQUIRED("Required");

    private String displayName;

    private SSLClientAuthEnum(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
