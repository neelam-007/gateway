package com.l7tech.client.gui;

import com.l7tech.gui.widgets.ContextMenuTextArea;
import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.HttpHeaders;
import com.l7tech.util.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.HttpHeadersKnob;
import com.l7tech.message.Message;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.client.gui.policy.PolicyTreeCellRenderer;
import com.l7tech.client.gui.policy.PolicyTreeModel;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.awt.*;
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
 */
class MessageViewerModel extends AbstractListModel implements RequestInterceptor {
    private static final String PER_MESSAGE_STORAGE_SIZE = "com.l7tech.client.gui.perMessageStorageSize";
    private static final int MAX_PER_MESSAGE_STORAGE_SIZE = 500 * 1024; //500kb

    private static final Logger log = Logger.getLogger(MessageViewerModel.class.getName());
    private static final int MAX_MESSAGES = 64;
    private static final char RIGHTWARD_ARROW = '\u2192';
    private static final char LEFTWARD_ARROW = '\u2190';
    private static final String TO_SERVER = RIGHTWARD_ARROW + " To Server";
    private static final String FROM_SERVER = LEFTWARD_ARROW + " From Server";
    private static final String POLICY_UPDATED = LEFTWARD_ARROW + " Policy updated";
    private static final String POLICY_DOWNLOAD_ERROR = LEFTWARD_ARROW + " Policy download error";
    private static final String SERVER_ERROR = LEFTWARD_ARROW + " Server Error";

    private List<SavedMessage> messages = new ArrayList<SavedMessage>(MAX_MESSAGES);
    private boolean recordFromClient = false;
    private boolean recordToServer = false;
    private boolean recordFromServer = false;
    private boolean recordToClient = false;
    private boolean recordPolicies = true;
    private boolean recordErrors = true;
    private int perMessageStorageSize = 0;

    MessageViewerModel() {
        //initialize message viewer
        perMessageStorageSize = SyspropUtil.getInteger(PER_MESSAGE_STORAGE_SIZE, MAX_PER_MESSAGE_STORAGE_SIZE);
        if (perMessageStorageSize <= 0 || perMessageStorageSize > MAX_PER_MESSAGE_STORAGE_SIZE) {
            perMessageStorageSize = MAX_PER_MESSAGE_STORAGE_SIZE;
        }

        messages.add(new SavedTextMessage("Session started      ", "", perMessageStorageSize));
    }

    /** Represents an intercept message we are keeping track of. */
    private static abstract class SavedMessage {
        private final static SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
        private final String title;
        private final HttpHeaders headers;
        private final Date when;
        private final int truncateSize;
        private boolean messageTruncated;

        SavedMessage(final String title, final HttpHeaders headers, int truncateSize) {
            this.title = title;
            this.headers = headers;
            this.when = new Date();
            this.truncateSize = truncateSize;
            this.messageTruncated = false;
        }

        SavedMessage(final String title, int truncateSize) {
            this(title, null, truncateSize);
        }

        abstract Component getComponent(boolean reformat);

        /**
         * Truncates the message if the message size limit has been reached.
         * Generally, we'll truncate to the message size limit and append "..." to denote the truncated parts.
         *
         * @param message   The message to be processed for truncation
         */
        abstract void processMessageTruncation(String message);

        public String toString() {
            return dateFormat.format(when) + ": " + title;
        }

        protected String headersToString() {
            return headers == null ? "" : (headers.toExternalForm() + '\n');
        }

        public int getTruncateSize() {
            return truncateSize;
        }

        public boolean isMessageTruncated() {
            return messageTruncated;
        }

        public void setMessageTruncated(boolean messageTruncated) {
            this.messageTruncated = messageTruncated;
        }
    }

    /** Represents a message in text form. */
    private static class SavedTextMessage extends SavedMessage {
        private String message;

        SavedTextMessage(final String title, final String message, int truncateSize) {
            this(title, message, null, truncateSize);
        }

        SavedTextMessage(final String title, final String message, final HttpHeaders headers, int truncateSize) {
            super(title, headers, truncateSize);
            processMessageTruncation(message);
        }

        Component getComponent(boolean reformat) {
            JTextArea ta = new ContextMenuTextArea(headersToString() + message);
            ta.setEditable(false);
            return ta;
        }

        void processMessageTruncation(String message) {
            int messageSize = 0;
            try {                              
                messageSize = message == null ? 0 : message.getBytes("UTF-8").length * 2;
                if (messageSize  > getTruncateSize()) {
                    setMessageTruncated(true);
                    log.info("Message was truncated");
                    this.message = (new String(message.getBytes("UTF-8"), 0, getTruncateSize(), "UTF-8")) + "\n   [Message truncated...]" ;
                } else {
                    this.message = message;
                }
            } catch (UnsupportedEncodingException uee) {
                this.message = "Unable truncated message.  Message size '" + messageSize + "' bytes too large to store: " + uee;
            } catch (IndexOutOfBoundsException iobe) {
                this.message = "Unable truncated message.  Message size '" + messageSize + "' bytes too large to store: " + iobe;
            }
        }
    }

    private static class SavedPolicyMessage extends SavedMessage {
        private final ClientAssertion policy;
        private final PolicyAttachmentKey key;

        SavedPolicyMessage(final String title, PolicyAttachmentKey key, ClientAssertion policy) {
            super(title, 0);
            this.key = key;
            this.policy = policy;
        }

        Component getComponent(boolean reformat) {
            JPanel panel = makePakPanel(key);

            JTree policyTree = new JTree((TreeModel)null);
            policyTree.setCellRenderer(new PolicyTreeCellRenderer());
            policyTree.setModel(policy == null ? null : new PolicyTreeModel(policy));
            int erow = 0;
            while (erow < policyTree.getRowCount()) {
                policyTree.expandRow(erow);
                erow++;
            }
            panel.add(policyTree,
                      new GridBagConstraints(0, 3, GridBagConstraints.REMAINDER, 1, 1.0, 1.0,
                                             GridBagConstraints.SOUTH,
                                             GridBagConstraints.BOTH,
                                             new Insets(0, 0, 0, 0), 0, 0));

            return panel;
        }

        void processMessageTruncation(String message) {
            //no message to be truncated
        }
    }

    private static JPanel makePakPanel(PolicyAttachmentKey key) {
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
        return panel;
    }

    private static class PolicyErrorMessage extends SavedMessage {
        private final PolicyAttachmentKey key;
        private final String err;

        PolicyErrorMessage(String title, PolicyAttachmentKey key, String err) {
            super(title, 0);
            this.key = key;
            this.err = err != null ? err : "No extra information available";
        }

        Component getComponent(boolean reformat) {
            JPanel panel = makePakPanel(key);

            JTextArea ta = new ContextMenuTextArea(err);
            ta.setEditable(false);
            panel.add(ta,
                      new GridBagConstraints(0, 3, GridBagConstraints.REMAINDER, 1, 1.0, 1.0,
                                             GridBagConstraints.SOUTH,
                                             GridBagConstraints.BOTH,
                                             new Insets(0, 0, 0, 0), 0, 0));
            return panel;
        }

        void processMessageTruncation(String message) {
            //not sure if we really want to truncate error messages, so we'll leave it as is for now
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
        private String str = null;
        private boolean strWasFormatted = false;
        private String unformatted = null;
        private String formatted = null;
        //private Document message = null;

        @Deprecated
        SavedXmlMessage(final String title, final Document msg, HttpHeaders headers, int truncateSize) {
            super(title, headers, truncateSize);

        }

        SavedXmlMessage(final String title, final String unparsed, HttpHeaders headers, int truncateSize) {
            super(title, headers, truncateSize);
            processMessageTruncation(unparsed);
        }

        String getMessageText(boolean reformat) {
            if (str != null && reformat == strWasFormatted)
                return str;

            if (reformat) {
                str = headersToString() + formatted;
            } else {
                str = headersToString() + unformatted;
            }

            strWasFormatted = reformat;
            return str;
        }

        public Component getComponent(boolean reformat) {
            JTextArea ta = new ContextMenuTextArea(getMessageText(reformat));
            ta.setEditable(false);
            return ta;
        }

        void processMessageTruncation(String message) {
            int messageSize = 0;
            boolean isTruncated = false;
            Document document = null;
            try {
                document = XmlUtil.stringToDocument(message);
                StringBuffer buf = new StringBuffer(XmlUtil.nodeToString(document));

                //process the unformated message
                messageSize = buf.toString().getBytes("UTF-8").length * 2;
                if (messageSize > getTruncateSize()) {
                    log.info("Message was truncated");
                    setMessageTruncated(true);
                    this.unformatted = (new String(buf.toString().getBytes("UTF-8"), 0, getTruncateSize(), "UTF-8")) + "\n   [Message truncated...]" ;
                    isTruncated = true;
                } else {
                    this.unformatted = buf.toString();
                }

                //process the formated message
                buf = new StringBuffer(XmlUtil.nodeToFormattedString(document));
                messageSize = buf.toString().getBytes("UTF-8").length * 2;
                if (messageSize > getTruncateSize()) {
                    if (!isMessageTruncated()) {
                        log.info("Message was truncated");
                    }

                    setMessageTruncated(true);
                    this.formatted = (new String(buf.toString().getBytes("UTF-8"), 0, getTruncateSize(), "UTF-8")) + "\n   [Message truncated...]" ;
                    isTruncated = true;
                } else {
                    this.formatted = buf.toString();
                }
            } catch (UnsupportedEncodingException see) {
                this.formatted = "Unable truncated message.  Message size '" + messageSize + "' bytes too large to store: " + see;
                this.unformatted = "Unable truncated message.  Message size '" + messageSize + "' bytes too large to store: " + see;
            } catch (SAXException se) {
                this.formatted = "Unable truncated message.  Message size '" + messageSize + "' bytes too large to store: " + se;
                this.unformatted = "Unable truncated message.  Message size '" + messageSize + "' bytes too large to store: " + se;
            } catch (IOException ioe) {
                this.formatted = "Unable truncated message.  Message size '" + messageSize + "' bytes too large to store: " + ioe;
                this.unformatted = "Unable truncated message.  Message size '" + messageSize + "' bytes too large to store: " + ioe;
            }
        }
    }

    /** Throw away all but the last maxMessages saved messages. */
    private void cutoff() {
        while (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
    }

    /**
     * Get the content of message #idx as an XML string.
     * @param idx message index. Required.
     * @param reformat if true, XML should be reformatted (if relevant)
     * @return the component for that message.  Never null.
     */
    public Component getComponentAt(final int idx, boolean reformat) {
        return messages.get(idx).getComponent(reformat);
    }

    /**
     * Get the message value to determine whether the message was truncated.
     *
     * @param idx   The index of the messsage.
     * @return  TRUE if the original message was truncated, otherwise FALSE.
     */
    public boolean isMessageTruncated(final int idx) {
        return messages.get(idx).isMessageTruncated();
    }

    /**
     * Add a simple custom text message to the GUI viewer.
     * Can be called from any thread.
     *
     * @param title  the title of the message.  Required.
     * @param body   the body of the message.  May be empty but must not be null.
     */
    public void addMessage(String title, String body) {
        appendMessage(new SavedTextMessage(title, body, perMessageStorageSize));
    }

    /**
     * Add a message to the end of our list.
     * Can be called from any thread.
     * @param message the message to add.  Required.
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

    // Can be called from any thread
    public void onFrontEndRequest(PolicyApplicationContext context) {
        if (!isRecordFromClient()) return;
        HttpHeadersKnob hhk = (HttpHeadersKnob)context.getRequest().getKnobAlways(HttpHeadersKnob.class);
        try {
            appendMessage(new SavedXmlMessage("From Client",
                    XmlUtil.nodeToString(context.getRequest().getXmlKnob().getOriginalDocument()),
                    hhk.getHeaders(), perMessageStorageSize));
        } catch (Exception e) {
            final String msg = "Message Viewer unable to get request as XML Document: " + e.getMessage();
            log.log(Level.WARNING, msg, e);
            appendMessage(new SavedTextMessage("From Client", msg, perMessageStorageSize));
        }
    }

    // Can be called from any thread
    public void onFrontEndReply(PolicyApplicationContext context) {
        if (!isRecordToClient()) return;
        try {
            final Message response = context.getResponse();
            final HttpHeadersKnob hhk = (HttpHeadersKnob)response.getKnobAlways(HttpHeadersKnob.class);
            final HttpHeaders headers = hhk.getHeaders();
            if (!response.isXml()) {
                appendMessage(new SavedTextMessage("To Client",
                        "<Non-XML response of type " + response.getMimeKnob().getOuterContentType().getMainValue() + '>',
                        headers, perMessageStorageSize));
                return;
            }
            final Document responseDoc = response.getXmlKnob().getDocumentReadOnly();
            appendMessage(new SavedXmlMessage("To Client", XmlUtil.nodeToString(responseDoc), headers, perMessageStorageSize));
        } catch (Exception e) {
            final String msg = "Message Viewer unable to get response as XML Document: " + e.getMessage();
            log.log(Level.WARNING, msg, e);
            appendMessage(new SavedTextMessage("To Client", msg, perMessageStorageSize));
        }
    }

    // Can be called from any thread
    public void onBackEndRequest(PolicyApplicationContext context, List<HttpHeader> headersSent) {
        if (!isRecordToServer()) return;
        try {
            GenericHttpHeaders headers = new GenericHttpHeaders(headersSent.toArray(new HttpHeader[headersSent.size()]));
            appendMessage(new SavedXmlMessage(TO_SERVER,
                                              XmlUtil.nodeToString(context.getRequest().getXmlKnob().getDocumentReadOnly()),
                                              headers, perMessageStorageSize));
        } catch (Exception e) {
            final String msg = "Message Viewer unable to get request as XML Document: " + e.getMessage();
            log.log(Level.WARNING, msg, e);
            appendMessage(new SavedTextMessage(TO_SERVER, msg, perMessageStorageSize));
        }
    }

    // Can be called from any thread
    public void onBackEndReply(PolicyApplicationContext context) {
        if (!isRecordFromServer()) return;
        try {
            final Message response = context.getResponse();
            final HttpHeadersKnob hhk = (HttpHeadersKnob)response.getKnobAlways(HttpHeadersKnob.class);
            final HttpHeaders headers = hhk.getHeaders();
            if (!response.isXml()) {
                appendMessage(new SavedTextMessage(FROM_SERVER,
                        "<Non-XML response of type " + response.getMimeKnob().getOuterContentType().getMainValue() + '>',
                        headers, perMessageStorageSize));
                return;
            }
            Document responseDoc = response.getXmlKnob().getDocumentReadOnly();
            //responseDoc = XmlUtil.stringToDocument(XmlUtil.nodeToString(responseDoc));
            appendMessage(new SavedXmlMessage(FROM_SERVER, XmlUtil.nodeToString(responseDoc), headers, perMessageStorageSize));
        } catch (Exception e) {
            final String msg = "Message Viewer unable to get response as XML Document: " + e.getMessage();
            log.log(Level.WARNING, msg, e);
            appendMessage(new SavedTextMessage(FROM_SERVER, msg, perMessageStorageSize));
        }
    }

    /**
     * Fired when an error is encountered while reading the message from a client.
     * @param t The error that occurred during the request.
     */
    public void onMessageError(final Throwable t) {
        if (!isRecordErrors()) return;
        final BufferPoolByteArrayOutputStream b = new BufferPoolByteArrayOutputStream(2048);
        PrintStream p = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            p = new PrintStream(b, true, "UTF-8");
            t.printStackTrace(p);
            p.flush();
            appendMessage(new SavedTextMessage("Client Error", b.toString("UTF-8"), perMessageStorageSize));
        } catch (UnsupportedEncodingException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            appendMessage(new SavedTextMessage("Client Error", t.getMessage(), perMessageStorageSize));
        } finally {
            ResourceUtils.closeQuietly(p);
            ResourceUtils.closeQuietly(b);
        }
    }

    /**
     * Fired when an error is encountered while obtaining a reply from the server.
     * @param t The error that occurred during the request.
     */
    public void onReplyError(final Throwable t) {
        if (!isRecordErrors()) return;
        final BufferPoolByteArrayOutputStream b = new BufferPoolByteArrayOutputStream(2048);
        PrintStream p = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            p = new PrintStream(b, true, "UTF-8");
            t.printStackTrace(p);
            p.flush();            
            appendMessage(new SavedTextMessage(SERVER_ERROR, t.getMessage(), perMessageStorageSize));
        } catch (UnsupportedEncodingException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            appendMessage(new SavedTextMessage(SERVER_ERROR, t.getMessage(), perMessageStorageSize));
        } finally {
            ResourceUtils.closeQuietly(p);
            ResourceUtils.closeQuietly(b);
        }
    }

    // can be called from any thread
    public void onPolicyUpdated(Ssg ssg, PolicyAttachmentKey binding, Policy policy) {
        if (!isRecordPolicies()) return;
        ClientAssertion clientAssertion = null;
        try {
            clientAssertion = policy.getClientAssertion();
        } catch (PolicyAssertionException e) {
            // Fallthrough and use null
        }
        appendMessage(new SavedPolicyMessage(POLICY_UPDATED, binding, clientAssertion));
    }

    // can be called from any thread
    public void onPolicyError(Ssg ssg, PolicyAttachmentKey binding, Throwable error) {
        if (!isRecordErrors()) return;
        final BufferPoolByteArrayOutputStream b = new BufferPoolByteArrayOutputStream(2048);
        PrintStream p = null;
        try {
            //noinspection IOResourceOpenedButNotSafelyClosed
            p = new PrintStream(b, true, "UTF-8");
            error.printStackTrace(p);
            p.flush();
            String mess = b.toString("UTF-8");
            appendMessage(new PolicyErrorMessage(POLICY_DOWNLOAD_ERROR, binding, mess));
        } catch (UnsupportedEncodingException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            appendMessage(new SavedTextMessage(POLICY_DOWNLOAD_ERROR, error == null ? "null" : error.getMessage(), perMessageStorageSize));
        } finally {
            ResourceUtils.closeQuietly(p);
            ResourceUtils.closeQuietly(b);
        }
    }


    public boolean isRecordFromClient() {
        return recordFromClient;
    }

    public synchronized void setRecordFromClient(boolean recordFromClient) {
        this.recordFromClient = recordFromClient;
    }

    public synchronized boolean isRecordToServer() {
        return recordToServer;
    }

    public synchronized void setRecordToServer(boolean recordToServer) {
        this.recordToServer = recordToServer;
    }

    public synchronized boolean isRecordFromServer() {
        return recordFromServer;
    }

    public synchronized void setRecordFromServer(boolean recordFromServer) {
        this.recordFromServer = recordFromServer;
    }

    public synchronized boolean isRecordToClient() {
        return recordToClient;
    }

    public synchronized void setRecordToClient(boolean recordToClient) {
        this.recordToClient = recordToClient;
    }

    public synchronized boolean isRecordPolicies() {
        return recordPolicies;
    }

    public synchronized void setRecordPolicies(boolean recordPolicies) {
        this.recordPolicies = recordPolicies;
    }

    public synchronized boolean isRecordErrors() {
        return recordErrors;
    }

    public synchronized void setRecordErrors(boolean recordErrors) {
        this.recordErrors = recordErrors;
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

