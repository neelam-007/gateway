package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.form.AbstractSingleSelectChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.util.string.AppendingStringBuffer;

import java.util.List;

/**
 * Option form component that supports option groups.
 */
public class GroupingDropDownChoice extends AbstractSingleSelectChoice {

    //- PUBLIC

    /**
     *
	 */
	public GroupingDropDownChoice( final String id, final List data ) {
		super( id, data );
        this.defaultChoiceRenderer = new ChoiceRenderer();
        this.setChoiceRenderer( new OptionChoiceRenderer() );
    }

    //- PROTECTED

    @Override
    protected void appendOptionHtml( final AppendingStringBuffer buffer,
                                     final Object choice,
                                     final int index,
                                     final String selected ) {
        String optionGroup = getOptionGroupForChoice( choice );
        if ( optionGroup != null ) {
            if ( !optionGroup.equals(lastRenderedOptionGroup) ) {
                buffer.append( "<optgroup label=\"" );
                buffer.append( optionGroup );
                buffer.append( "\">" );
                lastRenderedOptionGroup = optionGroup;
            }
        } else if ( lastRenderedOptionGroup != null ) {
            // close option group
            buffer.append( "</optgroup>" );
            lastRenderedOptionGroup = null;
        }

        super.appendOptionHtml(buffer, choice, index, selected);

        if (  getChoices().size()-1 <= index ) {
            // close final option group
            buffer.append( "</optgroup>" );
        }
    }

    /**
     * 
	 */
	@Override
    protected void onComponentTag( final ComponentTag tag ) {
		checkComponentTag(tag, "select");
        super.onComponentTag(tag);
	}

    protected Object getOptionDisplayValue( final Object object ) {
        return defaultChoiceRenderer.getDisplayValue( object );
    }

    protected String getOptionIdValue( final Object object, final int index ) {
        return defaultChoiceRenderer.getIdValue( object, index );
    }

    protected String getOptionGroupForChoice( final Object object ) {
        return null;
    }

    //- PRIVATE

    private String lastRenderedOptionGroup;
    private ChoiceRenderer defaultChoiceRenderer;

    private class OptionChoiceRenderer implements IChoiceRenderer {
        @Override
        public Object getDisplayValue(Object object) {
            return getOptionDisplayValue( object );
        }

        @Override
        public String getIdValue(Object object, int index) {
            return getOptionIdValue( object, index );
        }
    }
}
