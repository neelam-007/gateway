package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterPropertyDescriptor;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.test.BugId;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Option;
import com.l7tech.util.ValidationUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(MockitoJUnitRunner.class)
public class ServerModuleFileClusterPropertiesReaderTest {
    @Mock
    private Registry registry;

    @Mock
    private ClusterStatusAdmin clusterStatusAdmin;

    private ServerModuleFileClusterPropertiesReader clusterPropertiesReader = new ServerModuleFileClusterPropertiesReader();


    @Before
    public void setUp() throws Exception {
        Registry.setDefault(registry);

        Mockito.doReturn(clusterStatusAdmin).when(registry).getClusterStatusAdmin();
        Mockito.doReturn(true).when(registry).isAdminContextPresent();

    }

    private void reflectChangePrivateField(final Object obj, final String fieldName, final Object value) throws Exception {
        Assert.assertThat(obj, Matchers.notNullValue());
        final Class cls = obj.getClass();
        Assert.assertThat(cls, Matchers.notNullValue());
        Assert.assertThat(fieldName, Matchers.not(Matchers.isEmptyOrNullString()));

        final Field field = cls.getDeclaredField(fieldName);
        Assert.assertThat(field, Matchers.notNullValue());
        field.setAccessible(true);
        field.set(obj, value);
    }

    @Test
    public void testIsModulesUploadEnabled() throws Exception {
        // create initial ClusterPropertyDescriptor for CLUSTER_PROP_UPLOAD_ENABLE, with default value to true
        final ClusterPropertyDescriptor uploadEnabled = new ClusterPropertyDescriptor(
                ServerModuleFileClusterPropertiesReader.CLUSTER_PROP_UPLOAD_ENABLE,
                null,
                "true", // default is true
                true,
                Option.<ValidationUtils.Validator<String>>optional(null)
        );
        final String defaultValueFieldName = "defaultValue";

        // mock getAllPropertyDescriptors
        Mockito.doReturn(
                CollectionUtils.list(
                        uploadEnabled,
                        new ClusterPropertyDescriptor(
                                "some.other.prop",
                                "some.other.prop.desc",
                                "some.other.prop.def.value",
                                true,
                                Option.<ValidationUtils.Validator<String>>optional(null)
                        )
                )
        ).when(clusterStatusAdmin).getAllPropertyDescriptors();

        // make sure there are no upload enable and upload max size cluster props
        Assert.assertThat(clusterStatusAdmin.findPropertyByName(ServerModuleFileClusterPropertiesReader.CLUSTER_PROP_UPLOAD_ENABLE), Matchers.nullValue());
        Assert.assertThat(clusterStatusAdmin.findPropertyByName(ServerModuleFileClusterPropertiesReader.CLUSTER_PROP_UPLOAD_MAX_SIZE), Matchers.nullValue());

        // test default value
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));

        // change default value to true
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "false");
        // test default value again
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));

        // test with invalid default value; trailing and leading space
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "  true");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "false ");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "  true ");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "\tfalse");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "true\t");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "\tfalse\t");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, " \ttrue \t");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "\nfalse");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "true\n");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "\nfalse\n");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, " \ntrue \n");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "\r\nfalse");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "true\r\n");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "\r\nfalse\r\n");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, " \r\ntrue \r\n");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, System.lineSeparator() + "false");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "true" + System.lineSeparator());
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, System.lineSeparator() + "false" + System.lineSeparator());
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, " " + System.lineSeparator() + "true " + System.lineSeparator());
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));

        // test with invalid default value; not a valid boolean
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "not valid boolean");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, null);
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        // set it to true
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "true");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));

        // mock findPropertyByName; only return valueToReturn for CLUSTER_PROP_UPLOAD_ENABLE
        final AtomicReference<String> valueToReturn = new AtomicReference<>("true");
        Mockito.doAnswer(
                new Answer<ClusterProperty>() {
                    @Override
                    public ClusterProperty answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertThat(invocation, Matchers.notNullValue());
                        Assert.assertThat(invocation.getArguments().length, Matchers.is(1));
                        Assert.assertThat(invocation.getArguments()[0], Matchers.instanceOf(String.class));
                        final String propName = (String) invocation.getArguments()[0];
                        Assert.assertThat(propName, Matchers.notNullValue());
                        // check if property name is modules upload enabled
                        if (propName.equals(ServerModuleFileClusterPropertiesReader.CLUSTER_PROP_UPLOAD_ENABLE)) {
                            return new ClusterProperty(propName, valueToReturn.get());
                        }
                        return null;
                    }
                }
        ).when(clusterStatusAdmin).findPropertyByName(Mockito.anyString());

        // test property  value
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));

        // change value to false
        valueToReturn.set("false");
        // test property value again
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));

        // test with invalid value; trailing and leading space
        valueToReturn.set("  true");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        valueToReturn.set("false ");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        valueToReturn.set("  true ");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        valueToReturn.set("\tfalse");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        valueToReturn.set("true\t");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        valueToReturn.set("\tfalse\t");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        valueToReturn.set(" \ttrue \t");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        valueToReturn.set("\nfalse");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        valueToReturn.set("true\n");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        valueToReturn.set("\nfalse\n");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        valueToReturn.set(" \ntrue \n");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        valueToReturn.set("\r\nfalse");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        valueToReturn.set("true\r\n");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        valueToReturn.set("\r\nfalse\r\n");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        valueToReturn.set(" \r\ntrue \r\n");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        valueToReturn.set(System.lineSeparator() + "false");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        valueToReturn.set("true" + System.lineSeparator());
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        valueToReturn.set(System.lineSeparator() + "false" + System.lineSeparator());
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        valueToReturn.set(" " + System.lineSeparator() + "true " + System.lineSeparator());
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(true));
        // test with invalid default value; not a valid boolean
        valueToReturn.set("not valid boolean");
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
        valueToReturn.set(null);
        Assert.assertThat(clusterPropertiesReader.isModulesUploadEnabled(), Matchers.is(false));
    }

    @BugId("SSG-11447")
    @Test
    public void testGetModulesUploadMaxSize() throws Exception {
        // create initial ClusterPropertyDescriptor for CLUSTER_PROP_UPLOAD_MAX_SIZE, with default value to 100
        final ClusterPropertyDescriptor uploadEnabled = new ClusterPropertyDescriptor(
                ServerModuleFileClusterPropertiesReader.CLUSTER_PROP_UPLOAD_MAX_SIZE,
                null,
                "100", // default is 100
                true,
                Option.<ValidationUtils.Validator<String>>optional(null)
        );
        final String defaultValueFieldName = "defaultValue";

        // mock getAllPropertyDescriptors
        Mockito.doReturn(
                CollectionUtils.list(
                        uploadEnabled,
                        new ClusterPropertyDescriptor(
                                "some.other.prop",
                                "some.other.prop.desc",
                                "some.other.prop.def.value",
                                true,
                                Option.<ValidationUtils.Validator<String>>optional(null)
                        )
                )
        ).when(clusterStatusAdmin).getAllPropertyDescriptors();

        // make sure there are no upload enable and upload max size cluster props
        Assert.assertThat(clusterStatusAdmin.findPropertyByName(ServerModuleFileClusterPropertiesReader.CLUSTER_PROP_UPLOAD_ENABLE), Matchers.nullValue());
        Assert.assertThat(clusterStatusAdmin.findPropertyByName(ServerModuleFileClusterPropertiesReader.CLUSTER_PROP_UPLOAD_MAX_SIZE), Matchers.nullValue());

        // test default value
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(100L));

        // change default value to 101
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "101");
        // test default value again
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(101L));

        // test with invalid default value; trailing and leading space
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "  102");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(102L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "103 ");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(103L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "  104 ");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(104L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "\t105");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(105L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "106\t");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(106L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "\t107\t");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(107L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, " \t108 \t");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(108L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "\n1105");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(1105L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "1106\n");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(1106L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "\n1107\n");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(1107L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, " \n1108 \n");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(1108L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "\r\n2105");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(2105L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "2106\r\n");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(2106L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "\r\n2107\r\n");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(2107L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, " \r\n2108 \r\n");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(2108L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, System.lineSeparator() + "3105");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(3105L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "3106" + System.lineSeparator());
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(3106L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, System.lineSeparator() + "3107" + System.lineSeparator());
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(3107L));
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, " " + System.lineSeparator() + "3108 " + System.lineSeparator());
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(3108L));
        // test with invalid default value; not a valid boolean
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "not valid number");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(ServerModuleFileClusterPropertiesReader.DEFAULT_SERVER_MODULE_FILE_UPLOAD_MAXSIZE)); // default value
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, null);
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(ServerModuleFileClusterPropertiesReader.DEFAULT_SERVER_MODULE_FILE_UPLOAD_MAXSIZE)); // default value
        // set it to 109
        reflectChangePrivateField(uploadEnabled, defaultValueFieldName, "109");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(109L));

        // mock findPropertyByName; only return valueToReturn for CLUSTER_PROP_UPLOAD_MAX_SIZE
        final AtomicReference<String> valueToReturn = new AtomicReference<>("110");
        Mockito.doAnswer(
                new Answer<ClusterProperty>() {
                    @Override
                    public ClusterProperty answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertThat(invocation, Matchers.notNullValue());
                        Assert.assertThat(invocation.getArguments().length, Matchers.is(1));
                        Assert.assertThat(invocation.getArguments()[0], Matchers.instanceOf(String.class));
                        final String propName = (String) invocation.getArguments()[0];
                        Assert.assertThat(propName, Matchers.notNullValue());
                        // check if property name is modules upload enabled
                        if (propName.equals(ServerModuleFileClusterPropertiesReader.CLUSTER_PROP_UPLOAD_MAX_SIZE)) {
                            return new ClusterProperty(propName, valueToReturn.get());
                        }
                        return null;
                    }
                }
        ).when(clusterStatusAdmin).findPropertyByName(Mockito.anyString());

        // test property value
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(110L));

        // change value to 111
        valueToReturn.set("111");
        // test property value again
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(111L));

        // test with invalid value; trailing and leading space
        valueToReturn.set("  112");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(112L));
        valueToReturn.set("113 ");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(113L));
        valueToReturn.set("  114 ");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(114L));
        valueToReturn.set("\t115");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(115L));
        valueToReturn.set("116\t");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(116L));
        valueToReturn.set("\t117\t");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(117L));
        valueToReturn.set(" \t118 \t");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(118L));
        // test with invalid default value; not a valid boolean
        valueToReturn.set("not valid number");
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(109L));
        valueToReturn.set(null);
        Assert.assertThat(clusterPropertiesReader.getModulesUploadMaxSize(), Matchers.is(109L));
    }
}