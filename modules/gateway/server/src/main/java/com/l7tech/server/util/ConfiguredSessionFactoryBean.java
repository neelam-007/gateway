package com.l7tech.server.util;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.HexUtils;
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
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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

        public ConfiguredGOIDGenerator() {

            // make sure hi cannot be zero and clash with default GOID value
            long random = getRandomLong();
            hi = new AtomicLong(random == 0 ? random + 1 : random);
            random = getRandomLong();
            low = new AtomicLong(random);
        }

        @Override
        public Serializable generate(SessionImplementor sessionImplementor, Object o) throws HibernateException {
            // increment hi on low rollover
            if (low.get() == Long.MAX_VALUE) {
                low.set(0);
                hi.incrementAndGet();
            }

            if (o instanceof GoidEntity &&
                    ((GoidEntity) o).getGoid() != GoidEntity.DEFAULT_GOID) {
                return ((GoidEntity) o).getGoid();
            }
            return new Goid(hi.get(), low.getAndIncrement());
        }

        protected static long getRandomLong(){
            return ((long) (RandomUtil.nextInt(Integer.MAX_VALUE)) << 32) + RandomUtil.nextInt(Integer.MAX_VALUE);
        }
    }

    //- PRIVATE

    private final Map<String, Class<IdentityGenerator>> generators;
}
