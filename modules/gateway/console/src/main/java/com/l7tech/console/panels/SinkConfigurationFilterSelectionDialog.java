package com.l7tech.console.panels;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.identity.finder.FindIdentitiesDialog;
import com.l7tech.console.panels.identity.finder.FindIdentitiesDialog.FindIdentities;
import com.l7tech.console.panels.identity.finder.Options;
import com.l7tech.console.panels.identity.finder.SearchType;
import com.l7tech.console.tree.servicesAndPolicies.OrganizationSelectionTree;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.transport.jms.JmsAdmin.JmsTuple;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TreeModelListenerAdapter;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;

import static com.l7tech.objectmodel.EntityType.JMS_ENDPOINT;
import static com.l7tech.objectmodel.EntityType.findTypeByEntity;

import com.l7tech.objectmodel.folder.FolderHeader;
import static com.l7tech.util.ClassUtils.cast;

import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.CollectionUtils;
import static com.l7tech.util.CollectionUtils.join;
import static com.l7tech.util.CollectionUtils.set;
import com.l7tech.util.Functions.Binary;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryThrows;
import static com.l7tech.util.Functions.grepFirst;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.Functions.reduce;
import static com.l7tech.util.InetAddressUtil.isValidIpv4Address;
import static com.l7tech.util.InetAddressUtil.isValidIpv6Address;
import com.l7tech.util.Option;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.optional;
import com.l7tech.util.TextUtils;
import com.l7tech.util.ValidationUtils;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

/**
 * Dialog for selection of logging filters
 */
public class SinkConfigurationFilterSelectionDialog extends JDialog {

    private static ResourceBundle resources = ResourceBundle.getBundle( SinkConfigurationFilterSelectionDialog.class.getName() );

    private JPanel mainPanel;
    private JComboBox keyCombobox;
    private OrganizationSelectionTree<OrganizationHeader> folderTree;
    private OrganizationSelectionTree<ServiceHeader> servicesTree;
    private OrganizationSelectionTree<PolicyHeader> policiesTree;
    private JComboBox transportTypeComboBox;
    private JTextField ipTextField;
    private JTextField packageTextField;
    private JButton cancelButton;
    private JButton addButton;
    private JPanel filterConfigPanel;
    private JList transportList;
    private JList categoryList;
    private JPanel identitySearchPanel;
    private FindIdentities findIdentities;

    private final FilterContext filterContext;
    private final Collection<FilterSelection> filterSelections = new ArrayList<FilterSelection>();

    public static final Set<EntityType> SUPPORTED_TRANSPORTS = set(
            EntityType.EMAIL_LISTENER,
            EntityType.JMS_CONNECTION,
            EntityType.SSG_CONNECTOR
    );

    public SinkConfigurationFilterSelectionDialog( final Window owner, final FilterContext filterContext ) {
        super( owner, resources.getString( "dialog.title" ), DEFAULT_MODALITY_TYPE );
        this.filterContext = filterContext;
        initialize();
    }

    public Collection<FilterSelection> getSelections() {
        return Collections.unmodifiableCollection( filterSelections );
    }

    private void initialize() {
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        setContentPane( mainPanel );
        Utilities.setEscKeyStrokeDisposes( this );

        keyCombobox.setModel( new DefaultComboBoxModel( FilterType.values() ) );
        keyCombobox.setRenderer( new Renderers.KeyedResourceRenderer( resources, "filterType.{0}.text" ) );
        keyCombobox.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged( final ItemEvent e) {
                final CardLayout layout = (CardLayout)filterConfigPanel.getLayout();
                final FilterType type =  (FilterType) e.getItem();
                initialize( type );
                layout.show( filterConfigPanel, type.toString() );
            }
        });
        keyCombobox.setSelectedIndex( 0 );

        ipTextField.setDocument( new MaxLengthDocument(128) );
        packageTextField.setDocument( new MaxLengthDocument(4096) );

        transportTypeComboBox.setModel( Utilities.comboBoxModel( SUPPORTED_TRANSPORTS ) );
        transportTypeComboBox.setRenderer( new Renderers.KeyedResourceRenderer( resources, "transport.{0}.text" ) );
        transportTypeComboBox.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                updateTransportList();
            }
        } );
        transportTypeComboBox.setSelectedIndex( 0 );

        categoryList.setModel( Utilities.listModel( SinkConfiguration.CATEGORIES_SET ) );
        categoryList.setCellRenderer( new Renderers.KeyedResourceRenderer( resources, "categories.{0}.text" ) );

        ((CardLayout)filterConfigPanel.getLayout()).show( filterConfigPanel, FilterType.CATEGORY.toString() );

        addButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                onOk();
            }
        } );
        cancelButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                dispose();
            }
        } );

        final RunOnChangeListener enableDisableLister = new RunOnChangeListener(){
            @Override
            protected void run() {
                enableDisableComponents();
            }
        };
        final TreeModelListener treeListener = new TreeModelListenerAdapter() {
            @Override
            public void treeNodesChanged( final TreeModelEvent e ) {
                enableDisableComponents();
            }
        };

        folderTree.getModel().addTreeModelListener( treeListener );
        servicesTree.getModel().addTreeModelListener( treeListener );
        policiesTree.getModel().addTreeModelListener( treeListener );

        keyCombobox.addItemListener( enableDisableLister );
        transportTypeComboBox.addItemListener( enableDisableLister );
        transportList.addListSelectionListener( enableDisableLister );
        categoryList.addListSelectionListener( enableDisableLister );
        ipTextField.getDocument().addDocumentListener( enableDisableLister );
        packageTextField.getDocument().addDocumentListener( enableDisableLister );

        enableDisableComponents();

        // init user panel now for layout
        initalizeUser();

        pack();
        Utilities.setMinimumSize( this );
        Utilities.centerOnParentWindow( this );
    }

    private void initialize( final FilterType type ) {
        switch ( type ) {
            case SERVICE:
                servicesTree.initTree(filterContext.getFolderHeaders(), new ArrayList<ServiceHeader>(filterContext.getServiceHeaders()));
                break;
            case FOLDER :
                ArrayList<OrganizationHeader> list =  new ArrayList<OrganizationHeader>();
                list.addAll(filterContext.getServiceHeaders());
                list.addAll(filterContext.getPolicyHeaders());
                folderTree.initTree(filterContext.getFolderHeaders(), list);
                break;
            case POLICY :
                policiesTree.initTree(filterContext.getFolderHeaders(),new ArrayList<PolicyHeader>(filterContext.getPolicyHeaders()));
                break;
        }
    }

    private void initalizeUser() {
        final Options options = new Options();
        options.setDisableOpenProperties( true );
        options.setEnableDeleteAction( false );
        options.setSearchType( SearchType.USER );
        findIdentities = FindIdentitiesDialog.getFindIdentities( options, filterContext.getIdentityProviderHeaders(), new Runnable(){
            @Override
            public void run() {
                enableDisableComponents();
            }
        } );

        identitySearchPanel.setLayout( new BorderLayout( 0, 4 ) );
        identitySearchPanel.add( findIdentities.getSearchDetailsPanel(), BorderLayout.NORTH );
        identitySearchPanel.add( findIdentities.getSearchResultsPanel(), BorderLayout.CENTER );
        identitySearchPanel.add( findIdentities.getSearchStatusComponent(), BorderLayout.SOUTH );
    }

    private void updateTransportList() {
        final EntityType type = (EntityType)transportTypeComboBox.getSelectedItem();
        final Collection<EntityHeader> headers;
        switch (type){
            case EMAIL_LISTENER:
                headers = filterContext.getEmailHeaders();
                break;
            case JMS_CONNECTION:
                headers = filterContext.getJmsHeaders();
                break;
            case SSG_CONNECTOR:
                headers = filterContext.getListenPortHeaders();
                break;
            default:
                headers = Collections.emptyList();
        }
        transportList.setModel( Utilities.listModel( headers ) );
    }

    private String toTypeId( final EntityType type ) {
        String typeId = null;
        switch (type){
            case EMAIL_LISTENER:
                typeId = GatewayDiagnosticContextKeys.EMAIL_LISTENER_ID;
                break;
            case JMS_CONNECTION:
                typeId = GatewayDiagnosticContextKeys.JMS_LISTENER_ID;
                break;
            case SSG_CONNECTOR:
                typeId = GatewayDiagnosticContextKeys.LISTEN_PORT_ID;
                break;
        }
        return typeId;
    }

    private void enableDisableComponents() {
        boolean enableAdd = false;

        final FilterType index = (FilterType) keyCombobox.getSelectedItem();
        switch (index){
            case CATEGORY:
                enableAdd = categoryList.getSelectedValues().length > 0;
                break;
            case FOLDER:
                enableAdd = !folderTree.getSelectedFolders().isEmpty();
                break;
            case SERVICE:
                enableAdd = !servicesTree.getSelectedEntities().isEmpty();
                break;
            case POLICY:
                enableAdd = !policiesTree.getSelectedEntities().isEmpty();
                break;
            case TRANSPORT:
                enableAdd = transportList.getSelectedValues().length>0 && transportTypeComboBox.getSelectedItem()!=null;
                break;
            case USER:
                enableAdd = findIdentities != null && !findIdentities.getSelections().isEmpty();
                break;
            case PACKAGE:
                enableAdd = !packageTextField.getText().trim().isEmpty();
                break;
            case IP :
                final String ipAddress = ipTextField.getText().trim();
                enableAdd = isValidIpv4Address( ipAddress ) || isValidIpv6Address( ipAddress );
                break;
        }

        addButton.setEnabled( enableAdd );
    }

    private void onOk() {
        final FilterType index = (FilterType) keyCombobox.getSelectedItem();
        switch (index){
            case FOLDER:
                filterSelections.addAll( map( folderTree.getSelectedFolders(), FilterContext.toFolderSelection() ) );
                break;
            case SERVICE:
                filterSelections.addAll( map( servicesTree.getSelectedEntities(), FilterContext.toServiceSelection() ) );
                break;
            case POLICY:
                filterSelections.addAll( map( policiesTree.getSelectedEntities(), FilterContext.toPolicySelection() ) );
                break;
            case TRANSPORT:
                filterSelections.addAll( map( map(
                        asList( transportList.getSelectedValues() ), cast( EntityHeader.class ) ),
                        FilterContext.toSelection(toTypeId( (EntityType) transportTypeComboBox.getSelectedItem() ) ) ) );
                break;
            case USER:
                if ( findIdentities != null ) filterSelections.addAll( map( findIdentities.getSelections(), filterContext.toUserSelection()) );
                break;
            case CATEGORY :
                filterSelections.addAll( join( map( asList( categoryList.getSelectedValues() ), new Unary<List<FilterSelection>, Object>() {
                    @Override
                    public List<FilterSelection> call( final Object value ) {
                        return toSelection( (String) value, "category" );
                    }
                } ) ) );
                break;
            case PACKAGE:
                filterSelections.addAll( toSelection(packageTextField.getText(), GatewayDiagnosticContextKeys.LOGGER_NAME ) );
                break;
            case IP :
                filterSelections.addAll( toSelection(ipTextField.getText(), GatewayDiagnosticContextKeys.CLIENT_IP ) );
                break;
        }

        dispose();
    }

    private List<FilterSelection> toSelection( final String value, final String typeId ) {
        return optional( value )
                .map( TextUtils.trim() )
                .filter( TextUtils.isNotEmpty() )
                .map( FilterContext.toSelectionFromValue( typeId ) )
                .toList();
    }

    private void createUIComponents() {
        folderTree = new OrganizationSelectionTree<OrganizationHeader>(false);
        servicesTree = new OrganizationSelectionTree<ServiceHeader>(true);
        policiesTree = new OrganizationSelectionTree<PolicyHeader>(true);
    }

    public static final class FilterContext {
        private Collection<EntityHeader> emailHeaders;
        private Collection<EntityHeader> listenPortHeaders;
        private Collection<EntityHeader> jmsHeaders;
        private Collection<EntityHeader> identityProviderHeaders;
        private Collection<FolderHeader> folderHeaders;
        private Collection<ServiceHeader> serviceHeaders;
        private Collection<PolicyHeader> policyHeaders;

        private final Unary<EntityHeader, NamedEntity> headerBuilder = new Unary<EntityHeader, NamedEntity>(){
            @Override
            public EntityHeader call( final NamedEntity namedEntity ) {
                return new EntityHeader( namedEntity.getId(), findTypeByEntity(namedEntity.getClass()), namedEntity.getName(), null );
            }
        };

        public Collection<EntityHeader> getEmailHeaders() {
            return emailHeaders = doAdmin( emailHeaders, new UnaryThrows<Collection<EntityHeader>, Registry, ObjectModelException>() {
                @Override
                public Collection<EntityHeader> call( final Registry registry ) throws ObjectModelException {
                    return unmodifiableCollection( map( registry.getEmailListenerAdmin().findAllEmailListeners(), headerBuilder ) );
                }
            } ).orSome( Collections.<EntityHeader>emptyList() );
        }

        public Collection<EntityHeader> getJmsHeaders() {
            return jmsHeaders = doAdmin( jmsHeaders, new UnaryThrows<Collection<EntityHeader>, Registry, ObjectModelException>() {
                @Override
                public Collection<EntityHeader> call( final Registry registry ) throws ObjectModelException {
                    return unmodifiableCollection( reduce(
                            asList( registry.getJmsManager().findAllTuples() ),
                            new ArrayList<EntityHeader>(),
                            new Binary<List<EntityHeader>, List<EntityHeader>, JmsTuple>() {
                                @Override
                                public List<EntityHeader> call( final List<EntityHeader> list, final JmsTuple jmsTuple ) {
                                    if ( jmsTuple.getEndpoint().isMessageSource() ) {
                                        list.add( new EntityHeader( jmsTuple.getEndpoint().getId(), JMS_ENDPOINT, jmsTuple.getEndpoint().getName(), null ) );
                                    }
                                    return list;
                                }
                            } ) );
                }
            } ).orSome( Collections.<EntityHeader>emptyList() );
        }

        public Collection<EntityHeader> getListenPortHeaders() {
            return listenPortHeaders = doAdmin( listenPortHeaders, new UnaryThrows<Collection<EntityHeader>, Registry, ObjectModelException>() {
                @Override
                public Collection<EntityHeader> call( final Registry registry ) throws ObjectModelException {
                    return unmodifiableCollection( map( registry.getTransportAdmin().findAllSsgConnectors(), headerBuilder ) );
                }
            } ).orSome( Collections.<EntityHeader>emptyList() );
        }

        public Collection<EntityHeader> getIdentityProviderHeaders() {
            return identityProviderHeaders = doAdmin( identityProviderHeaders, new UnaryThrows<Collection<EntityHeader>, Registry, ObjectModelException>() {
                @Override
                public Collection<EntityHeader> call( final Registry registry ) throws ObjectModelException {
                    return CollectionUtils.list( registry.getIdentityAdmin().findAllIdentityProviderConfig() );
                }
            } ).orSome( Collections.<EntityHeader>emptyList() );
        }

        public Collection<FolderHeader> getFolderHeaders() {
            return folderHeaders = doAdmin( folderHeaders, new UnaryThrows<Collection<FolderHeader>, Registry, ObjectModelException>() {
                @Override
                public Collection<FolderHeader> call( final Registry registry ) throws ObjectModelException {
                    return unmodifiableCollection( registry.getFolderAdmin().findAllFolders() );
                }
            } ).orSome( Collections.<FolderHeader>emptyList() );
        }

        public Collection<ServiceHeader> getServiceHeaders() {
            return serviceHeaders = doAdmin( serviceHeaders, new UnaryThrows<Collection<ServiceHeader>, Registry, ObjectModelException>() {
                @Override
                public Collection<ServiceHeader> call( final Registry registry ) throws ObjectModelException {
                    return CollectionUtils.list( registry.getServiceManager().findAllPublishedServices( false ) );
                }
            } ).orSome( Collections.<ServiceHeader>emptyList() );
        }

        public Collection<PolicyHeader> getPolicyHeaders() {
            return policyHeaders = doAdmin( policyHeaders, new UnaryThrows<Collection<PolicyHeader>, Registry, ObjectModelException>() {
                @Override
                public Collection<PolicyHeader> call( final Registry registry ) throws ObjectModelException {
                    return CollectionUtils.toList( registry.getPolicyAdmin().findPolicyHeadersWithTypes(EnumSet.of(PolicyType.INCLUDE_FRAGMENT, PolicyType.INTERNAL, PolicyType.GLOBAL_FRAGMENT))  );
                }
            } ).orSome( Collections.<PolicyHeader>emptyList() );
        }

        public <R> Option<R> doAdmin( @Nullable final R value,
                                      final UnaryThrows<R,Registry,ObjectModelException> adminCallback ) {
            Option<R> result = optional( value );
            final Registry registry = Registry.getDefault();
            if ( !result.isSome() && registry.isAdminContextPresent() ) {
                try {
                    result = optional(adminCallback.call( registry ));
                } catch ( PermissionDeniedException e ) {
                    // Display as unknown
                } catch ( ObjectModelException e ) {
                    ErrorManager.getDefault().notify( Level.WARNING, e, "Error listing entities for log filtering" );
                }
            }
            return result;
        }

        public Option<FilterSelection> resolveServiceFilter( final String value ) {
            return findHeader( getServiceHeaders(), value ).map( toServiceSelection() );
        }

        public Option<FilterSelection> resolvePolicyFilter( final String value ) {
            return findHeader( getPolicyHeaders(), value ).map( toPolicySelection() );
        }

        public Option<FilterSelection> resolveFolderFilter( final String value ) {
            return findHeader( getFolderHeaders(), value ).map( toFolderSelection() );
        }

        public Option<FilterSelection> resolveEmailTransportFilter( final String value ) {
            return findHeader( getEmailHeaders(), value ).map( toSelection( GatewayDiagnosticContextKeys.EMAIL_LISTENER_ID ) );
        }

        public Option<FilterSelection> resolveJmsTransportFilter( final String value ) {
            return findHeader( getJmsHeaders(), value ).map( toSelection( GatewayDiagnosticContextKeys.JMS_LISTENER_ID ) );
        }

        public Option<FilterSelection> resolveListenPortTransportFilter( final String value ) {
            return findHeader( getListenPortHeaders(), value ).map( toSelection( GatewayDiagnosticContextKeys.LISTEN_PORT_ID ) );
        }

        public Option<FilterSelection> resolveUserFilter( final String value ) {
            Option<FilterSelection> selection = none();
            final String[] id = value.split( ":", 2 );
            if ( id.length == 2 && ValidationUtils.isValidGoid( id[0], false)) {
                selection = doAdmin( null, new UnaryThrows<User,Registry,ObjectModelException>(){
                    @Override
                    public User call( final Registry registry ) throws ObjectModelException {
                        return registry.getIdentityAdmin().findUserByID( Goid.parseGoid( id[0] ), id[1] );
                    }
                } ).map( new Unary<FilterSelection, User>(){
                    @Override
                    public FilterSelection call( final User user ) {
                        return new FilterSelection(
                                GatewayDiagnosticContextKeys.USER_ID,
                                value,
                                getIdentityProviderName(user.getProviderId())+"/"+optional(user.getLogin()).orSome(id[1]) );
                    }
                } );
            }

            return selection;
        }

        private String getIdentityProviderName( final Goid providerId ) {
            return findHeader( getIdentityProviderHeaders(), Goid.toString( providerId ) )
                    .map( toName() )
                    .orSome( "Unknown Provider '" + providerId + "'" );
        }

        private static Unary<FilterSelection, OrganizationHeader> toOrganizationHeaderSelection( final String filterKey ) {
            return new Unary<FilterSelection, OrganizationHeader>(){
                @Override
                public FilterSelection call( final OrganizationHeader header ) {
                    return new FilterSelection( filterKey, header.getStrId(), header.getDisplayName() );
                }
            };
        }

        private static Unary<FilterSelection, OrganizationHeader> toServiceSelection() {
            return toOrganizationHeaderSelection( GatewayDiagnosticContextKeys.SERVICE_ID );
        }

        private static Unary<FilterSelection, OrganizationHeader> toPolicySelection() {
            return toOrganizationHeaderSelection( GatewayDiagnosticContextKeys.POLICY_ID );
        }

        private static Unary<FilterSelection, FolderHeader> toFolderSelection() {
            return new Unary<FilterSelection, FolderHeader>(){
                @Override
                public FilterSelection call( final FolderHeader folderHeader ) {
                    return new FilterSelection( GatewayDiagnosticContextKeys.FOLDER_ID, folderHeader.getStrId(), folderHeader.getPath() );
                }
            };
        }
        private Unary<FilterSelection, IdentityHeader> toUserSelection() {
            return new Unary<FilterSelection, IdentityHeader>(){
                @Override
                public FilterSelection call( final IdentityHeader header ) {
                    return new FilterSelection( GatewayDiagnosticContextKeys.USER_ID, header.getProviderGoid()+":"+header.getStrId(), getIdentityProviderName(header.getProviderGoid())+"/"+header.getName() );
                }
            };
        }

        private static Unary<FilterSelection, EntityHeader> toSelection( final String typeId ) {
            return new Unary<FilterSelection, EntityHeader>(){
                @Override
                public FilterSelection call( final EntityHeader header ) {
                    return new FilterSelection( typeId, header.getStrId(), header.getName() );
                }
            };
        }

        private static Unary<FilterSelection,String> toSelectionFromValue( final String typeId ) {
            return new Unary<FilterSelection, String>(){
                @Override
                public FilterSelection call( final String value ) {
                    return new FilterSelection( typeId, value, null );
                }
            };

        }

        private Unary<String, EntityHeader> toName() {
            return new Unary<String, EntityHeader>(){
                @Override
                public String call( final EntityHeader header ) {
                    return header.getName();
                }
            };
        }

        private <E extends EntityHeader> Option<E> findHeader( final Collection<E> headers, final String id ) {
            return optional( grepFirst( headers, new Unary<Boolean, E>() {
                @Override
                public Boolean call(  final E header ) {
                    return id.equals( header.getStrId() );
                }
            } ) );
        }
    }

    public static enum FilterType {
        CATEGORY,
        IP,
        FOLDER,
        PACKAGE,
        POLICY,
        SERVICE,
        TRANSPORT,
        USER,
    }

    public static final class FilterSelection {
        private final String typeId;
        private final String value;
        private final String displayValue;

        public FilterSelection( final String typeId,
                                final String value,
                                final String displayValue ) {
            this.typeId = typeId;
            this.value = value;
            this.displayValue = displayValue;
        }

        public String getTypeId() {
            return typeId;
        }

        public String getValue() {
            return value;
        }

        public String getDisplayValue() {
            return displayValue;
        }
    }
}
