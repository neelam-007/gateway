package com.l7tech.proxy.gui;

import com.l7tech.proxy.RequestInterceptor;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.log4j.Category;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;

import javax.swing.*;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Keep track of messages that have come and gone.
 * User: mike
 * Date: May 22, 2003
 * Time: 5:03:25 PM
 */
public class MessageViewerModel extends AbstractListModel implements RequestInterceptor {
    private static final Category log = Category.getInstance(MessageViewerModel.class.getName());
    private static final int maxMessages = 32;

    private List messages = new ArrayList(maxMessages);

    /** Represents an intercept message we are keeping track of. */
    private static abstract class SavedMessage {
        private final static SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
        private String title;
        private  Date when;

        SavedMessage(final String title) {
            this.title = title;
            this.when = new Date();
        }

        abstract public String getMessageText();

        public String toString() {
            return dateFormat.format(when) + ": " + title;
        }
    }

    /** Represents a message in text form. */
    private static class SavedTextMessage extends SavedMessage {
        private String message;

        SavedTextMessage(final String title, final String message) {
            super(title);
            this.message = message;
        }

        public String getMessageText() {
            return message;
        }
    }

    /**
     * Represents a message in SOAPEnvelope form.
     * Used for SOAPEnvelope messages to avoid keeping lots of textual copies of
     * possibly-large SOAP messages; instead, we just keep references to
     * the last N already-build SOAPEnvelope objects and render them as
     * ASCII on-demand.
     */
    private static class SavedXmlMessage extends SavedMessage {
        private static final XMLSerializer xmlSerializer = new XMLSerializer();
        private SOAPEnvelope soapEnvelope;

        static {
            // Set up the output format for the xmlSerializer
            OutputFormat outputFormat = new OutputFormat();
            outputFormat.setLineWidth(80);
            outputFormat.setIndent(2);
            outputFormat.setIndenting(true);
            xmlSerializer.setOutputFormat(outputFormat);
        }

        SavedXmlMessage(final String title, final SOAPEnvelope msg) {
            super(title);
            this.soapEnvelope = msg;
        }

        public String getMessageText() {
            final StringWriter sw = new StringWriter();
            xmlSerializer.setOutputCharStream(sw);
            try {
                xmlSerializer.serialize(soapEnvelope.getAsDOM());
            } catch (Exception e) {
                log.error(e);
                return "(Internal error)";
            }
            return sw.toString();
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
    public String getMessageTextAt(final int idx) {
        return ((SavedMessage)messages.get(idx)).getMessageText();
    }

    /**
     * Add a message to the end of our list.
     * Can be called from any thread.
     * @param message
     */
    private void appendMessage(final SavedMessage message) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final int oldSize = messages.size();
                messages.add(message);
                cutoff();
                final int newSize = messages.size();
                if (newSize > oldSize) {
                    fireIntervalAdded(this, oldSize, newSize);
                } else {
                    fireContentsChanged(this, 0, newSize);
                }
            }
        });
    }

    /**
     * Fired when a message is received from a client, after it is parsed.
     * Can be called from any thread.
     * @param message
     */
    public void onReceiveMessage(final SOAPEnvelope message) {
        appendMessage(new SavedXmlMessage("From Client", message));
    }

    /**
     * Fired when a reply is read from the SSG, after it is parsed.
     * Can be called from any thread.
     * @param reply
     */
    public void onReceiveReply(final SOAPEnvelope reply) {
        appendMessage(new SavedXmlMessage("From Server", reply));
    }

    /**
     * Fired when an error is encountered while reading the message from a client.
     * @param t The error that occurred during the request.
     */
    public void onMessageError(final Throwable t) {
        appendMessage(new SavedTextMessage("Client Error", t.getMessage()));
    }

    /**
     * Fired when an error is encountered while obtaining a reply from the server.
     * @param t The error that occurred during the request.
     */
    public void onReplyError(final Throwable t) {
        appendMessage(new SavedTextMessage("Server Error", t.getMessage()));
    }

    /** Remove all saved messages from the list. */
    public void clear() {
        final int oldsize = messages.size();
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
    public Object getElementAt(final int index) {
        return messages.get(index);
    }
}

