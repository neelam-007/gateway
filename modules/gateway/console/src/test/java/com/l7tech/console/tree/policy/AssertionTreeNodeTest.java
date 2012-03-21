package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AssertionTreeNodeTest {
    private AssertionTreeNode node;
    private AssertionStub assertionStub;
    private List<VariableMetadata> metadata;

    @Before
    public void setup() {
        metadata = new ArrayList<VariableMetadata>();
        assertionStub = new AssertionStub(metadata);
        node = new AssertionTreeNodeStub(assertionStub);
    }

    @Test
    public void getTooltip() {
        metadata.add(new VariableMetadata("test", false, false, null, true, DataType.STRING));

        final String tooltip = node.getTooltipText();

        assertEquals("<html>Sets ${<b>test</b>} [String]</html>", tooltip);
    }

    @Test
    public void getTooltipMultivalued() {
        metadata.add(new VariableMetadata("test", false, true, null, true, DataType.STRING));

        final String tooltip = node.getTooltipText();

        assertEquals("<html>Sets ${<b>test</b>} [String, Multivalued]</html>", tooltip);
    }

    @Test
    public void getTooltipMultiple() {
        metadata.add(new VariableMetadata("test1", false, false, null, true, DataType.STRING));
        metadata.add(new VariableMetadata("test2", false, false, null, true, DataType.STRING));

        final String tooltip = node.getTooltipText();

        assertEquals("<html>Sets ${<b>test1</b>} [String], ${<b>test2</b>} [String]</html>", tooltip);
    }

    @Test
    public void getTooltipNullDataTypeNotMultivalued() {
        metadata.add(new VariableMetadata("test", false, false, null, true, null));

        final String tooltip = node.getTooltipText();

        assertEquals("<html>Sets ${<b>test</b>}</html>", tooltip);
    }

    @Test
    public void getTooltipNullDataTypeMultivalued() {
        metadata.add(new VariableMetadata("test", false, true, null, true, null));

        final String tooltip = node.getTooltipText();

        assertEquals("<html>Sets ${<b>test</b>} [Multivalued]</html>", tooltip);
    }

    @Test
    public void getTooltipMoreThanThree() {
        metadata.add(new VariableMetadata("test1", false, false, null, true, DataType.STRING));
        metadata.add(new VariableMetadata("test2", false, false, null, true, DataType.STRING));
        metadata.add(new VariableMetadata("test3", false, false, null, true, DataType.STRING));
        metadata.add(new VariableMetadata("test4", false, false, null, true, DataType.STRING));

        final String tooltip = node.getTooltipText();

        assertEquals("<html>Sets ${<b>test1</b>} [String], ${<b>test2</b>} [String], ${<b>test3</b>} [String], <br>${<b>test4</b>} [String]</html>", tooltip);
    }

    private class AssertionStub extends Assertion implements SetsVariables {
        private final List<VariableMetadata> metadata;

        public AssertionStub(final List<VariableMetadata> metadata) {
            this.metadata = metadata;
        }

        @Override
        public VariableMetadata[] getVariablesSet() {
            return metadata.toArray(new VariableMetadata[metadata.size()]);
        }
    }

    private class AssertionTreeNodeStub extends AssertionTreeNode<AssertionStub> {
        AssertionTreeNodeStub(AssertionStub assertion) {
            super(assertion);
        }

        @Override
        public String getName(boolean decorate) {
            return null;
        }

        @Override
        protected String iconResource(boolean open) {
            return null;
        }
    }
}
