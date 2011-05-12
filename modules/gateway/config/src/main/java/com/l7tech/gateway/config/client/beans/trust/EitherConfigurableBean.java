package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.config.client.ConfigurationException;
import com.l7tech.gateway.config.client.beans.ConfigResult;
import com.l7tech.gateway.config.client.beans.ConfigurationContext;
import com.l7tech.gateway.config.client.beans.EditableConfigurationBean;
import com.l7tech.util.Either;
import com.l7tech.util.Functions;

/**
 * Editable configurable for an Either<A,B> backed by separate configuration beans.
 */
abstract class EitherConfigurableBean<A,B,CA extends EditableConfigurationBean<A>,CB extends EditableConfigurationBean<B>> extends EditableConfigurationBean<Either<A,B>> {

    //- PUBLIC

    @Override
    public String getDisplayValue() {
        String value = null;

        if ( getConfigValue() != null ) {
            getConfigValue().either( new Functions.Unary<String, A>() {
                @Override
                public String call( final A a ) {
                    return configBeanA.getDisplayValue();
                }
            }, new Functions.Unary<String, B>() {
                @Override
                public String call( final B b ) {
                    return configBeanB.getDisplayValue();
                }
            } );
        }

        return value;
    }

    @Override
    public Either<A, B> parse( final String userInput ) throws ConfigurationException {
        if ( isA( userInput ) ) {
            return Either.left( configBeanA.parse( userInput ) );
        } else {
            return Either.right( configBeanB.parse( userInput ) );
        }
    }

    @Override
    public void validate( final Either<A, B> value ) throws ConfigurationException {
        value.either( new Functions.UnaryThrows<Void,A,ConfigurationException>(){
            @Override
            public Void call( final A a ) throws ConfigurationException {
                configBeanA.validate( a );
                return null;
            }
        }, new Functions.UnaryThrows<Void,B,ConfigurationException>(){
            @Override
            public Void call( final B b ) throws ConfigurationException {
                configBeanB.validate( b );
                return null;
            }
        } );
    }

    @Override
    public ConfigResult onConfiguration( final Either<A, B> value,
                                         final ConfigurationContext context ) {
        return value.either( new Functions.Unary<ConfigResult, A>() {
                @Override
                public ConfigResult call( final A a ) {
                    return configBeanA.onConfiguration( a, context );
                }
            }, new Functions.Unary<ConfigResult, B>() {
                @Override
                public ConfigResult call( final B b ) {
                    return configBeanB.onConfiguration( b, context );
                }
            } );
    }

    //- PROTECTED

    protected EitherConfigurableBean( final String id,
                                      final String shortIntro,
                                      final Either<A, B> defaultValue,
                                      final CA configBeanA,
                                      final CB configBeanB ) {
        super( id, shortIntro, defaultValue );
        this.configBeanA = configBeanA;
        this.configBeanB = configBeanB;
    }

    protected abstract boolean isA( String input );

    //- PRIVATE

    private final CA configBeanA;
    private final CB configBeanB;
}
