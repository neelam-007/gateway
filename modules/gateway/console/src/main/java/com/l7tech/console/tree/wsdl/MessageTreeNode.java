package com.l7tech.console.tree.wsdl;

import javax.wsdl.Message;
import javax.wsdl.Part;
import java.util.Iterator;
import java.util.Map;

/**
 * Class MessageTreeNode.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class MessageTreeNode extends WsdlTreeNode {
    private Message message;

    public MessageTreeNode(Message m) {
        this(m, new Options());
    }

    /**
     * full constructor
     * 
     * @param m       the message
     * @param options the wsdl rendering options
     */
    public MessageTreeNode(Message m, Options options) {
        super(null, options);
        this.message = m;
    }

    /**
     * load the message parts if specifid in the options
     */
    protected void loadChildren() {
        if (wsdlOptions.isShowMessageParts()) {
            int index = 0;
            children = null;
            Map parts = message.getParts();
            for (Iterator i = parts.keySet().iterator(); i.hasNext();) {
                Object key = i.next();
                insert(new MessagePartTreeNode((Part)parts.get(key), wsdlOptions), index++);
            }
        }
    }

    /**
     * get the message this node represents
     *
     * @return the corresponding message
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Returns true if the receiver is a leaf
     */
    public boolean isLeaf() {
        return !wsdlOptions.isShowMessageParts();
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return wsdlOptions.isShowMessageParts();
    }


    /**
     * subclasses override this method specifying the resource name
     * 
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/SendMail16.gif";
    }


    /**
     * @return a string representation of the object.
     */
    public String toString() {
        return message.getQName().getLocalPart();
    }
}

