package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.service.PublishedService;
import com.l7tech.service.Wsdl;
import com.l7tech.console.util.Registry;

import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Arrays;
import java.io.StringReader;


/**
 * The class represents a node element in the TreeModel.
 * It represents the published service.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class ServiceNode extends EntityHeaderNode {
    PublishedService svc;
    /**
     * construct the <CODE>ServiceNode</CODE> instance for
     * a given entity header.
     *
     * @param e  the EntityHeader instance, must represent published service
     * @exception IllegalArgumentException
     *                   thrown if unexpected type
     */
    public ServiceNode(EntityHeader e)
      throws IllegalArgumentException {
        super(e);
    }


    /**
       * Returns true if the receiver is a leaf.
       *
       * @return true if leaf, false otherwise
       */
      public boolean isLeaf() {
          return false;
      }

      /**
       * Returns the children of the reciever as an Enumeration.
       *
       * @return the Enumeration of the child nodes.
       * @exception Exception thrown when an erro is encountered when
       *                      retrieving child nodes.
       */
      public Enumeration children() throws Exception {
          if (svc == null) {
              svc = Registry.getDefault().getServiceManager().findByPrimaryKey(getEntityHeader().getOid());
          }

          if (svc != null) {
              Wsdl wsdl = Wsdl.newInstance(null, new StringReader(svc.getWsdlXml()));
              TreeNode node = WsdlTreeNode.newInstance(wsdl);
              node.getChildCount();
              return node.children();
          }

          return Collections.enumeration(Collections.EMPTY_LIST);
      }

      /**
       * Returns true if the receiver allows children.
       */
      public boolean getAllowsChildren() {
          return true;
      }

}
