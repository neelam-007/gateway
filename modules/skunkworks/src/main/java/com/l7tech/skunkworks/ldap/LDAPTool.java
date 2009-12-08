package com.l7tech.skunkworks.ldap;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.util.Pair;
import com.l7tech.util.Functions;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.server.identity.ldap.LdapUtils;

import javax.swing.*;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.NamingEnumeration;
import javax.naming.SizeLimitExceededException;
import javax.naming.NamingException;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

/**
 * 
 */
@SuppressWarnings({ "unchecked" })
public class LDAPTool extends JFrame {
    private JPanel mainPanel;
    private JTextField server;
    private JTextField baseDN;
    private JTextField bindDN;
    private JPasswordField bindPassword;
    private JTextField searchFilter;
    private JButton searchButton;
    private JTable searchResultsTable;
    private JTextField fetchDN;
    private JButton fetchButton;
    private JTable fetchTable;
    private SimpleTableModel<Pair> searchResultsTableModel;
    private SimpleTableModel<Pair> fetchTableModel;

    public static void main( final String[] args ) {
        LDAPTool tool = new LDAPTool();
        tool.pack();
        Utilities.centerOnScreen( tool );
        tool.setVisible( true );
    }

    public LDAPTool() {
        super( "LDAP Tool v0.1" );
        initComponents();
        initServer();
    }

    private void initComponents() {
        setLayout( new BorderLayout() );
        add( mainPanel, BorderLayout.CENTER );

        searchResultsTableModel = TableUtil.configureTable(
                searchResultsTable,
                TableUtil.column("DN", 40, 80, 10000, Functions.propertyTransform(Pair.class, "key")),
                TableUtil.column("Objectclass", 40, 80, 10000, Functions.propertyTransform(Pair.class, "value"))
        );
        searchResultsTable.setModel( searchResultsTableModel );
        searchResultsTable.getTableHeader().setReorderingAllowed( false );
        searchResultsTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        Utilities.setRowSorter( searchResultsTable, searchResultsTableModel );

        fetchTableModel = TableUtil.configureTable(
                fetchTable,
                TableUtil.column("Name", 40, 80, 10000, Functions.propertyTransform(Pair.class, "key")),
                TableUtil.column("Value", 40, 80, 10000, Functions.propertyTransform(Pair.class, "value"))
        );
        fetchTable.setModel( fetchTableModel );
        fetchTable.getTableHeader().setReorderingAllowed( false );
        fetchTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        Utilities.setRowSorter( fetchTable, fetchTableModel );

        searchButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doSearch();
            }
        } );

        fetchButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doFetch();
            }
        } );
    }

    private void initServer() {
//        server.setText("qaad.l7tech.com");
//        baseDN.setText("OU=QA Test Users,DC=qaWin2003,DC=com");
//        bindDN.setText("browse");

        server.setText("10.7.41.11");
        baseDN.setText("dc=perftest,dc=com ");
        bindDN.setText("cn=Manager,dc=perftest,dc=com ");
    }

    private void doSearch() {
        DirContext context = null;
        NamingEnumeration<SearchResult> results = null;
        try {
            context = getLdapContext();

            final SearchControls controls = new SearchControls();
            controls.setCountLimit( 100 );
            controls.setReturningAttributes( new String[]{ "objectclass" } );

            results = context.search( baseDN.getText(), searchFilter.getText(), controls );

            final java.util.List<Pair> resultList = new ArrayList<Pair>();
            try {
                while ( results.hasMore() ) {
                    SearchResult result = results.next();
                    resultList.add( new Pair( result.getNameInNamespace(), result.getAttributes().get("objectclass")) );
                }
            } catch ( SizeLimitExceededException slee ) {
                System.out.println( slee.getMessage() );
            }
            searchResultsTableModel.setRows( resultList );
        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            ResourceUtils.closeQuietly( results );
            ResourceUtils.closeQuietly( context );
        }
    }

    private void doFetch() {
        DirContext context = null;
        NamingEnumeration<? extends Attribute> attributeEnum = null;
        try {
            context = getLdapContext();

            final Attributes attributes = context.getAttributes( fetchDN.getText() );

            final java.util.List<Pair> resultList = new ArrayList<Pair>();
            attributeEnum = attributes.getAll();
            while ( attributeEnum.hasMore() ) {
                Attribute attribute = attributeEnum.next();
                NamingEnumeration<?> values = null;
                try {
                    values = attribute.getAll();
                    int index = 0;
                    while ( values.hasMore() ) {
                        Object value = values.next();
                        if ( value instanceof byte[] ) {
                            value = HexUtils.encodeBase64( (byte[]) value );                           
                        }
                        resultList.add( new Pair( attribute.getID() + "[" + (index++) + "]", value ) );
                    }
                } finally {
                    ResourceUtils.closeQuietly( values );
                }
            }
            fetchTableModel.setRows( resultList );
        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            ResourceUtils.closeQuietly( attributeEnum );
            ResourceUtils.closeQuietly( context );
        }
    }

    private DirContext getLdapContext() throws NamingException {
        return LdapUtils.getLdapContext(
                "ldap://" + server.getText(),
                bindDN.getText(),
                new String(bindPassword.getPassword()),
                30000,
                60000);
    }
}
