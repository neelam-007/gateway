package com.l7tech.server;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.BeanUtils;
import com.l7tech.util.Functions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import static com.l7tech.util.CollectionUtils.set;

/**
 * Basic test for spring application context.
 *
 * @author Steve Jones
 */
public class ApplicationContextTest  {

    private static final String[] CONTEXTS = {
        "com/l7tech/server/resources/adminContext.xml",
        "com/l7tech/server/resources/dataAccessContext.xml",
        "com/l7tech/server/resources/standardDbContext.xml",
        "com/l7tech/server/resources/ssgApplicationContext.xml" ,
        "com/l7tech/server/resources/admin-servlet.xml"
    };

    private static final Set<String> NON_ADMIN_PROXY_BEANS = Collections.emptySet();
    private static final Set<String> NON_ADMIN_BEANS = set( "logAccessAdmin", "clusterLogAccessAdmin" );
    private static final Set<String> EXTRA_ADMIN_BEANS = set( "adminLogin", "customAssertionRegistrar" );
    private static final Set<String> NON_SECURED_BEANS = set( "customAssertionRegistrar" );
    private static final Set<String> TRANSACTIONAL_GETTER_BLACKLIST = set( "auditAdmin", "serviceAdmin", "trustedCertAdmin", "emailListenerAdmin", "clusterStatusAdmin", "jdbcAdmin");
    private static final Set<String> TRANSACTION_ROLLBACK_WHITELIST = set( "adminLogin", "clusterIDManager", "counterManager", "distributedMessageIdManager", "ftpAdmin", "kerberosAdmin", "schemaEntryManager");
    private static final Set<String> SECURED_RETURNTYPE_WHITELIST = set(
            "com.l7tech.gateway.common.audit.AuditAdmin.getDigestsForAuditRecords",
            "com.l7tech.gateway.common.audit.AuditAdmin.downloadAllAudits",
            "com.l7tech.gateway.common.audit.AuditAdmin.downloadNextChunk",
            "com.l7tech.gateway.common.resources.ResourceAdmin.getDefaultHttpProxyConfiguration",
            "com.l7tech.gateway.common.jdbc.JdbcAdmin.testJdbcConnection",
            "com.l7tech.gateway.common.cluster.ClusterStatusAdmin.isCluster",
            "com.l7tech.gateway.common.admin.UDDIRegistryAdmin.testUDDIRegistryAuthentication"
            );
    private static final Set<EntityType> IGNORE_ENTITY_TYPES = set(
        EntityType.ESM_ENTERPRISE_FOLDER,  
        EntityType.ESM_SSG_CLUSTER,
        EntityType.ESM_SSG_NODE,
        EntityType.ESM_STANDARD_REPORT,
        EntityType.ESM_MIGRATION_RECORD,
        EntityType.ESM_NOTIFICATION_RULE,
        EntityType.ESM_LOG,
        EntityType.VALUE_REFERENCE);
    private static final Set<String> IGNORED_APPLICATION_LISTENER_BEANS = set( "adminAuditListener", "applicationEventProxy", "cryptoInit", "gatewayState", "keystoreMutatorSwitch", "messageProcessingAuditListener", "systemAuditListener", "jdbcQueryingManager" );

    /**
     * Loading the definitions in this way will check the syntax and that all the
     * bean classes exist.
     */
    @Test
    public void testApplicationContextSyntaxAndBeanClasses() {
        // 
        DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(dlbf);

        for (String context : CONTEXTS) {
            xbdr.loadBeanDefinitions(new ClassPathResource(context));
        }
    }

    /**
     * Ensure that bean references are valid (constructor args and properties)
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testApplicationContextReferences() {
        //
        DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(dlbf);

        for (String context : CONTEXTS) {
            xbdr.loadBeanDefinitions(new ClassPathResource(context));
        }

        for (String beanId : dlbf.getBeanDefinitionNames()) {
            BeanDefinition bd = dlbf.getBeanDefinition(beanId);

            final String className = bd.getBeanClassName();
            Class beanClass = null;
            if ( className != null ) {
                try {
                    final Class<?> clazz = Class.forName( className );
                    if ( !FactoryBean.class.isAssignableFrom( clazz ) && className.startsWith( "com.l7tech" ) ) {
                        beanClass = clazz;
                    }
                } catch ( ClassNotFoundException e ) {
                    Assert.fail("Class not found for bean '"+beanId+"': " + className);
                }
            }

            ConstructorArgumentValues cav = bd.getConstructorArgumentValues();
            if (cav != null) {
                List<ConstructorArgumentValues.ValueHolder> constructorArgs = new ArrayList<ConstructorArgumentValues.ValueHolder>();
                constructorArgs.addAll( cav.getGenericArgumentValues() );
                constructorArgs.addAll( cav.getIndexedArgumentValues().values() );
                for (ConstructorArgumentValues.ValueHolder holder : constructorArgs) {
                    Object value = holder.getValue();
                    if (value instanceof RuntimeBeanReference) {
                        RuntimeBeanReference rbr = (RuntimeBeanReference) value;
                        String name = rbr.getBeanName();
                        Assert.assertTrue("Application context uses bean with name '"+name+"', but no such bean exists (referenced from bean '"+beanId+"' as constructor-arg) ",
                                   dlbf.containsBean(name));
                    }
                }

                checkBeanConstructor( beanId, bd, cav, constructorArgs );
            }

            MutablePropertyValues mpv = bd.getPropertyValues();
            if (mpv != null) {
                final Set<PropertyDescriptor> descriptors = beanClass==null ?
                        Collections.<PropertyDescriptor>emptySet() :
                        BeanUtils.getProperties( beanClass, false, true );

                for (PropertyValue propValue : mpv.getPropertyValues()) {
                    Object value = propValue.getValue();
                    if (value instanceof RuntimeBeanReference) {
                        RuntimeBeanReference rbr = (RuntimeBeanReference) value;
                        String name = rbr.getBeanName();
                        Assert.assertTrue("Application context uses bean with name '"+name+"', but no such bean exists (referenced from bean '"+beanId+"' as property) ",
                                   dlbf.containsBean(name));
                    }

                    if ( beanClass != null ) {
                        boolean foundProperty = false;
                        for ( final PropertyDescriptor descriptor : descriptors ) {
                            if ( descriptor.getName().equals( propValue.getName() ) ) {
                                foundProperty = true;
                                break;
                            }
                        }

                        Assert.assertTrue( "Property '"+propValue.getName()+"' for bean '"+beanId+"' does not exist", foundProperty );
                    }
                }
            }
        }
    }

    public static void checkBeanConstructor( final String beanId,
                                             final BeanDefinition bd,
                                             final ConstructorArgumentValues cav,
                                             final List<ConstructorArgumentValues.ValueHolder> constructorArgs ) {
        // Verify that the constructor arguments match the class (at least to some extent)
        Class beanClass = null;
        try {
            if ( bd.getBeanClassName() != null ) {
                beanClass = Class.forName( bd.getBeanClassName() );
            }
        } catch ( ClassNotFoundException e ) {
            Assert.fail( "Class not found '" + bd.getBeanClassName() + "' for bean '" + beanId + "'" );
        }
        if ( beanClass != null && !FactoryBean.class.isInstance( beanClass ) ) {
            int constructorArgumentCount = constructorArgs.size();
            if ( !cav.getIndexedArgumentValues().isEmpty() ) {
                int maxIndex = Functions.reduce( cav.getIndexedArgumentValues().keySet(), 0, new Functions.Binary<Integer, Integer, Integer>() {
                    @Override
                    public Integer call( final Integer integer, final Integer integer1 ) {
                        return Math.max( integer, integer1 );
                    }
                } );
                if ( maxIndex >= constructorArgumentCount ) {
                    constructorArgumentCount = maxIndex + 1;
                }
            }
            boolean foundConstructor = false;
            for ( final Constructor constructor : beanClass.getDeclaredConstructors() ) {
                if ( constructor.getParameterTypes().length >= constructorArgumentCount ) {
                    foundConstructor = true;
                    break;
                }
            }
            Assert.assertTrue( "Application context bean '"+beanId+"' has no constructor with "+constructorArgumentCount+" parameters", constructorArgumentCount==0 || foundConstructor );
        }
    }

    @Test
    public void testTransactionRollback() throws Exception {
        DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(dlbf);

        for ( String context : CONTEXTS ) {
            xbdr.loadBeanDefinitions(new ClassPathResource(context));
        }

        // Test admin beans
        List<String> adminBeans = getAdminBeanIds( dlbf );
        for ( String adminBeanId : adminBeans ) {
            BeanDefinition beanDef = dlbf.getBeanDefinition( adminBeanId );
            Class<?> adminClass = Class.forName(beanDef.getBeanClassName());

            for ( Class interfaceClass : adminClass.getInterfaces() ) {
                if ( interfaceClass.getName().endsWith("Admin") ) {
                    enforceTransactionRollbackForClass(interfaceClass, adminBeanId);
                }
            }
        }

        // Test manager beans
        for ( String beanId : dlbf.getBeanDefinitionNames() ) {
            if ( beanId.endsWith("Manager") || beanId.startsWith("entity") || beanId.equals("auditExporter") ) {
                BeanDefinition beanDef = dlbf.getBeanDefinition( beanId );
                Class<?> beanClass = Class.forName(beanDef.getBeanClassName());
                if ( HibernateDaoSupport.class.isAssignableFrom(beanClass) ) {
                    enforceTransactionRollbackForClass(beanClass, beanId);
                }
            }
        }
    }

    private void enforceTransactionRollbackForClass( final Class<?> beanClass, final String beanId ) throws ClassNotFoundException {
        Transactional classTransactional = beanClass.getAnnotation(Transactional.class);
        if ( (classTransactional == null || classTransactional.rollbackFor().length==0) &&
             !TRANSACTION_ROLLBACK_WHITELIST.contains(beanId)) {
            Assert.fail( "Entity manager does not rollback transaction on checked exceptions '" + beanId  + "' (add rollbackFor or add to TRANSACTION_ROLLBACK_WHITELIST)." );
        }
    }

    /**
     * Test that all HTTP exported admin interfaces are marked as administrative
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testAdministrativeExports() throws Exception {
        //
        DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(dlbf);

        for ( String context : CONTEXTS ) {
            xbdr.loadBeanDefinitions(new ClassPathResource(context));
        }

        int testedcount = 0;
        for ( String beanId : dlbf.getBeanDefinitionNames() ) {
            if ( beanId.startsWith("/") && !NON_ADMIN_PROXY_BEANS.contains(beanId) ) {
                BeanDefinition beanDef = dlbf.getBeanDefinition( beanId );
                if ( "httpInvokerParent".equals(beanDef.getParentName()) ) {
                    testedcount++;
                    PropertyValues props = beanDef.getPropertyValues();
                    Class serviceInterface = Class.forName(((TypedStringValue)props.getPropertyValue("serviceInterface").getValue()).getValue());
                    if ( serviceInterface.getAnnotation(Administrative.class) == null ) {
                        Assert.fail( "Administrative HTTP exported bean '"+beanId+"' service interface '"+serviceInterface.getName()+"', is not annotated as '"+Administrative.class.getName()+"'." );
                    }
                }
            }
        }

        if (testedcount==0) Assert.fail("Failed to find any http exported admin beans.");
    }

    /**
     * Using the administrative annotation on an instance is incorrect, it should only be used on an interface
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testNoAdministrativeImplementations() throws Exception {
        checkForImplementationAnnotation( Administrative.class );
    }

    /*
     * Ensure that all Administration bean interfaces in the application context are annotated with @Administrative
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testAdministrativeAutoProxy() throws Exception {
        //
        DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        List<String> beans = getAdminBeanIds( dlbf );

        for ( String beanId : beans ) {
            System.out.println("Checking bean '"+beanId+"'.");
            BeanDefinition beanDef = dlbf.getBeanDefinition( beanId );
            String className = beanDef.getBeanClassName();
            Class adminClass = Class.forName(className);

            if ( adminClass.getAnnotation(Administrative.class) != null ) {
                Assert.fail( "Administrative bean '"+beanId+"', annotated with '"+Administrative.class.getName()+"' annotation, this should be on the interface." );
            }

            boolean admin = false;
            boolean secured = false;
            for ( Class interfaceClass : adminClass.getInterfaces() ) {
                if ( interfaceClass.getAnnotation(Secured.class) != null ) {
                    secured = true;
                }
                if ( interfaceClass.getAnnotation(Administrative.class) != null ) {
                    admin = true;
                }
            }

            if ( !admin ) {
                Assert.fail( "Administrative bean '"+beanId+"', has no interface with the '"+Administrative.class.getName()+"' annotation." );
            }

            if ( !secured && !NON_SECURED_BEANS.contains(beanId) ) {
                Assert.fail( "Administrative bean '"+beanId+"', has no interface with the '"+Secured.class.getName()+"' annotation." );
            }
        }
    }

    /*
    * Ensure that all Secured entity/entity getters return filterable return types
    */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testAdministrativeSecuredReturnTypes() throws Exception {
        DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        List<String> beans = getAdminBeanIds( dlbf );

        for ( String beanId : beans ) {

            System.out.println("Checking bean '"+beanId+"'.");
            BeanDefinition beanDef = dlbf.getBeanDefinition(beanId);
            String className = beanDef.getBeanClassName();
            Class adminClass = Class.forName(className);

            Class adminInterface = null;
            for ( Class interfaceClass : adminClass.getInterfaces() ) {
                if ( interfaceClass.getAnnotation(Administrative.class) != null ) {
                    adminInterface = interfaceClass;
                    break;
                }
            }

            if ( adminInterface != null ) {
                Secured classSecuredAnn = (Secured) adminInterface.getAnnotation( Secured.class );
                if ( classSecuredAnn != null ) {
                    for ( Method method : adminInterface.getMethods() ) {
                        Secured securedAnn = method.getAnnotation( Secured.class );
                        if ( securedAnn!=null ) {
                            if(SECURED_RETURNTYPE_WHITELIST.contains(adminInterface.getName()+"."+method.getName()))
                                continue;
                            else if((securedAnn.stereotype()== MethodStereotype.FIND_ENTITIES ||
                                     securedAnn.stereotype()== MethodStereotype.FIND_HEADERS||
                                    securedAnn.stereotype()== MethodStereotype.FIND_ENTITY )
                                && securedAnn.customEntityTranslatorClassName().isEmpty()){
                                Class returnType = method.getReturnType();
                                if(EntityHeaderSet.class.isAssignableFrom(returnType)){
                                    continue;
                                }else if(Iterable.class.isAssignableFrom(returnType)){
                                    continue;
                                }else if (Entity.class.isAssignableFrom(returnType)||
                                          EntityHeader.class.isAssignableFrom(returnType)){
                                    continue;
                                }else if (returnType.isArray() && (
                                                  Entity.class.isAssignableFrom(returnType.getComponentType()) ||
                                                  EntityHeader.class.isAssignableFrom(returnType.getComponentType()))){
                                    continue;
                                }else{
                                   Assert.fail( "Secured method '"+adminInterface.getName()+"."+method.getName()+"' does not return a filter-able type" );
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void testAdminGettersReadonlyTransaction() throws Exception {
        DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        List<String> beans = getAdminBeanIds( dlbf );

        for ( String beanId : beans ) {
            if ( TRANSACTIONAL_GETTER_BLACKLIST.contains(beanId) ) {
                // listed beans either perform db updates in the getter (such as auditing)
                // or just have not been checked to see if the readOnly option can be added
                System.out.println("Not checking bean '"+beanId+"'.");
                continue;
            }
            System.out.println("Checking bean '"+beanId+"'.");
            BeanDefinition beanDef = dlbf.getBeanDefinition( beanId );
            String className = beanDef.getBeanClassName();
            Class adminClass = Class.forName(className);

            Class adminInterface = null;
            for ( Class interfaceClass : adminClass.getInterfaces() ) {
                if ( interfaceClass.getAnnotation(Administrative.class) != null ) {
                    adminInterface = interfaceClass;
                    break;
                }
            }

            if ( adminInterface != null ) {
                Transactional classTransAnn = (Transactional) adminInterface.getAnnotation( Transactional.class );
                if ( classTransAnn != null ) {
                    boolean classReadonly = classTransAnn.readOnly() || classTransAnn.propagation()==Propagation.SUPPORTS;

                    for ( Method method : adminInterface.getMethods() ) {
                        if ( method.getName().startsWith("get") ) {
                            Transactional transAnn = method.getAnnotation( Transactional.class );
                            if ( (transAnn!=null && !transAnn.readOnly() && transAnn.propagation()!=Propagation.SUPPORTS) || (transAnn==null && !classReadonly) ) {
                                Assert.fail( "Administration bean '"+beanId+"' method '"+method.getName()+"', is a getter so should be Transactional(readOnly=true)." );
                            }
                        }
                    }
                }
            }
        }
    }

    private List<String> getAdminBeanIds( final DefaultListableBeanFactory dlbf ) {
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(dlbf);

        for ( String context : CONTEXTS ) {
            xbdr.loadBeanDefinitions(new ClassPathResource(context));
        }

        List<String> beans = new ArrayList<String>();
        beans.addAll(EXTRA_ADMIN_BEANS);

        for ( String beanId : dlbf.getBeanDefinitionNames() ) {
            if ( beanId.endsWith("Admin") && !beanId.startsWith("/") && !NON_ADMIN_BEANS.contains(beanId) ) {
                beans.add(beanId);
            }
        }

        return beans;
    }

    /**
     * Using the secured annotation on an instance is incorrect, it only works on an interface
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testNoSecuredImplementations() throws Exception {
        checkForImplementationAnnotation(Secured.class);
    }

    @Test
    public void testEntityTypesDeclarations() throws Exception {
        EntityType typeAny = EntityType.findTypeByEntity( Entity.class );
        Assert.assertEquals("Any entity", EntityType.ANY, typeAny);

        for ( EntityType type : EntityType.values() ) {
            if ( IGNORE_ENTITY_TYPES.contains( type ) ) continue; 

            Class<? extends Entity> clazz = type.getEntityClass();
            Assert.assertNotNull( "EntityType class must not be null for " + type, clazz );

            EntityType foundType = EntityType.findTypeByEntity( clazz );
            Assert.assertEquals("EntityType type mismatch for " + type, type, foundType);
        }
    }

    /**
     * Lazy proxies are on by default, but cause problems so ensure they are always disabled.
     */
    @Test
    public void testPersistentEntityForLazyProxy() {
        for ( EntityType type : EntityType.values() ) {
            if ( IGNORE_ENTITY_TYPES.contains( type ) ) continue;

            Class<? extends Entity> clazz = type.getEntityClass();
            Assert.assertNotNull( "EntityType class must not be null.", clazz );

            if ( clazz.getAnnotation(javax.persistence.Entity.class) != null ) {
                Assert.assertNotNull( clazz + " should have Proxy annotation (with lazy=false)", clazz.getAnnotation(org.hibernate.annotations.Proxy.class) );
                Assert.assertEquals( clazz + " should have Proxy annotation (with lazy=false)", false, clazz.getAnnotation(org.hibernate.annotations.Proxy.class).lazy() );
            }
        }

    }

    /**
     * Test that beans use the PostStartupApplicationListener interface instead of ApplicationListener.
     *
     * Directly implementing ApplicationListener can lead to a bean being initialized too
     * early in the startup process. When an event is fired the application context is
     * inspected and any ApplicationListeners must be created in order to handle the
     * event. Using PostStartupApplicationListener works around this problem, as the
     * beans are located by the StartupListenerRegistration bean and registered as
     * ApplicationListeners only when the ApplicationContext (implementation) is started.
     */
    @Test
    public void testPostStartupApplicationListenerUsage() throws Exception {
        final DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        final XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(dlbf);

        for ( final String context : CONTEXTS ) {
            xbdr.loadBeanDefinitions(new ClassPathResource(context));
        }

        final Collection<String> beans = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        int postStartupListenerCount = 0;
        for ( final String beanId : dlbf.getBeanDefinitionNames() ) {
            if ( !IGNORED_APPLICATION_LISTENER_BEANS.contains(beanId) ) {
                final BeanDefinition beanDef = dlbf.getBeanDefinition( beanId );
                if ( beanDef.getBeanClassName()==null ) continue;
                final Class<?> beanClass = Class.forName(beanDef.getBeanClassName());

                if ( ArrayUtils.contains( beanClass.getInterfaces(), ApplicationListener.class ) ) {
                    beans.add( beanId );
                } else if ( ArrayUtils.contains( beanClass.getInterfaces(), PostStartupApplicationListener.class ) ) {
                    postStartupListenerCount++;
                }
            }
        }

        if ( !beans.isEmpty() )
            Assert.fail( "Beans "+beans+" implement ApplicationListener, consider switching to PostStartupApplicationListener (or add to IGNORED_APPLICATION_LISTENER_BEANS whitelist)" );

        // Sanity check to ensure test is still working
        if (postStartupListenerCount==0) Assert.fail("Failed to find any listeners.");
    }

    @SuppressWarnings({"unchecked"})
    private void checkForImplementationAnnotation( final Class<? extends Annotation> annotation ) throws Exception {
        //
        DefaultListableBeanFactory dlbf = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(dlbf);

        for ( String context : CONTEXTS ) {
            xbdr.loadBeanDefinitions(new ClassPathResource(context));
        }

        for ( String beanId : dlbf.getBeanDefinitionNames() ) {
            BeanDefinition beanDef = dlbf.getBeanDefinition( beanId );
            String className = beanDef.getBeanClassName();
            if ( className != null && className.startsWith("com.l7tech" ) ) {
                Class beanClass = Class.forName(className);
                if ( beanClass.getAnnotation(annotation) != null ) {
                    Assert.fail( "Implementation bean '"+beanId+"', is annotated with the '"+annotation.getName()+"' annotation, this should only be used on interfaces." );
                }

                for ( Method method : beanClass.getDeclaredMethods() ) {
                    if ( method.getAnnotation(annotation) != null ) {
                        Assert.fail( "Implementation bean '"+beanId+"' method '"+method.getName()+"', is annotated with the '"+annotation.getName()+"' annotation, this should only be used on interfaces." );
                    }
                }
            }
        }
    }
}
