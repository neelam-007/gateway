package com.l7tech.common.audit;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
        Pattern pat = Pattern.compile("\\{[0-9]\\}");

        for(int i=0; i<15000; i++) {
            String messageText = Messages.getMessageById(i);
            if(messageText!=null) {
                // Check for odd numbers of single quotes, this is a an error since a single quote should be escaped
                Matcher matcher = pat.matcher(messageText);
                if(matcher.find()) {
                    char[] characters = messageText.toCharArray();
                    int count = 0;
                    for (int j = 0; j < characters.length; j++) {
                        char character = characters[j];
                        if(character == '\'') count++;
                    }
                    if(count%2!=0) {
                        fail("Message '"+i+"', has invalid quoting '"+messageText+"' (use escaped quotes e.g. '' for ').");
                    }
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

}
