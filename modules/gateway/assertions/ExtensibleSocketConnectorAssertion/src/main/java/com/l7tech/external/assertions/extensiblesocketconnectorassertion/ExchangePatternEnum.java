package com.l7tech.external.assertions.extensiblesocketconnectorassertion;

import com.l7tech.util.XmlSafe;
//import org.apache.camel.ExchangePattern;

/**
 * @author omogstad
 */
@XmlSafe
public enum ExchangePatternEnum {

    OutOnly("OutOnly"),
    OutIn("OutIn");

    private String displayName;

    private ExchangePatternEnum(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
