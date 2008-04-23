package com.l7tech.autotest.logparser;

import javax.mail.*;
import java.io.FileWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 18-Dec-2007
 * Time: 10:45:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class MailParser {
    private static class MailMessage {
        public String sender;
        public String subject;
        public String testCase;
    }

    private HashSet<MailMessage> expectedMessages = new HashSet<MailMessage>();
    private HashSet<MailMessage> unexpectedMessages = new HashSet<MailMessage>();

    public MailParser() {
        // Get the list of expected mail messages
        Set<String> messageProperties = new HashSet<String>();
        Pattern pattern = Pattern.compile("(mail\\.messages\\.\\d+\\.).*");
        for(Enumeration e = Main.getProperties().propertyNames();e.hasMoreElements();) {
            Object key = e.nextElement();
            if(!(key instanceof String)) {
                continue;
            }
            String propertyName = (String)key;
            Matcher matcher = pattern.matcher(propertyName);
            if(matcher.matches()) {
                messageProperties.add(matcher.group(1));
            }
        }

        for(String propertyPrefix : messageProperties) {
            MailMessage mailMessage = new MailMessage();
            mailMessage.sender = Main.getProperties().getProperty(propertyPrefix + "sender");
            mailMessage.subject = Main.getProperties().getProperty(propertyPrefix + "subject");
            mailMessage.testCase = Main.getProperties().getProperty(propertyPrefix + "testCase");
            expectedMessages.add(mailMessage);
        }
    }

    protected void parseMessages() {
        Session session = Session.getDefaultInstance(new Properties());

        try {
            Store store = session.getStore(new URLName("mstor:" + Main.getProperties().getProperty("mail.mbox.file")));
            store.connect();

            Folder inbox = store.getDefaultFolder();
            inbox.open(Folder.READ_ONLY);

            for(Message message : inbox.getMessages()) {
                boolean found = false;
                for(Iterator<MailMessage> it = expectedMessages.iterator();it.hasNext();) {
                    MailMessage m = it.next();

                    if(message.getSubject().equals(m.subject)) {
                        for(Address address : message.getFrom()) {
                            if(address.toString().equals(m.sender)) {
                                it.remove();
                                found = true;
                                break;
                            }
                        }
                    }
                }

                if(!found) {
                    MailMessage unexpectedMessage = new MailMessage();
                    unexpectedMessage.sender = message.getFrom()[0].toString();
                    unexpectedMessage.subject = message.getSubject();
                    unexpectedMessage.testCase = "Unknown";

                    unexpectedMessages.add(unexpectedMessage);
                }
            }

            store.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    protected HashMap<String, String> getUnmatchedExpectedMessages() {
        HashMap<String, String> returnValue = new HashMap<String, String>();

        for(MailMessage m : expectedMessages) {
            returnValue.put(m.testCase, m.subject + "\n");
        }

        return returnValue;
    }

    protected HashMap<String, String> getUnexpectedMessages() {
        HashMap<String, String> returnValue = new HashMap<String, String>();

        for(MailMessage m : unexpectedMessages) {
            returnValue.put(m.testCase, m.subject + "\n");
        }

        return returnValue;
    }

    protected void clearInbox() {
        try {
            FileWriter fw = new FileWriter(Main.getProperties().getProperty("mail.mbox.file"));
            fw.close();
        } catch(Exception e) {
            System.out.println(e.toString());
        }
    }
}
