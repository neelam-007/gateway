package com.l7tech.server.module;

import com.l7tech.gateway.common.module.ModuleDigest;
import com.l7tech.gateway.common.module.ModuleState;
import com.l7tech.gateway.common.module.ModuleType;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.policy.module.ModulesScannerTestBase;
import com.l7tech.util.Either;
import com.l7tech.util.Triple;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

import java.io.*;
import java.util.*;

/**
 * Base class for {@code ServerModuleFile} unit tests.<br/>
 * Provides common ways of creating {@code ServerModuleFile}'s.
 *
 */
public class ServerModuleFileTestBase extends ModulesScannerTestBase {

    // used to generate random GOID
    protected static final Random rnd = new Random();
    protected static final int typeLength = ModuleType.values().length;
    protected static final long GOID_HI_START = Long.MAX_VALUE - 1;

    /**
     * Utility class for storing the module bytes as a File.
     */
    @SuppressWarnings("serial")
    protected class MyServerModuleFile extends ServerModuleFile {
        private Either<File, byte[]> content;
        public void setModuleContent(final Either<File, byte[]> content) {
            Assert.assertNotNull(content);
            Assert.assertTrue(content.isLeft() || content.isRight());
            this.content = content;
        }
        public InputStream getModuleContentStream() throws FileNotFoundException {
            if (content != null) {
                Assert.assertTrue(content.isLeft() || content.isRight());
                if (content.isLeft()) {
                    return new BufferedInputStream(new FileInputStream(content.left()));
                } else {
                    return new ByteArrayInputStream(content.right());
                }
            }
            return null;
        }
    }

    /**
     * {@link MyServerModuleFile} builder class.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected class ServerModuleFileBuilder {
        private Goid goid;
        private String name;
        private Integer version;
        private ModuleType moduleType;
        private Either<File, byte[]> moduleContent;
        private String checkSum;

        /**
         * Pre-attached {@link MyServerModuleFile}.
         * The builder will append new properties or override existing ones.
         */
        private MyServerModuleFile moduleFile;

        /**
         * Default constructor
         */
        public ServerModuleFileBuilder() {
            this(new MyServerModuleFile());
        }

        /**
         * Initialize the builder with preexisting module file.
         * This way the builder will append new properties or override existing ones.
         *
         * @param moduleFile    the {@link MyServerModuleFile} to attach to.
         */
        public ServerModuleFileBuilder(final MyServerModuleFile moduleFile) {
            this.moduleFile = moduleFile;
        }

        private final Collection<Triple<String, ModuleState, String>> states = new ArrayList<>();
        private final Map<String, String> properties = new HashMap<>();

        public ServerModuleFileBuilder goid(final Goid goid) {
            this.goid = goid;
            return this;
        }

        public ServerModuleFileBuilder name(final String name) {
            this.name = name;
            return this;
        }

        public ServerModuleFileBuilder version(final Integer version) {
            this.version = version;
            return this;
        }

        public ServerModuleFileBuilder moduleType(final ModuleType moduleType) {
            this.moduleType = moduleType;
            return this;
        }

        public ServerModuleFileBuilder content(final File file) {
            if (file != null) {
                this.moduleContent = Either.left(file);
            }
            return this;
        }

        public ServerModuleFileBuilder content(final byte[] bytes) {
            if (bytes != null) {
                this.moduleContent = Either.right(bytes);
            }
            return this;
        }

        public ServerModuleFileBuilder checkSum(final String checkSum) {
            this.checkSum = checkSum;
            return this;
        }

        public ServerModuleFileBuilder addState(final String node, final ModuleState state) {
            if (state == null) // do not add if null
                return this;
            return addState(node, state, null);
        }

        public ServerModuleFileBuilder addStateError(final String node, final String error) {
            if (error == null) // do not add if null
                return this;
            return addState(node, null, error);

        }

        private ServerModuleFileBuilder addState(final String node, final ModuleState state, final String error) {
            this.states.add(Triple.triple(node, state, error));
            return this;
        }

        public ServerModuleFileBuilder addProperty(final String name, final String value) {
            this.properties.put(name, value);
            return this;
        }

        public MyServerModuleFile build() {
            Assert.assertNotNull(moduleFile);
            if (goid != null) {
                moduleFile.setGoid(goid);
            }
            if (name != null) {
                moduleFile.setName(name);
            }
            if (version != null) {
                moduleFile.setVersion(version);
            }
            if (moduleType != null) {
                moduleFile.setModuleType(moduleType);
            }
            if (moduleContent != null) {
                moduleFile.setModuleContent(moduleContent);
                try {
                    moduleFile.setModuleSha256(ModuleDigest.digest(moduleFile.getModuleContentStream()));
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (checkSum != null) {
                moduleFile.setModuleSha256(checkSum); // override check-sum
            }
            for (final Map.Entry<String, String> property : properties.entrySet()) {
                if (MyServerModuleFile.PROP_SIZE.equals(property.getKey())) {
                    moduleFile.setProperty(MyServerModuleFile.PROP_SIZE, property.getValue());
                } else if (MyServerModuleFile.PROP_ASSERTIONS.equals(property.getKey())) {
                    moduleFile.setProperty(MyServerModuleFile.PROP_ASSERTIONS, property.getValue());
                } else if (MyServerModuleFile.PROP_FILE_NAME.equals(property.getKey())) {
                    moduleFile.setProperty(MyServerModuleFile.PROP_FILE_NAME, property.getValue());
                } else {
                    Assert.fail("Unsupported property: " + property.getKey());
                }
            }
            for (final Triple<String, ModuleState, String> state : states) {
                if (StringUtils.isNotBlank(state.left)) {
                    if (state.middle != null) {
                        moduleFile.setStateForNode(state.left, state.middle);
                    } else {
                        moduleFile.setStateErrorMessageForNode(state.left, state.right);
                    }
                }
            }

            return moduleFile;
        }
    }

    /**
     * Convenient method for creating a test sample of {@link MyServerModuleFile} without any states, having the following attributes:
     * <ul>
     *     <li>goid: {@code Goid(GOID_HI_START, ordinal)}</li>
     *     <li>name: {@code module_[ordinal]}</li>
     *     <li>version: {@code 0}</li>
     *     <li>specified {@code moduleType} or a random type if {@code null}</li>
     *     <li>bytes: {@code test data _[ordinal]}</li>
     *     <li>file-name: {@code module_[ordinal].[jar or aar, depending whether the type is modular or custom assertions]}</li>
     *     <li>size: {@code length of the bytes array}</li>
     *     <li>assertions: {@code assertion_[ordinal]}</li>
     * </ul>
     * @param ordinal       the ordinal of this test sample
     * @param moduleType    the module type either {@link ModuleType#MODULAR_ASSERTION} or {@link ModuleType#CUSTOM_ASSERTION}
     * @param moduleBytes   the module file containing the bytes, instead of loading the bytes in memory. Optional and can be {@code null}.
     */
    protected MyServerModuleFile create_test_module_without_states(final long ordinal, ModuleType moduleType, final File moduleBytes) {
        Assert.assertTrue(moduleBytes == null || moduleBytes.exists());
        moduleType = moduleType != null ? moduleType : ModuleType.values()[rnd.nextInt(typeLength)];
        return new ServerModuleFileBuilder()
                .goid(new Goid(GOID_HI_START, ordinal))
                .name("module_" + ordinal)
                .version(0)
                .moduleType(moduleType)
                .content(moduleBytes)
                .addProperty(ServerModuleFile.PROP_FILE_NAME, (ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? "CustomAssertion" : "ModularAssertion") + "FileName" + ordinal + (ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? ".jar" : ".aar"))
                .addProperty(ServerModuleFile.PROP_ASSERTIONS, (ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? "Custom" : "Modular") + "Assertion_" + ordinal)
                .addProperty(ServerModuleFile.PROP_SIZE, String.valueOf(moduleBytes != null ? moduleBytes.length() : 0))
                .build();
    }

    /**
     * @see #create_test_module_without_states(long, com.l7tech.gateway.common.module.ModuleType, java.io.File)
     */
    protected MyServerModuleFile create_test_module_without_states(final long ordinal, ModuleType moduleType, final byte[] moduleBytes) {
        moduleType = moduleType != null ? moduleType : ModuleType.values()[rnd.nextInt(typeLength)];
        return new ServerModuleFileBuilder()
                .goid(new Goid(GOID_HI_START, ordinal))
                .name("module_" + ordinal)
                .version(0)
                .moduleType(moduleType)
                .content(moduleBytes)
                .addProperty(ServerModuleFile.PROP_FILE_NAME, (ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? "CustomAssertion" : "ModularAssertion") + "FileName" + ordinal + (ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? ".jar" : ".aar"))
                .addProperty(ServerModuleFile.PROP_ASSERTIONS, (ModuleType.CUSTOM_ASSERTION.equals(moduleType) ? "Custom" : "Modular") + "Assertion_" + ordinal)
                .addProperty(ServerModuleFile.PROP_SIZE, String.valueOf(moduleBytes != null ? moduleBytes.length : 0))
                .build();
    }
}
