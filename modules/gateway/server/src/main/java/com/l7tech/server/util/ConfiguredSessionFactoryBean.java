package com.l7tech.server.util;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.imp.PersistentEntityUtil;
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

            // The code below will reuse the goid a persistent entity has if the preserve id property have been set.
            if (o instanceof PersistentEntity &&
                    ((PersistentEntity) o).getGoid()!=null &&
                            PersistentEntityUtil.isPreserveId((PersistentEntity) o)) {
                return ((PersistentEntity) o).getGoid();
            }

            return new Goid(hi.get(), low.getAndIncrement());
        }
    }

    //- PRIVATE

    private final Map<String, Class<IdentityGenerator>> generators;
}
