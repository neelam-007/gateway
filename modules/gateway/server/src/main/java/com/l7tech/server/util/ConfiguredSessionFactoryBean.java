package com.l7tech.server.util;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.SequenceHiLoGenerator;
import org.hibernate.id.factory.DefaultIdentifierGeneratorFactory;
import org.hibernate.type.Type;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

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

    //- PRIVATE

    private final Map<String, Class<IdentityGenerator>> generators;
}
