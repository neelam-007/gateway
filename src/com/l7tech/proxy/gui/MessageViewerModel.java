package com.l7tech.proxy.gui;

import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.console.tree.EntityTreeCellRenderer;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import org.apache.log4j.Category;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.awt.*;

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
        private Date when;

        SavedMessage(final String title) {
            this.title = title;
            this.when = new Date();
        }

        abstract public Component getComponent();

        public String toString() {
            return dateFormat.format(when) + ": " + title;
        }
    }

    /** Represents a message in text form. */
    private static class SavedTextMessage extends SavedMessage {
        protected String message;

        SavedTextMessage(final String title, final String message) {
            super(title);
            this.message = message;
        }

        public Component getComponent() {
            return new JTextArea(message);
        }
    }

    private static class SavedPolicyMessage extends SavedMessage {
        private Assertion policy;
        private PolicyAttachmentKey key;

        SavedPolicyMessage(final String title, PolicyAttachmentKey key, Assertion policy) {
            super(title);
            this.key = key;
            this.policy = policy;
        }

        public Component getComponent() {
            JTree policyTree = new JTree((TreeModel)null);
            policyTree.setCellRenderer(new EntityTreeCellRenderer());
            policyTree.setModel(policy == null ? null : new PolicyTreeModel(policy));
            return policyTree;
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
        private Document soapEnvelope;

        static {
            // Set up the output format for the xmlSerializer
            OutputFormat outputFormat = new OutputFormat();
            outputFormat.setLineWidth(80);
            outputFormat.setIndent(2);
            outputFormat.setIndenting(true);
            xmlSerializer.setOutputFormat(outputFormat);
        }

        SavedXmlMessage(final String title, final Document msg) {
            super(title);
            this.soapEnvelope = msg;
        }

        public String getMessageText() {
            final StringWriter sw = new StringWriter();
            xmlSerializer.setOutputCharStream(sw);
            try {
                xmlSerializer.serialize(soapEnvelope);
            } catch (Exception e) {
                log.error(e);
                return "(Internal error)";
            }
            return sw.toString();
        }

        public Component getComponent() {
            return new JTextArea(getMessageText());
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
    public Component getComponentAt(final int idx) {
        return ((SavedMessage)messages.get(idx)).getComponent();
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
    public void onReceiveMessage(final Document message) {
        appendMessage(new SavedXmlMessage("From Client", message));
    }

    /**
     * Fired when a reply is read from the SSG, after it is parsed.
     * Can be called from any thread.
     * @param reply
     */
    public void onReceiveReply(final SsgResponse reply) {
        appendMessage(new SavedTextMessage("From Server", reply.toString()));
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

    /**
     * Fired when a policy is updated.
     * @param binding
     * @param policy
     */
    public void onPolicyUpdated(Ssg ssg, PolicyAttachmentKey binding, Assertion policy) {
        appendMessage(new SavedPolicyMessage("Policy updated", binding, policy));
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

