package com.l7tech.proxy.gui;

import com.l7tech.common.gui.widgets.ContextMenuTextArea;
import com.l7tech.common.http.HttpHeaders;
import com.l7tech.common.message.HttpHeadersKnob;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.gui.policy.PolicyTreeCellRenderer;
import com.l7tech.proxy.gui.policy.PolicyTreeModel;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import org.w3c.dom.Document;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keep track of messages that have come and gone.
 *
 * User: mike
 * Date: May 22, 2003
 * Time: 5:03:25 PM
 */
class MessageViewerModel extends AbstractListModel implements RequestInterceptor {
    private static final Logger log = Logger.getLogger(MessageViewerModel.class.getName());
    private static final int maxMessages = 32;

    private List messages = new ArrayList(maxMessages);

    MessageViewerModel() {
        messages.add(new SavedTextMessage("Listening", ""));
    }

    /** Represents an intercept message we are keeping track of. */
    private static abstract class SavedMessage {
        private final static SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
        private String title;
        private Date when;

        SavedMessage(final String title) {
            this.title = title;
            this.when = new Date();
        }

        abstract Component getComponent(boolean reformat);

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

        Component getComponent(boolean reformat) {
            JTextArea ta = new ContextMenuTextArea(message);
            ta.setEditable(false);
            return ta;
        }
    }

    private static class SavedPolicyMessage extends SavedMessage {
        private ClientAssertion policy;
        private PolicyAttachmentKey key;

        SavedPolicyMessage(final String title, PolicyAttachmentKey key, ClientAssertion policy) {
            super(title);
            this.key = key;
            this.policy = policy;
        }

        Component getComponent(boolean reformat) {
            JPanel panel = new JPanel(new GridBagLayout());

            final JLabel namespaceLabel = new JLabel("Body Namespace:  ");
            panel.add(namespaceLabel,
                      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                             GridBagConstraints.EAST,
                                             GridBagConstraints.NONE,
                                             new Insets(5, 5, 3, 0), 0, 0));
            final JLabel namespace = new JLabel(key.getUri());
            panel.add(namespace,
                      new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                                             GridBagConstraints.WEST,
                                             GridBagConstraints.HORIZONTAL,
                                             new Insets(5, 0, 3, 5), 0, 0));
            final JLabel soapActionLabel = new JLabel("SOAPAction:  ");
            panel.add(soapActionLabel,
                      new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                             GridBagConstraints.EAST,
                                             GridBagConstraints.NONE,
                                             new Insets(0, 5, 5, 0), 0, 0));
            final JLabel soapAction = new JLabel(key.getSoapAction());
            panel.add(soapAction,
                      new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
                                             GridBagConstraints.WEST,
                                             GridBagConstraints.HORIZONTAL,
                                             new Insets(0, 0, 5, 5), 0, 0));
            final JLabel proxyUriLabel = new JLabel("Proxy URI:  ");
            panel.add(proxyUriLabel,
                      new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                                             GridBagConstraints.EAST,
                                             GridBagConstraints.NONE,
                                             new Insets(0, 5, 5, 0), 0, 0));
            final JLabel proxyUri = new JLabel(key.getProxyUri());
            panel.add(proxyUri,
                      new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
                                             GridBagConstraints.WEST,
                                             GridBagConstraints.HORIZONTAL,
                                             new Insets(0, 0, 5, 5), 0, 0));

            JTree policyTree = new JTree((TreeModel)null);
            policyTree.setCellRenderer(new PolicyTreeCellRenderer());
            policyTree.setModel(policy == null ? null : new PolicyTreeModel(policy));
            int erow = 0;
            while (erow < policyTree.getRowCount())
                policyTree.expandRow(erow++);            
            panel.add(policyTree,
                      new GridBagConstraints(0, 3, GridBagConstraints.REMAINDER, 1, 1.0, 1.0,
                                             GridBagConstraints.SOUTH,
                                             GridBagConstraints.BOTH,
                                             new Insets(0, 0, 0, 0), 0, 0));

            return panel;
        }
    }

    private static String headersToString(HttpHeaders headers) {
        return headers == null ? "" : headers.toExternalForm();
    }

    /**
     * Represents a message in SOAPEnvelope form.
     * Used for SOAPEnvelope messages to avoid keeping lots of textual copies of
     * possibly-large SOAP messages; instead, we just keep references to
     * the last N already-build SOAPEnvelope objects and render them as
     * ASCII on-demand.
     */
    private static class SavedXmlMessage extends SavedMessage {
        private String str = null;
        private boolean strWasFormatted = false;
        private String unparsed = null;
        private Document message = null;
        private HttpHeaders headers = null;

        SavedXmlMessage(final String title, final Document msg, HttpHeaders headers) {
            super(title);
            this.message = msg;
            this.headers = headers;
        }

        SavedXmlMessage(final String title, final String unparsed, HttpHeaders headers) {
            super(title);
            this.unparsed = unparsed;
            this.headers = headers;
        }

        String getMessageText(boolean reformat) {
            if (str != null && reformat == strWasFormatted)
                return str;
            if (message == null) {
                try {
                    message = XmlUtil.stringToDocument(unparsed);
                } catch (Exception e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                    return headersToString(headers) + "\n" + unparsed;
                }
            }
            try {
                if (reformat)
                    str = headersToString(headers) + "\n" + XmlUtil.nodeToFormattedString(message);
                else
                    str = headersToString(headers) + "\n" + XmlUtil.nodeToString(message);
            } catch (IOException e) {
                str = headersToString(headers) + "Unable to read message: " + e;
            }
            strWasFormatted = reformat;
            return str;
        }

        public Component getComponent(boolean reformat) {
            JTextArea ta = new ContextMenuTextArea(getMessageText(reformat));
            ta.setEditable(false);
            return ta;
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
    public Component getComponentAt(final int idx, boolean reformat) {
        return ((SavedMessage)messages.get(idx)).getComponent(reformat);
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
     * @param context
     */
    public void onReceiveMessage(PolicyApplicationContext context) {
        HttpHeadersKnob hhk = (HttpHeadersKnob)context.getRequest().getKnobAlways(HttpHeadersKnob.class);
        try {
            appendMessage(new SavedXmlMessage("From Client",
                                              context.getRequest().getXmlKnob().getOriginalDocument(),
                                              hhk.getHeaders()));
        } catch (Exception e) {
            final String msg = "Message Viewer unable to get request as XML Document: " + e.getMessage();
            log.log(Level.WARNING, msg, e);
            appendMessage(new SavedTextMessage("From Client", msg));
            return;
        }
    }

    /**
     * Fired when a reply is read from the SSG, after it is parsed.
     * Can be called from any thread.
     * @param context
     */
    public void onReceiveReply(PolicyApplicationContext context) {
        Document responseDoc = null;
        try {
            responseDoc = context.getResponse().getXmlKnob().getDocumentReadOnly();
        } catch (Exception e) {
            final String msg = "Message Viewer unable to get response as XML Document: " + e.getMessage();
            log.log(Level.WARNING, msg, e);
            appendMessage(new SavedTextMessage("From Server", msg));
            return;
        }
        HttpHeadersKnob hhk = (HttpHeadersKnob)context.getResponse().getKnobAlways(HttpHeadersKnob.class);
        appendMessage(new SavedXmlMessage("From Server", responseDoc, hhk.getHeaders()));
    }

    /**
     * Fired when an error is encountered while reading the message from a client.
     * @param t The error that occurred during the request.
     */
    public void onMessageError(final Throwable t) {
        try {
            final ByteArrayOutputStream b = new ByteArrayOutputStream(2048);
            PrintStream p = new PrintStream(b, true, "UTF-8");
            t.printStackTrace(p);
            p.flush();
            appendMessage(new SavedTextMessage("Client Error", b.toString("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            appendMessage(new SavedTextMessage("Client Error", t.getMessage()));
        }
    }

    /**
     * Fired when an error is encountered while obtaining a reply from the server.
     * @param t The error that occurred during the request.
     */
    public void onReplyError(final Throwable t) {
        try {
            final ByteArrayOutputStream b = new ByteArrayOutputStream(2048);
            PrintStream p = new PrintStream(b, true, "UTF-8");
            t.printStackTrace(p);
            p.flush();
            appendMessage(new SavedTextMessage("Server Error", b.toString("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            appendMessage(new SavedTextMessage("Server Error", t.getMessage()));
        }
    }

    /**
     * Fired when a policy is updated.
     * @param binding
     * @param policy
     */
    public void onPolicyUpdated(Ssg ssg, PolicyAttachmentKey binding, Policy policy) {
        ClientAssertion clientAssertion = null;
        try {
            clientAssertion = policy.getClientAssertion();
        } catch (PolicyAssertionException e) {
            // Fallthrough and use null
        }
        appendMessage(new SavedPolicyMessage("Policy updated", binding, clientAssertion));
    }

    /** Remove all saved messages from the list. */
    void clear() {
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

