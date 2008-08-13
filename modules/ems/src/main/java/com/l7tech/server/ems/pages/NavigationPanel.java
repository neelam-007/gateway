package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.AttributeModifier;

import java.util.Set;
import java.util.LinkedHashSet;

/**
 * Panel for main navigation.
 */
public class NavigationPanel extends Panel {

    /**
     * Page metadata.
     */
    private static final String[][] pages = {
        { "EnterpriseGateways", "Enterprise", "EnterpriseGateways.html" },
        { "EnterpriseUsers", "Enterprise", "EnterpriseUsers.html" },

        { "PolicyMigration", "Policies", "PolicyMigration.html" },
        { "PolicySubmission", "Policies", "PolicySubmission.html" },
        { "PolicyApproval", "Policies", "PolicyApproval.html" },
        { "PolicyMapping", "Policies", "PolicyMapping.html" },

        { "StandardReports", "Reports", "StandardReports.html" },
        { "ExternalReports", "Reports", "ExternalReports.html" },

        { "Backup", "Backup", "Backup.html" },
        { "Restore", "Backup", "Restore.html" },

        { "Messages", "Tools", "Messages.html" },
        { "Audits", "Tools", "Audits.html" },
        { "Logs", "Tools", "Logs.html" },
        { "TestWebService", "Tools", "TestWebService.html" },

        { "UserSettings", "Settings", "UserSettings.html" },
        { "SystemSettings", "Settings", "SystemSettings.html" },
    };

    /**
     * The model must be the page being rendered.
     */
    public NavigationPanel( final String id, final IModel model ) {
        super(id, model);

        String currentSection = pages[0][1];
        String page = ((EmsPage)model.getObject()).getPageName();
        page = page.substring(page.lastIndexOf('.')+1);
        String section;
        Set<String> sectionSet = new LinkedHashSet<String>();
        for ( String[] pageInfo : pages  ) {
            if ( page.equals(pageInfo[0]) ) {
                currentSection = pageInfo[1];
            }
            sectionSet.add(pageInfo[1]);
        }

        RepeatingView navigationTopContainer = new RepeatingView("navigationTabTop");
        navigationTopContainer.setRenderBodyOnly(true);
        add( navigationTopContainer );
        section  = null;
        int viewCount = 0;
        for ( String[] pageInfo : pages ) {
            if (!pageInfo[1].equals(section)) {
                section = pageInfo[1];
                Fragment fragment = new Fragment(Integer.toString(viewCount++), "navigationTabTopFragment", this );
                fragment.setRenderBodyOnly(true);
                fragment.add( new WebMarkupContainer("idSection1").add(new AppendingAttributeModifier("id", new Model(section))) );
                fragment.add( new WebMarkupContainer("idSection2").add(new AppendingAttributeModifier("id", new Model(section))) );
                fragment.add( new WebMarkupContainer("idSection3").add(new AppendingAttributeModifier("id", new Model(section))) );
                navigationTopContainer.add( fragment );
            }
        }

        RepeatingView navigationMiddleContainer = new RepeatingView("navigationTabMiddle");
        navigationMiddleContainer.setRenderBodyOnly(true);
        add( navigationMiddleContainer );
        section  = null;
        for ( String[] pageInfo : pages ) {
            if (!pageInfo[1].equals(section)) {
                section = pageInfo[1];
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
                    ExternalLink link = new ExternalLink("link", pageInfo[2]);
                    td.add( link );
                    Label label = new Label("name", new StringResourceModel( "section." + section + ".label", this, null ) );
                    label.setRenderBodyOnly(true);
                    link.add( label );
                    fragment.setRenderBodyOnly(true);
                    fragment.add( new WebMarkupContainer("idSection3").add(new AppendingAttributeModifier("id", new Model(section))) );
                    navigationMiddleContainer.add( fragment );
                }
            }
        }

        RepeatingView navigationBottomContainer = new RepeatingView("navigationTabBottom");
        navigationBottomContainer.setRenderBodyOnly(true);
        add( navigationBottomContainer );
        section  = null;
        for ( String[] pageInfo : pages ) {
            if (!pageInfo[1].equals(section)) {
                section = pageInfo[1];
                Fragment fragment = new Fragment(Integer.toString(viewCount++), currentSection.equals(section) ? "navigationTabBottomFragmentOn" : "navigationTabBottomFragmentOff", this );
                fragment.setRenderBodyOnly(true);
                fragment.add( new WebMarkupContainer("idSection1").add(new AppendingAttributeModifier("id", new Model(section))) );
                fragment.add( new WebMarkupContainer("idSection2").add(new AppendingAttributeModifier("id", new Model(section))) );
                fragment.add( new WebMarkupContainer("idSection3").add(new AppendingAttributeModifier("id", new Model(section))) );
                navigationBottomContainer.add( fragment );
            }
        }

        RepeatingView view = new RepeatingView("navigationScript");
        add( view );
        Set<String> items = new LinkedHashSet<String>(sectionSet);
        items.remove(currentSection);
        for ( String scriptSection : items ) {
            Label label = new Label("tabName", scriptSection);
            Fragment fragment = new Fragment(Integer.toString(viewCount++), "navigationScriptFragment", this);
            fragment.add(label);
            label.setRenderBodyOnly(true);
            view.add(fragment);
        }

        RepeatingView navigationSubtab = new RepeatingView("navigationSubtab");
        add( navigationSubtab );

        boolean first = true;
        for ( String[] pageInfo : pages ) {
            if ( pageInfo[1].equals(currentSection) ) {
                if ( first ) {
                    first = false;
                } else {
                    Fragment fragment = new Fragment(Integer.toString(viewCount++), "navigationSubtabSep", this);
                    fragment.setRenderBodyOnly(true);
                    navigationSubtab.add( fragment );
                }

                if ( page.equals(pageInfo[0]) ) {
                    Fragment fragment = new Fragment(Integer.toString(viewCount++), "navigationSubtabOn", this);
                    fragment.setRenderBodyOnly(true);
                    Label label = new Label("name", new StringResourceModel( "page." + pageInfo[0] + ".label", this, null ) );
                    label.setRenderBodyOnly(true);
                    fragment.add( label );
                    navigationSubtab.add( fragment );
                } else {
                    Fragment fragment = new Fragment(Integer.toString(viewCount++), "navigationSubtabOff", this);
                    fragment.setRenderBodyOnly(true);
                    ExternalLink link = new ExternalLink("link", pageInfo[2]);
                    fragment.add( link );
                    Label label = new Label("name", new StringResourceModel( "page." + pageInfo[0] + ".label", this, null ) );
                    label.setRenderBodyOnly(true);
                    link.add( label );
                    navigationSubtab.add( fragment );
                }
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

        protected String newValue(String s, String s1) {
            return s + s1;
        }
    }

}
