package com.l7tech.server.ems.ui.pages;

import com.l7tech.server.ems.NavigationModel;
import com.l7tech.server.ems.EsmSecurityManager;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.util.Functions;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/**
 * Panel for main navigation.
 */
@Administrative(licensed=false)
public class NavigationPanel extends Panel {

    /**
     * The model must be the page being rendered.
     */
    public NavigationPanel( final String id, final IModel model, final EsmSecurityManager securityManager ) {
        super(id, model);

        NavigationModel navigationModel = new NavigationModel("com.l7tech.server.ems.ui.pages", new Functions.Unary<Boolean,Class<? extends Page>>(){
            @Override
            public Boolean call(Class<? extends Page> aClass) {
                return securityManager.hasPermission( aClass );
            }
        });


        String currentPage = ((EsmPage)model.getObject()).getPageName();
        currentPage = currentPage.substring(currentPage.lastIndexOf('.')+1);
        String currentSection = navigationModel.getNavigationSectionForPage(currentPage);
        if ( currentSection == null ) {
            currentSection = navigationModel.getNavigationSections().iterator().next();            
        }

        RepeatingView navigationTopContainer = new RepeatingView("navigationTabTop");
        navigationTopContainer.setRenderBodyOnly(true);
        add( navigationTopContainer );

        int viewCount = 0;
        for ( String section : navigationModel.getNavigationSections() ) {
            Fragment fragment = new Fragment(Integer.toString(viewCount++), "navigationTabTopFragment", this );
            fragment.setRenderBodyOnly(true);
            fragment.add( new WebMarkupContainer("idSection1").add(new AppendingAttributeModifier("id", new Model(section))) );
            fragment.add( new WebMarkupContainer("idSection2").add(new AppendingAttributeModifier("id", new Model(section))) );
            fragment.add( new WebMarkupContainer("idSection3").add(new AppendingAttributeModifier("id", new Model(section))) );
            navigationTopContainer.add( fragment );
        }

        RepeatingView navigationMiddleContainer = new RepeatingView("navigationTabMiddle");
        navigationMiddleContainer.setRenderBodyOnly(true);
        add( navigationMiddleContainer );
        for ( String section : navigationModel.getNavigationSections() ) {
            if ( currentSection.equals(section) ) {
                Fragment fragment = new Fragment(Integer.toString(viewCount++),  "navigationTabMiddleFragmentOn" , this );
                fragment.add( new WebMarkupContainer("idSection1").add(new AppendingAttributeModifier("id", new Model(section))) );
                WebMarkupContainer td = new WebMarkupContainer("idSection2");
                td.add(new AppendingAttributeModifier("id", new Model(section)));
                fragment.add( td );
                Label label = new Label("name", new StringResourceModel( "section." + section + ".label", this, null ) );
                label.setRenderBodyOnly(true);
                td.add( label );
                fragment.setRenderBodyOnly(true);
                fragment.add( new WebMarkupContainer("idSection3").add(new AppendingAttributeModifier("id", new Model(section))) );
                navigationMiddleContainer.add( fragment );
            } else {
                Fragment fragment = new Fragment(Integer.toString(viewCount++),  "navigationTabMiddleFragmentOff", this );
                fragment.add( new WebMarkupContainer("idSection1").add(new AppendingAttributeModifier("id", new Model(section))) );
                WebMarkupContainer td = new WebMarkupContainer("idSection2");
                td.add(new AppendingAttributeModifier("id", new Model(section)));
                fragment.add( td );
                ExternalLink link = new ExternalLink("link", navigationModel.getPageUrlForSection(section) );
                td.add( link );
                Label label = new Label("name", new StringResourceModel( "section." + section + ".label", this, null ) );
                label.setRenderBodyOnly(true);
                link.add( label );
                fragment.setRenderBodyOnly(true);
                fragment.add( new WebMarkupContainer("idSection3").add(new AppendingAttributeModifier("id", new Model(section))) );
                navigationMiddleContainer.add( fragment );
            }
        }

        RepeatingView navigationBottomContainer = new RepeatingView("navigationTabBottom");
        navigationBottomContainer.setRenderBodyOnly(true);
        add( navigationBottomContainer );
        for ( String section : navigationModel.getNavigationSections() ) {
            Fragment fragment = new Fragment(Integer.toString(viewCount++), currentSection.equals(section) ? "navigationTabBottomFragmentOn" : "navigationTabBottomFragmentOff", this );
            fragment.setRenderBodyOnly(true);
            fragment.add( new WebMarkupContainer("idSection1").add(new AppendingAttributeModifier("id", new Model(section))) );
            fragment.add( new WebMarkupContainer("idSection2").add(new AppendingAttributeModifier("id", new Model(section))) );
            fragment.add( new WebMarkupContainer("idSection3").add(new AppendingAttributeModifier("id", new Model(section))) );
            navigationBottomContainer.add( fragment );
        }

        RepeatingView view = new RepeatingView("navigationScript");
        add( view );
        for ( String scriptSection : navigationModel.getNavigationSections() ) {
            if ( currentSection.equals(currentSection) ) continue;

            Label label = new Label("tabName", scriptSection);
            Fragment fragment = new Fragment(Integer.toString(viewCount++), "navigationScriptFragment", this);
            fragment.add(label);
            label.setRenderBodyOnly(true);
            view.add(fragment);
        }

        RepeatingView navigationSubtab = new RepeatingView("navigationSubtab");
        add( navigationSubtab );

        boolean first = true;
        for ( String page : navigationModel.getNavigationPages(currentSection) ) {
            if ( first ) {
                first = false;
            } else {
                Fragment fragment = new Fragment(Integer.toString(viewCount++), "navigationSubtabSep", this);
                fragment.setRenderBodyOnly(true);
                navigationSubtab.add( fragment );
            }

            if ( currentPage.equals(page) ) {
                Fragment fragment = new Fragment(Integer.toString(viewCount++), "navigationSubtabOn", this);
                fragment.setRenderBodyOnly(true);
                Label label = new Label("name", new StringResourceModel( "page." + page + ".label", this, null ) );
                label.setRenderBodyOnly(true);
                fragment.add( label );
                navigationSubtab.add( fragment );
            } else {
                Fragment fragment = new Fragment(Integer.toString(viewCount++), "navigationSubtabOff", this);
                fragment.setRenderBodyOnly(true);
                ExternalLink link = new ExternalLink("link", navigationModel.getPageUrlForPage(page));
                fragment.add( link );
                Label label = new Label("name", new StringResourceModel( "page." + page + ".label", this, null ) );
                label.setRenderBodyOnly(true);
                link.add( label );
                navigationSubtab.add( fragment );
            }
        }
    }

    /**
     * Attribute modifier that adds the old and new values together
     */
    private static final class AppendingAttributeModifier extends AttributeModifier {
        public AppendingAttributeModifier(String s, IModel iModel) {
            super(s, iModel);
        }

        @Override
        protected String newValue(String s, String s1) {
            return s + s1;
        }
    }

}
