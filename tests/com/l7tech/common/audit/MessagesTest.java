package com.l7tech.common.audit;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author $Author$
 * @version $Revision$
 */
public class MessagesTest extends TestCase {

    /**
      * create the <code>TestSuite</code> for the MessagesTest <code>TestCase</code>
      */
    public static Test suite() {
        TestSuite suite = new TestSuite(MessagesTest.class);
        return suite;
    }

    /**
     * test that all message formats are acceptable
     */
    public void testMessageText() {
        for(int i=0; i<15000; i++) {
            String messageText = Messages.getMessageById(i);
            if(messageText!=null) {
                for(int t=0; t<10; t++) {
                    int index = messageText.indexOf("'{"+t+"}'");
                    if(index >=0 && (index==0 || messageText.charAt(index-1)!='\'')) {
                        fail("Message '"+i+"', has invalid quoting '"+messageText+"' (use escaped quotes e.g. ''{0}'').");
                    }
                }
            }
        }
    }

}
