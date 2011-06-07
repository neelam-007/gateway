package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.variable.Syntax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Assertion that can validate the syntax of a target message's outer content type.
 */
public class ContentTypeAssertion extends MessageTargetableAssertion {
    private boolean changeContentType;
    private String newContentTypeValue;
    private boolean messagePart;
    private String messagePartNum = "0";

    public boolean isChangeContentType() {
        return changeContentType;
    }

    public void setChangeContentType(boolean changeContentType) {
        this.changeContentType = changeContentType;
    }

    public String getNewContentTypeValue() {
        return newContentTypeValue;
    }

    public void setNewContentTypeValue(String newContentTypeValue) {
        this.newContentTypeValue = newContentTypeValue;
    }

    public boolean isMessagePart() {
        return messagePart;
    }

    public void setMessagePart(boolean messagePart) {
        this.messagePart = messagePart;
    }

    public String getMessagePartNum() {
        return messagePartNum;
    }

    public void setMessagePartNum(String messagePartNum) {
        this.messagePartNum = messagePartNum;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        List<String> vars = new ArrayList<String>(Arrays.asList(super.getVariablesUsed()));
        if (messagePart) vars.addAll(Arrays.asList(Syntax.getReferencedNames(messagePartNum)));
        if (changeContentType) vars.addAll(Arrays.asList(Syntax.getReferencedNames(newContentTypeValue)));
        return vars.toArray(new String[vars.size()]);
    }

    protected final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<ContentTypeAssertion>(){
        @Override
        public String getAssertionName( final ContentTypeAssertion assertion, final boolean decorate) {
            final String displayName = assertion.meta().get(SHORT_NAME);
            if (!decorate) return displayName;

            final boolean change = assertion.isChangeContentType();
            String action = change ? "Change" : "Validate";
            if (assertion.isMessagePart()) action = "(Part " + assertion.getMessagePartNum() + ") " + action;
            final StringBuilder name = new StringBuilder();
            name.append( action );
            name.append( " Content Type" );
            if (change) name.append(" to ").append(assertion.getNewContentTypeValue());
            return AssertionUtils.decorateName(assertion, name);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(SHORT_NAME, "Validate or Change Content Type");
        meta.put(PROPERTIES_ACTION_NAME, "Content Type Properties");
        meta.put(DESCRIPTION, "Validate or change the content type of a target message.");
        meta.put(PALETTE_FOLDERS, new String[] { "threatProtection", "xml" });
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.ContentTypeAssertionDialog");
        return meta;
    }
}
