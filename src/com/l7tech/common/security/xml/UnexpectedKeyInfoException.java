/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Mar 14, 2005<br/>
 */
package com.l7tech.common.security.xml;

import java.security.GeneralSecurityException;

/**
 * Describes a case where a KeyInfo refers to a key that is different from the key we are expecting.
 *
 * @author flascelles@layer7-tech.com
 */
public class UnexpectedKeyInfoException extends GeneralSecurityException {
    public UnexpectedKeyInfoException(String msg) {
        super(msg);
    }
}
