package com.l7tech.server.util;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Arrays;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.l7tech.common.util.WhirlycacheFactory;

import com.whirlycott.cache.Cache;

/**
 * Factory for creating caching proxies.
 *
 * <p>Interface methods should be annotated to allow caching.</p>
 *  
 * @see Cachable
 * @author Steve Jones
 */
public class EntityCachingFactoryBean extends AbstractFactoryBean {

    //- PUBLIC

    /**
     * Create an EntityCachingFactoryBean for the given interface/instance.
     *
     * @param serviceInterface The interface to be proxied (should be annotated, must not be null).
     * @param serviceInstance The instance to be proxied (must not be null).
     */
    public EntityCachingFactoryBean(final Class serviceInterface,
                                    final Object serviceInstance) {
        if (serviceInterface == null) throw new IllegalArgumentException("serviceInterface is required");
        if (serviceInstance == null) throw new IllegalArgumentException("serviceInstance is required");
        
        this.serviceInterface = serviceInterface;
        this.serviceInstance = serviceInstance;
    }

    /**
     * Get the type of the object returned from this factory.
     *
     * @return The serviceInterface class.
     */
    public Class getObjectType() {
        return serviceInterface;
    }

    //- PROTECTED

    /**
     * Create the proxy instance. 
     *
     * @return The proxy object
     * @throws Exception if an error occurs
     */
    protected Object createInstance() throws Exception {
        if (cache == null) {
            cache = WhirlycacheFactory.createCache("EMCache-" + serviceInterface.getName(), 1000, 63, WhirlycacheFactory.POLICY_LRU);
        }

        Object instance = Proxy.newProxyInstance(
                EntityCachingFactoryBean.class.getClassLoader(),
                new Class[]{ serviceInterface },
                getInvocationHandler());

        return instance;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(EntityCachingFactoryBean.class.getName());

    private final Class serviceInterface;
    private final Object serviceInstance;
    private Cache cache;

    private InvocationHandler getInvocationHandler() {
        return new InvocationHandler(){
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object result = null;

                // Get the cache configuration for the method.
                Cachable cacheConfig = method.getAnnotation(Cachable.class);
                MethodArgumentKey mak = null;
                CachedValue cacheResult = null;

                // See if cached
                if (cacheConfig != null) {
                    if (logger.isLoggable(Level.FINEST))
                        logger.log(Level.FINEST, "Using cachable annotation arg={0}, max-age={1}, ",
                                new Object[]{cacheConfig.relevantArg(),cacheConfig.maxAge()});

                    mak = buildCacheKey(cacheConfig, args, method);

                    cacheResult = (CachedValue) cache.retrieve(mak);

                    // Ignore if expired
                    if ( cacheResult != null && cacheResult.isExpired() ) {
                        if (logger.isLoggable(Level.FINER))
                            logger.log(Level.FINER, "Not using expired cached value for method ''{0}'', parameter ''{1}''.",
                                    new Object[]{method.getName(), args[cacheConfig.relevantArg()]});

                        cacheResult = null;
                    }
                }

                // Check cacheResult to allow caching of null values
                if ( cacheResult == null ) {
                    if (logger.isLoggable(Level.FINER))
                        logger.log(Level.FINER, "Invoking method ''{0}''.", new Object[]{method.getName()});

                    result = method.invoke(serviceInstance, args);

                    // Cache if permitted
                    if (cacheConfig != null) {
                        if (logger.isLoggable(Level.FINER))
                            logger.log(Level.FINER, "Caching value for method ''{0}'', parameter ''{1}''.",
                                    new Object[]{method.getName(), args[cacheConfig.relevantArg()]});

                        long expiryPeriod = cacheConfig.maxAge();
                        cache.store(mak, new CachedValue(result, System.currentTimeMillis() + expiryPeriod), expiryPeriod);
                    }
                } else {
                    // Use value from cache
                    if (logger.isLoggable(Level.FINER))
                        logger.log(Level.FINER, "Using cached value for method ''{0}'', parameter ''{1}''.",
                                new Object[]{method.getName(), args[cacheConfig.relevantArg()]});

                    result = cacheResult.result;
                }

                return result;
            }
        };
    }

    /**
     *
     */
    private MethodArgumentKey buildCacheKey(Cachable cacheConfig, Object[] args, Method method) {
        Object[] cacheArgs = new Object[]{};

        if ( cacheConfig.relevantArg() >= 0 ) {
            cacheArgs = new Object[]{ args[cacheConfig.relevantArg()] };
        }

        return new MethodArgumentKey(method, cacheArgs);
    }

    /**
     * Cache key for method invocations
     */
    private static final class MethodArgumentKey {
        private final Method method;
        private final Object[] methodArguments;

        private MethodArgumentKey(Method method, Object[] methodArguments) {
            this.method = method;
            this.methodArguments = methodArguments;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MethodArgumentKey that = (MethodArgumentKey) o;

            if (!method.equals(that.method)) return false;
            if (!Arrays.equals(methodArguments, that.methodArguments)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = method.hashCode();
            result = 31 * result + Arrays.hashCode(methodArguments);
            return result;
        }
    }

    /**
     * Value is just an Object with a timestamp (of the actual expiry)
     */
    private static final class CachedValue {
        private final Object result;
        private final long expiry;

        private CachedValue(Object result, long expiry) {
            this.result = result;
            this.expiry = expiry;
        }

        private boolean isExpired() {
            boolean expired = true;

            if (expiry > System.currentTimeMillis()) {
                expired = false;
            }

            return expired;
        }
    }
}
