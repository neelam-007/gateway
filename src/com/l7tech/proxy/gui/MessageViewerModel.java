package com.l7tech.proxy.gui;

import com.l7tech.proxy.RequestInterceptor;
import org.apache.axis.message.SOAPEnvelope;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Date;

/**
 * Keep track of messages that have come and gone.
 * User: mike
 * Date: May 22, 2003
 * Time: 5:03:25 PM
 * To change this template use Options | File Templates.
 */
public class MessageViewerModel extends AbstractListModel implements RequestInterceptor {
    private final int maxMessages = 32;
    private ArrayList messages = new ArrayList(maxMessages);

    /** Represents a SOAP message we are keeping track of. */
    private static class SavedMessage {
        public String title;
        public SOAPEnvelope soapEnvelope;
        public Date when;

        SavedMessage(String title, SOAPEnvelope msg) {
            this.title = title;
            this.soapEnvelope = msg;
            this.when = new Date();
        }

        public String toString() {
            return when + ": " + title;
        }
    }

    /** Throw away all but the last maxMessages saved messages. */
    private void cutoff() {
        while (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }

    /**
     * Get the content of message #idx as an XML string.
     * @param idx
     * @return
     */
    public String getXmlAt(int idx) {
        return ((SavedMessage)messages.get(idx)).soapEnvelope.toString();
    }

    /**
     * Fired when a message is received from a client, after it is parsed.
     * Can be called from any thread.
     * @param message
     */
    public void onReceiveMessage(final SOAPEnvelope message) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                messages.add(new SavedMessage("From Client", message));
                cutoff();
                fireContentsChanged(this, 0, getSize());
            }
        });
    }

    /**
     * Fired when a reply is read from the SSG, after it is parsed.
     * Can be called from any thread.
     * @param reply
     */
    public void onReceiveReply(final SOAPEnvelope reply) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                messages.add(new SavedMessage("From Server", reply));
                cutoff();
                fireContentsChanged(this, 0, getSize());
            }
        });
    }

    /** Remove all saved messages from the list. */
    public void clear() {
        int oldsize = messages.size();
        messages.clear();
        fireContentsChanged(this, 0, oldsize);
    }

    /**
     * Returns the length of the list.
     * @return the length of the list
     */
    public int getSize() {
        return messages.size();
    }

    /**
     * Returns the value at the specified index.
     * @param index the requested index
     * @return the value at <code>index</code>
     */
    public Object getElementAt(int index) {
        return messages.get(index);
    }
}
