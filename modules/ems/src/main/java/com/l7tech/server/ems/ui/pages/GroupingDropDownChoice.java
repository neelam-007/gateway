package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.form.AbstractSingleSelectChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.util.string.AppendingStringBuffer;

import java.util.List;

/**
 * Option form component that supports option groups.
 */
public class GroupingDropDownChoice<T> extends AbstractSingleSelectChoice<T> {

    //- PUBLIC

    /**
     *
	 */
	public GroupingDropDownChoice( final String id, final List<? extends T> data ) {
		super( id, data );
        this.defaultChoiceRenderer = new ChoiceRenderer<T>();
        this.setChoiceRenderer( new OptionChoiceRenderer() );
    }

    //- PROTECTED

    @Override
    protected void appendOptionHtml( final AppendingStringBuffer buffer,
                                     final T choice,
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

    protected Object getOptionDisplayValue( final T object ) {
        return defaultChoiceRenderer.getDisplayValue( object );
    }

    protected String getOptionIdValue( final T object, final int index ) {
        return defaultChoiceRenderer.getIdValue( object, index );
    }

    protected String getOptionGroupForChoice( final Object object ) {
        return null;
    }

    //- PRIVATE

    private String lastRenderedOptionGroup;
    private ChoiceRenderer<T> defaultChoiceRenderer;

    private class OptionChoiceRenderer implements IChoiceRenderer<T> {
        @Override
        public Object getDisplayValue(T object) {
            return getOptionDisplayValue( object );
        }

        @Override
        public String getIdValue(T object, int index) {
            return getOptionIdValue( object, index );
        }
    }
}
