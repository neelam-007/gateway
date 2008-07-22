/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Mar 14, 2005<br/>
 */
package com.l7tech.security.xml;

import com.l7tech.security.xml.processor.ProcessorException;

/**
 * Describes a case where a KeyInfo refers to a key that is unrecognized or different from the key we are expecting.
 *
 * @author flascelles@layer7-tech.com
 */
public class UnexpectedKeyInfoException extends ProcessorException {
    public UnexpectedKeyInfoException(String msg) {
        super(msg);
    }
}
