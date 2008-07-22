package com.l7tech.gateway.common.audit;

import java.util.logging.Level;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.Messages;

/**
 * @author $Author$
 * @version $Revision$
 */
public class MessagesTest extends TestCase {

    private static final int MESSAGE_MAX_ID = 20000;

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
        for(int i=0; i<MESSAGE_MAX_ID; i++) {
            AuditDetailMessage message = Messages.getAuditDetailMessageById(i);
            String messageText = message==null ? null : message.getMessage();
            if(messageText!=null) {
                // Check for odd numbers of single quotes, this is a an error since a single quote should be escaped
                char[] characters = messageText.toCharArray();
                int count = 0;
                for (int j = 0; j < characters.length; j++) {
                    char character = characters[j];
                    if(character == '\'') count++;
                }
                if(count%2!=0) {
                    fail("Message '"+i+"', has invalid quoting '"+messageText+"' (use escaped quotes e.g. '' for ').");
                }

                // Check for quoted substitutions e.g. text such as '{0}', which is usually an error
                for(int t=0; t<10; t++) {
                    int index = messageText.indexOf("'{"+t+"}'");
                    if(index >=0 && (index==0 || messageText.charAt(index-1)!='\'')) {
                        fail("Message '"+i+"', has invalid quoting '"+messageText+"' (use escaped quotes e.g. ''{0}'').");
                    }
                }
            }
        }
    }

    /**
     * test that all message formats are acceptable
     */
    public void testMessageLevel() {
        for(int i=0; i<MESSAGE_MAX_ID; i++) {
            AuditDetailMessage message = Messages.getAuditDetailMessageById(i);
            Level level = message==null ? null : message.getLevel();
            if(level!=null) {
                if (level.intValue() >= Level.SEVERE.intValue()) {
                    fail("Message '"+i+"', level is too high (must be < SEVERE).");                    
                }
            }
        }
    }

    /**
     * Test for duplicated assertion status
     */
    public void assertionStatusTest() throws Exception {
        Field[] fields = AssertionStatus.class.getFields();
        for (int i=0; i<fields.length; i++) {
            Field field = fields[i];
            if (field.getType().isAssignableFrom(AssertionStatus.class) &&
                (field.getModifiers()&Modifier.STATIC) > 0) {
                AssertionStatus status = (AssertionStatus) field.get(null);
                AssertionStatus byid = AssertionStatus.fromInt(status.getNumeric());

                // 402 is a known dupe, ignore it
                if (status != byid && status.getNumeric()!=402) {
                    fail("Duplicate assertion status id : " + status.getNumeric());
                }
            }
        }
    }
}
