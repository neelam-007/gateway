package com.l7tech.console.xmlviewer;

import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.console.xmlviewer.properties.ViewerProperties;
import com.l7tech.xml.xpath.XpathUtil;
import org.dom4j.Element;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Map;

/**
 * Insert comments here.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version $Revision$, $Date$
 */
public class ViewerToolBar extends JToolBar {
    Viewer viewer;
    ViewerProperties properties;
    private JTextField xpathField = null;
    private CollapseAllAction collapse = null;
    private ExpandAllAction expand = null;
    private Map<String,String> namespaces;

    public ViewerToolBar(ViewerProperties props, Viewer v) {
        viewer = v;
        properties = props;

        setFloatable(false);
        expand = new ExpandAllAction(v);
        collapse = new CollapseAllAction(v);

        xpathField = new SquigglyTextField();

        JLabel xpathLabel = new JLabel("XPath:");
        xpathLabel.setForeground(new Color(102, 102, 102));

        JPanel xpathPanel = new JPanel(new BorderLayout(5, 0));
        xpathPanel.setBorder(new EmptyBorder(2, 5, 2, 2));
        JPanel xpanel = new JPanel(new BorderLayout());
        xpanel.setBorder(new EmptyBorder(1, 0, 1, 0));

        xpathField.setFont(xpathField.getFont().deriveFont(Font.PLAIN, 12));
        xpathField.setPreferredSize(new Dimension(100, 19));
        xpathField.setEditable(true);

        xpanel.add(xpathField, BorderLayout.CENTER);
        xpathPanel.add(xpathLabel, BorderLayout.WEST);
        xpathPanel.add(xpanel, BorderLayout.CENTER);

        add(xpathPanel, BorderLayout.CENTER);

        add(expand);
        add(collapse);
        addSeparator();

        viewer.tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                final TreePath path = viewer.tree.getSelectionPath();
                if ( path != null ) {
                    XmlElementNode node = (XmlElementNode)path.getLastPathComponent();
                    ExchangerElement element = node.getElement();

                    if ( element != null ) {
                        final StringBuilder builder = new StringBuilder();
                        buildPath(builder, namespaces, node.getElement());
                        xpathField.setText( builder.toString() );
                    } else {
                        xpathField.setText( null );
                    }
                }
            }
        });
    }

    /**
     * Access the xpath combo box component. This accesses
     *
     * @return the xpath combo box
     */
    public JTextField getxpathField() {
        return xpathField;
    }

    /**
     * Sets whether or not this component controls are enabled.
     *
     * @param enabled true if this component controls should be enabled, false otherwise
     */
    public void setToolbarEnabled(boolean enabled) {
        xpathField.setEnabled(enabled);
        collapse.setEnabled(enabled);
        expand.setEnabled(enabled);
    }

    /**
     * @return the currently selected xpath or <b>null</b> if none selected
     */
    public String getXPath() {
        return xpathField.getText();
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces( final Map<String, String> namespaces ) {
        this.namespaces = namespaces;
    }

    private void buildPath( final StringBuilder builder,
                            final Map<String, String> namespaces,
                            final Element element ) {

        if ( element.getParent() != null ) {
            buildPath( builder, namespaces, element.getParent() );
        }

        builder.append( '/' );

        String prefix = element.getNamespacePrefix();
        String namespace = element.getNamespaceURI();

        if ( namespace == null ) {
            builder.append( element.getName() );
        } else if ( prefix == null || !namespace.equals(namespaces.get(prefix)) ) {
            if ( namespaces.containsValue( namespace )) {
                builder.append( findPrefix(namespaces, namespace) );
                builder.append( ':' );
                builder.append( element.getName() );
            } else {
                builder.append( "*[local-name()='" );
                builder.append( element.getName() ); // name cannot contain "'"
                builder.append( "' and namespace-uri()=" );
                builder.append( XpathUtil.literalExpression(namespace) );
                builder.append( ']' );
            }
        } else {
            builder.append( element.getQualifiedName() );
        }
    }

    private String findPrefix( final Map<String,String> namespaces, final String namespace ) {
        String prefix = null;

        for ( final Map.Entry<String,String> entry : namespaces.entrySet() ) {
            if ( namespace.equals(entry.getValue()) ) {
                prefix = entry.getKey();
                break;
            }
        }

        return prefix;
    }    
}
