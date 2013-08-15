package com.l7tech.server.util;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.util.RandomUtil;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.SequenceHiLoGenerator;
import org.hibernate.id.factory.DefaultIdentifierGeneratorFactory;
import org.hibernate.type.Type;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Extension of the annotation session factory bean that performs initial configuration.
 */
public class ConfiguredSessionFactoryBean extends AnnotationSessionFactoryBean {

    public ConfiguredSessionFactoryBean( @Nullable final Map<String, Class<IdentityGenerator>> generators ) {
        this.generators = Collections.unmodifiableMap(generators);
    }

    @Override
    protected Configuration newConfiguration() throws HibernateException {
        final Configuration configuration = super.newConfiguration();

        // Identifier generator configuration
        if ( !generators.isEmpty() ) {
            final DefaultIdentifierGeneratorFactory generatorFactory = configuration.getIdentifierGeneratorFactory();
            for ( final Entry<String, Class<IdentityGenerator>> entry : generators.entrySet() ) {
                generatorFactory.register( entry.getKey(), entry.getValue() );
            }
        }

        return configuration;
    }

    /**
     * Extension of SequenceHiLoGenerator with customized default configuration
     */
    public static final class ConfiguredHiLoGenerator extends SequenceHiLoGenerator {
        @Override
        public void configure( final Type type, final Properties params, final Dialect dialect ) throws MappingException {
            if ( !params.containsKey( MAX_LO ) ) params.setProperty( MAX_LO, "32767" );

            super.configure( type, params, dialect );
        }
    }

    /**
     *  Generate a GOID identifier.  Use existing GOID if available.
     */
    public static final class ConfiguredGOIDGenerator implements IdentifierGenerator {
        private static AtomicLong hi;
        private static AtomicLong low;
        private static final int MAX_GOID_RESERVED_PREFIX = 65536;

        public ConfiguredGOIDGenerator() {

            long random;
            do {
                random = RandomUtil.nextLong();
                // make sure hi cannot be in the range of default prefixes 0 - 2^16
            } while (random >= 0 && random < MAX_GOID_RESERVED_PREFIX);

            hi = new AtomicLong(random);
            low = new AtomicLong(RandomUtil.nextLong());
        }

        @Override
        public Serializable generate(SessionImplementor sessionImplementor, Object o) throws HibernateException {
            //Do not need to increment hi on low rollover. Preforming the increment could lead to race conditions without proper locking.
            // Also it is extremely unlikely a gateway will create 2^64 entities without restarting.

            // use an existing goid if one is given.
            if (o instanceof GoidEntity &&
                    (((GoidEntity) o).getGoid()!=null &&
                    !GoidEntity.DEFAULT_GOID.equals(((GoidEntity) o).getGoid()))) {
                return ((GoidEntity) o).getGoid();
            }
            //TODO: is there a more efficient way of creating a new goid?
            return new Goid(hi.get(), low.getAndIncrement());
        }
    }

    //- PRIVATE

    private final Map<String, Class<IdentityGenerator>> generators;
}
