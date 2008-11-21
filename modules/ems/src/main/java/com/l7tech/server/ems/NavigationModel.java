package com.l7tech.server.ems;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.Page;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l7tech.util.Functions;

/**
 * Model for site sections and pages. 
 * 
 * @author steve
 */
public class NavigationModel implements Serializable {

    //- PUBLIC

    /**
     * Build a navigation model using the "page.index" in the named package.
     *
     * @param packageName The name of the package containing pages.
     */
    public NavigationModel( final String packageName ) {
        this( Package.getPackage(packageName), null );
    }
    
    /**
     * Build a navigation model using the "page.index" in the named package.
     *
     * @param pagePackage The package containing pages.
     */
    public NavigationModel( final Package pagePackage ) {
        this( Collections.singleton( pagePackage ), null );
    }
    
    /**
     * Build a navigation model using the "page.index" in the named packages.
     *
     * @param packages The packages containing pages.
     */
    public NavigationModel( final Collection<Package> packages ) {
        this( packages, null );
    }

    // new Functions.Unary<Boolean,Class<? extends Page>>

    /**
     * Build a navigation model using the "page.index" in the named package.
     *
     * @param packageName The name of the package containing pages.
     * @param pageChecker Callback for verification of access to a page.
     */
    public NavigationModel( final String packageName, final Functions.Unary<Boolean,Class<? extends Page>> pageChecker ) {
        this( Package.getPackage(packageName), pageChecker );
    }

    /**
     * Build a navigation model using the "page.index" in the named package.
     *
     * @param pagePackage The package containing pages.
     * @param pageChecker Callback for verification of access to a page.
     */
    public NavigationModel( final Package pagePackage, final Functions.Unary<Boolean,Class<? extends Page>> pageChecker  ) {
        this( Collections.singleton( pagePackage ), pageChecker );
    }

    /**
     * Build a navigation model using the "page.index" in the named packages.
     *
     * @param packages The packages containing pages.
     * @param pageChecker Callback for verification of access to a page.
     */
    public NavigationModel( final Collection<Package> packages, final Functions.Unary<Boolean,Class<? extends Page>> pageChecker  ) {
        pages = loadPages( packages, pageChecker );
    }

    /**
     * Get the names of the navigation sections.
     *
     * @return The sections in order.
     */
    public Collection<String> getNavigationSections() {
        Set<String> sections = new LinkedHashSet<String>();
        
        for ( NavigationPageHolder holder : pages ) {
            sections.add( holder.page.section() );
        }
        
        return sections;
    } 
    
    /**
     * Get the names of the navigation pages.
     *
     * @return The pages in section/page order.
     */
    public Collection<String> getNavigationPages() {
        Set<String> pageNames = new LinkedHashSet<String>();

        for ( NavigationPageHolder holder : pages ) {
            pageNames.add( holder.page.page() );
        }

        return pageNames;
    }

    /**
     * Get the names of the navigation pages in a section.
     *
     * @param section The section of interest.
     * @return The pages in order.
     */
    public Collection<String> getNavigationPages( final String section ) {
        Set<String> pageNames = new LinkedHashSet<String>();
        
        for ( NavigationPageHolder holder : pages ) {
            if ( section.equals( holder.page.section() ) ) {
                pageNames.add( holder.page.page() );
            }
        }
        
        return pageNames;
    } 

    /**
     * Get the section of the given page.
     *
     * @param page The page of interest.
     * @return The section name or null
     */
    public String getNavigationSectionForPage( final String page ) {
        String section = null;

        for ( NavigationPageHolder holder : pages  ) {
            if ( holder.page.page().equals(page) ) {
                section = holder.page.section();
                break;
            }
        }

        return section;
    }

    /**
     * Get the URL of the given page.
     *
     * @param page The page of interest.
     * @return The URL or null
     */
    public String getPageUrlForPage( final String page ) {
        String pageUrl = null;

        for ( NavigationPageHolder holder : pages  ) {
            if ( holder.page.page().equals(page) ) {
                pageUrl = holder.page.pageUrl();
                break;
            }
        }

        return pageUrl;
    }

    /**
     * Get the Class of the given page.
     *
     * @param page The page of interest.
     * @return The Class or null
     */
    public Class<WebPage> getPageClassForPage( final String page ) {
        Class<WebPage> pageClass = null;

        for ( NavigationPageHolder holder : pages  ) {
            if ( holder.page.page().equals(page) ) {
                pageClass = holder.pageClass;
                break;
            }
        }

        return pageClass;
    }

    /**
     * Get the URL for the landing page of the given section.
     *
     * @param section The section of interest.
     * @return The URL or null
     */
    public String getPageUrlForSection( final String section ) {
        String pageUrl = null;

        // see if there is a specified default
        for ( NavigationPageHolder holder : pages  ) {
            if ( holder.page.section().equals(section) ) {
                pageUrl = getPageUrlForPage( holder.page.sectionPage() );
                break;
            }
        }

        if ( pageUrl == null ) {
            // find URL for first page in section
            for ( NavigationPageHolder holder : pages  ) {
                if ( holder.page.section().equals(section) ) {
                    pageUrl = holder.page.pageUrl();
                    break;
                }
            }
        }

        return pageUrl;
    }

    //- PRIVATE
    
    private static final Logger logger = Logger.getLogger( NavigationPage.class.getName() );
    private static final String PAGES_INDEX = "pages.index";
    
    private final Collection<NavigationPageHolder> pages;
    
    @SuppressWarnings({"unchecked"})
    private Collection<NavigationPageHolder> loadPages( final Collection<Package> packages, final Functions.Unary<Boolean,Class<? extends Page>> pageChecker ) {
        Set<NavigationPageHolder> pages = new TreeSet<NavigationPageHolder>();
        
        for ( Package pagePackage : packages ) {
            URL pagesResource = NavigationModel.class.getResource("/" + pagePackage.getName().replace('.', '/') + "/" + PAGES_INDEX);
            if ( pagesResource == null ) {
                logger.warning("No pages.index found for package '"+pagePackage.getName()+"'.");
                continue;
            }
            
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pagesResource.openStream()));
                String pageLine;
                while ( (pageLine = reader.readLine()) != null ) {
                    Class<WebPage> pageClass = (Class<WebPage>) Class.forName( pagePackage.getName() + "." + pageLine );
                    if ( pageChecker == null || pageChecker.call(pageClass) ) {
                        NavigationPage navPage = pageClass.getAnnotation(NavigationPage.class);
                        if ( navPage != null ) {
                            pages.add( new NavigationPageHolder(navPage, pageClass) );
                        }
                    }
                }
            } catch ( IOException ioe ) {
                logger.log( Level.WARNING, "Error loading navigation pages.", ioe );
            } catch ( ClassNotFoundException cnfe ) {
                logger.log( Level.WARNING, "Error loading navigation pages.", cnfe );
            }
        }
        
        return Collections.unmodifiableCollection(pages);
    }
    
    /**
     * Holder for NavigationPage annotations that adds ordering
     */
    private static final class NavigationPageHolder implements Comparable, Serializable {
        final NavigationPage page;
        final Class<WebPage> pageClass;
        
        NavigationPageHolder( final NavigationPage page, final Class<WebPage> pageClass ) {
            if ( page == null ) throw new IllegalArgumentException("page is required");
            this.page = page;
            this.pageClass = pageClass;
        }

        /**
         * Order is by section index, section name, page index, page name.
         */
        @Override
        public int compareTo( final Object pageHolderObject ) {
            NavigationPageHolder otherPage = (NavigationPageHolder) pageHolderObject;
            
            int result = Integer.valueOf(this.page.sectionIndex()).compareTo(otherPage.page.sectionIndex());
            if ( result == 0 ) {
                result = this.page.section().compareTo(otherPage.page.section());
            }
            if ( result == 0 ) {
                result = Integer.valueOf(this.page.pageIndex()).compareTo(otherPage.page.pageIndex());
            }
            if ( result == 0 ) {
                result = this.page.page().compareTo(otherPage.page.page());
            }
            
            return result;
        }
        
        @SuppressWarnings({"RedundantIfStatement"})
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NavigationPageHolder other = (NavigationPageHolder) obj;
            if (this.page != other.page && (this.page == null || !this.page.equals(other.page))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 41 * hash + (this.page != null ? this.page.hashCode() : 0);
            return hash;
        }
    }
}
