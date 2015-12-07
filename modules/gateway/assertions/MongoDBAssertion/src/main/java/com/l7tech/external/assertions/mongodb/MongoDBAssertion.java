package com.l7tech.external.assertions.mongodb;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.mongodb.console.MongoDBConnectionsDialog;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntityAdmin;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntityAdminImpl;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.search.Dependency;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME_FACTORY;

/**
 * 
 */
public class MongoDBAssertion extends Assertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(MongoDBAssertion.class.getName());

    private Goid connectionGoid;
    private String collectionName;
    private String operation = "FIND";
    private String writeConcern = "ACKNOWLEDGED";
    private String queryDocument;
    private String projectionDocument;
    private String updateDocument;
    private boolean failIfNoResults = false;
    private boolean enableMulti = true;
    private boolean enableUpsert = false;
    private String prefix = "mongoDBQuery";

    //
    // Metadata
    //
    private static final String META_INITIALIZED = MongoDBAssertion.class.getName() + ".metadataInitialized";

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getQueryDocument() {
        return queryDocument;
    }

    public void setQueryDocument(String queryDocument) {
        this.queryDocument = queryDocument;
    }

    public String getUpdateDocument() {
        return updateDocument;
    }

    public void setUpdateDocument(String updateDocument) {
        this.updateDocument = updateDocument;
    }

    // migrate & dependency
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Dependency(type = Dependency.DependencyType.GENERIC, methodReturnType = Dependency.MethodReturnType.GOID)
    public Goid getConnectionGoid() {
        return connectionGoid;
    }

    public void setConnectionGoid(Goid connectionGoid) {
        this.connectionGoid = connectionGoid;
    }

    public boolean isFailIfNoResults() {
        return failIfNoResults;
    }

    public void setFailIfNoResults(boolean failIfNoResults) {
        this.failIfNoResults = failIfNoResults;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getProjectionDocument() {
        return projectionDocument;
    }

    public void setProjectionDocument(String projectionDocument) {
        this.projectionDocument = projectionDocument;
    }

    public boolean isEnableMulti() {
        return enableMulti;
    }

    public void setEnableMulti(boolean enableMulti) {
        this.enableMulti = enableMulti;
    }

    public boolean isEnableUpsert() {
        return enableUpsert;
    }

    public void setEnableUpsert(boolean enableUpsert) {
        this.enableUpsert = enableUpsert;
    }

    public String getWriteConcern() {
        return writeConcern;
    }

    public void setWriteConcern(String writeConcern) {
        this.writeConcern = writeConcern;
    }

    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(queryDocument, updateDocument, projectionDocument);
    }

    @Override
       public VariableMetadata[] getVariablesSet() {
           List<VariableMetadata> varMeta = new ArrayList<VariableMetadata>();
           varMeta.add(new VariableMetadata(prefix + ".queryresult.count", false, false, null, false, DataType.INTEGER));
            varMeta.add(new VariableMetadata(prefix + ".queryresult.output", false, false, null, false, DataType.STRING));

           return varMeta.toArray(new VariableMetadata[varMeta.size()]);
       }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/PerformJdbcQuery16x16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/PerformJdbcQuery16x16.gif");

        meta.put(AssertionMetadata.EXTENSION_INTERFACES_FACTORY, new Functions.Unary<Collection<ExtensionInterfaceBinding>, ApplicationContext>() {
               @Override
               public Collection<ExtensionInterfaceBinding> call(ApplicationContext appContext) {
                   return  MongoDBConnectionEntityManagerSupport.getInstance(appContext).getExtensionInterfaceBindings();
               }
           });


        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.mongodb.MongoDBAssertionModuleLoadListener");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.mongodb.console.MongoDBAssertionPropertiesDialog");

        meta.put(AssertionMetadata.GLOBAL_ACTION_CLASSNAMES, new String[] { getClass().getName() + "$CustomAction" });

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:MongoDB" rather than "set:modularAssertions"
        //meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private final static String baseName = "Perform MongoDB Operation";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<MongoDBAssertion>(){
            @Override
            public String getAssertionName( final MongoDBAssertion assertion, final boolean decorate) {
                if(!decorate) return baseName;

                StringBuilder builder= new StringBuilder(baseName);
                builder.append(": ").append(assertion.getOperation());

                return builder.toString();
            }
        };

    public static class MongoDBConnectionEntityManagerSupport {
           private static MongoDBConnectionEntityManagerSupport instance;
           private EntityManager<MongoDBConnectionEntity, GenericEntityHeader> entityManager;

           public static synchronized MongoDBConnectionEntityManagerSupport getInstance(final ApplicationContext context) {
               if(instance == null){
                   MongoDBConnectionEntityManagerSupport support = new MongoDBConnectionEntityManagerSupport();
                   support.init(context);
                   instance = support;
               }
               return instance;
           }

           public void init(final ApplicationContext _context) {
               GenericEntityManager gem = _context.getBean("genericEntityManager", GenericEntityManager.class);
               gem.registerClass(MongoDBConnectionEntity.class);
               entityManager = gem.getEntityManager(MongoDBConnectionEntity.class);
           }

           public Collection<ExtensionInterfaceBinding> getExtensionInterfaceBindings() {
               ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<MongoDBConnectionEntityAdmin>(MongoDBConnectionEntityAdmin.class, null, MongoDBConnectionEntityAdminImpl.getInstance(entityManager));
               return Collections.singleton(binding);
           }
       }

    public static class CustomAction extends AbstractAction {
            public CustomAction() {
                super("Configure MongoDB Connection", ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/Bean16.gif"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                MongoDBConnectionsDialog mongoDBConnectionsDialog = new MongoDBConnectionsDialog(TopComponents.getInstance().getTopParent());
                Utilities.centerOnScreen(mongoDBConnectionsDialog);
                DialogDisplayer.display(mongoDBConnectionsDialog);
            }
       }

}
