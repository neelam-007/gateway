package com.l7tech.gateway.rest;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.IOUtils;
import junit.framework.Assert;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivilegedActionException;

/**
 * This was created: 11/14/13 as 2:05 PM
 *
 * @author Victor Kazakov
 */
@RunWith(MockitoJUnitRunner.class)
public class CustomInjectionResolverFeatureTest {

    @Mock
    private ApplicationContext applicationContext1;

    @Mock
    private ApplicationContext applicationContext2;

    @Test
    public void testSpringBeanInjectionFrom2DifferentApplicationContexts() throws URISyntaxException, PrivilegedActionException, RequestProcessingException, IOException {
        RestAgentImpl restAgent1 = new RestAgentImpl();
        restAgent1.setApplicationContext(applicationContext1);

        RestAgentImpl restAgent2 = new RestAgentImpl();
        restAgent2.setApplicationContext(applicationContext2);

        restAgent1.setAdditionalResourceClasses(CollectionUtils.<Class<?>>set(RestTestResource.class));
        restAgent2.setAdditionalResourceClasses(CollectionUtils.<Class<?>>set(RestTestResource.class));

        restAgent1.init();
        restAgent2.init();

        Mockito.when(applicationContext1.getBean(String.class)).thenReturn("applicationContext1");
        Mockito.when(applicationContext2.getBean(String.class)).thenReturn("applicationContext2");

        RestResponse response1 = restAgent1.handleRequest(null, new URI(""), new URI("test"), "GET", null, new EmptyInputStream(), null, null);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copyStream(response1.getInputStream(), bout);
        String response1Body = bout.toString();

        Assert.assertEquals("applicationContext1", response1Body);

        RestResponse response2 = restAgent2.handleRequest(null, new URI(""), new URI("test"), "GET", null, new EmptyInputStream(), null, null);

        bout = new ByteArrayOutputStream();
        IOUtils.copyStream(response2.getInputStream(), bout);
        String response2Body = bout.toString();

        Assert.assertEquals("applicationContext2", response2Body);
    }

    @Test
    public void test2InjectionResolvers() throws URISyntaxException, PrivilegedActionException, RequestProcessingException, IOException {
        RestAgentImpl restAgent = new RestAgentImpl();

        restAgent.setApplicationContext(applicationContext1);

        restAgent.setAdditionalResourceClasses(CollectionUtils.<Class<?>>set(RestTestResource.class));
        restAgent.setAdditionalComponentObjects(CollectionUtils.set(
                new CustomInjectionResolverFeature<Inject1,InjectionResolver<Inject1>>(new InjectionResolver<Inject1>() {
                    @Override
                    public Object resolve(Injectee injectee, ServiceHandle<?> serviceHandle) {
                        return "inject1";
                    }

                    @Override
                    public boolean isConstructorParameterIndicator() {
                        return false;
                    }

                    @Override
                    public boolean isMethodParameterIndicator() {
                        return false;
                    }
                }, Inject1.class){},
                new CustomInjectionResolverFeature<Inject2,InjectionResolver<Inject2>>(new InjectionResolver<Inject2>() {
                    @Override
                    public Object resolve(Injectee injectee, ServiceHandle<?> serviceHandle) {
                        return "inject2";
                    }

                    @Override
                    public boolean isConstructorParameterIndicator() {
                        return false;
                    }

                    @Override
                    public boolean isMethodParameterIndicator() {
                        return false;
                    }
                }, Inject2.class){}));

        Mockito.when(applicationContext1.getBean(String.class)).thenReturn("applicationContext");

        restAgent.init();


        RestResponse response1 = restAgent.handleRequest(null, new URI(""), new URI("test/inject1"), "GET", null, new EmptyInputStream(), null, null);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copyStream(response1.getInputStream(), bout);
        String response1Body = bout.toString();

        Assert.assertEquals("inject1", response1Body);

        RestResponse response2 = restAgent.handleRequest(null, new URI(""), new URI("test/inject2"), "GET", null, new EmptyInputStream(), null, null);

        bout = new ByteArrayOutputStream();
        IOUtils.copyStream(response2.getInputStream(), bout);
        String response2Body = bout.toString();

        Assert.assertEquals("inject2", response2Body);

        RestResponse response3 = restAgent.handleRequest(null, new URI(""), new URI("test"), "GET", null, new EmptyInputStream(), null, null);

        bout = new ByteArrayOutputStream();
        IOUtils.copyStream(response3.getInputStream(), bout);
        String response3Body = bout.toString();

        Assert.assertEquals("applicationContext", response3Body);
    }

    @Path("test")
    public static class RestTestResource {
        @SpringBean
        private String test;

        @Inject1
        private String inject1;

        @Inject2
        private String inject2;

        @GET
        public String test() {
            return test;
        }

        @GET
        @Path("inject1")
        public String inject1() {
            return inject1;
        }

        @GET
        @Path("inject2")
        public String inject2() {
            return inject2;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public @interface Inject1 {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public @interface Inject2 {
    }
}
