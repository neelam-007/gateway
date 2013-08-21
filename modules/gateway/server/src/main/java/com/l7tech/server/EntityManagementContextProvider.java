package com.l7tech.server;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.server.EntityManagementContext.EntityManagerException;
import com.l7tech.server.EntityManagementContext.EntityManagerProvider;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.util.Option;
import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.hibernate.id.TableHiLoGenerator;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

import static com.l7tech.util.Option.some;
import static java.util.Collections.singleton;

/**
 * Provides entity management contexts.
 *
 * This class will:
 *
 * - access the underlying datasource
 * - create a hibernate session factory
 * - register any entity manager context providers
 * - provide base implementations or support declaration of required DB schema(s) for a provider
 * - cache contexts
 * - dispose of contexts that are no longer in use
 */
public class EntityManagementContextProvider {

    //- PUBLIC

    public EntityManagementContextProvider() {
    }

    public EntityManagementContext getEntityManagementContext( final String datasourceName ) throws NamingException, SQLException {
        final DataSource dataSource = jdbcConnectionPoolManager.getDataSource( datasourceName );

        final Properties properties = new Properties();
        properties.put( "hibernate.dialect", MySQL5InnoDBDialect.class.getName() );
        // or one with "Sequence" support if we want to use our cluster safe identifiers (requires stored procedure)
        //properties.put( "hibernate.dialect", MySQL5InnoDBDialectWithSeq.class.getName() );

        final AbstractApplicationContext parentContext = new ClassPathXmlApplicationContext();
        parentContext.refresh();
        final ConfigurableListableBeanFactory beanFactory = parentContext.getBeanFactory();
        beanFactory.registerSingleton( "dataSource", dataSource );
        beanFactory.registerSingleton( "dataSourceHibernateProperties", properties );
        beanFactory.registerSingleton( "dataSourceIdGeneratorClass", TableHiLoGenerator.class.getName() );
        // or the "Sequence" generator if we want to use our cluster safe identifiers (requires stored procedure)
        //beanFactory.registerSingleton( "dataSourceIdGeneratorClass", ConfiguredHiLoGenerator.class.getName() );

        final AbstractApplicationContext context = new ClassPathXmlApplicationContext( new String[]{
                "/com/l7tech/server/resources/datasourceAccessContext.xml",
        }, EntityManagementContextProvider.class, parentContext );

        context.start(); //TODO manage lifecycle for contexts

        return new EntityManagementContext( singleton( new SimpleEntityManagerProvider( context.getBeanFactory() ) ) );
    }

    //- PRIVATE

    @NotNull
    @Inject
    private JdbcConnectionPoolManager jdbcConnectionPoolManager;

    /**
     * Initial entity manager provider that does not manage the schema.
     */
    private static final class SimpleEntityManagerProvider implements EntityManagerProvider {
        private final BeanFactory beanFactory;

        private SimpleEntityManagerProvider( final BeanFactory beanFactory ) {
            this.beanFactory = beanFactory;
        }

        @NotNull
        @Override
        public <EM extends EntityManager> Option<EM> getEntityManager( @NotNull final Class<EM> entityManager ) throws EntityManagerException {
            try {
                return some( beanFactory.getBean( entityManager ) );
            } catch ( final BeansException e ) {
                throw new EntityManagerException("Error creating manager of type '"+entityManager.getName()+"'", e);
            }
        }
    }
}
