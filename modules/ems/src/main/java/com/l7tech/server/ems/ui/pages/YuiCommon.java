package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;

import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/**
 * YuiCommon resources and functions.
 */
class YuiCommon {

    //- PACKAGE

    static final String RES_CSS_SAM_BUTTON = "yui/button/assets/skins/sam/button.css";
    static final String RES_CSS_SAM_MENU = "yui/menu/assets/skins/sam/menu.css";
    static final String RES_CSS_SAM_DATATABLE = "yui/datatable/assets/skins/sam/datatable.css";
    static final String RES_CSS_SAM_CALENDAR = "yui/calendar/assets/skins/sam/calendar.css";
    static final String RES_CSS_SAM_CONTAINER = "yui/container/assets/skins/sam/container.css";
    static final String RES_CSS_SAM_SKIN = "yui/assets/skins/sam/skin.css";

    static final String RES_JS_DOM_EVENT = "yui/yahoo-dom-event/yahoo-dom-event.js";
    static final String RES_JS_ANIMATION = "yui/animation/animation-min.js";
    static final String RES_JS_CONTAINER = "yui/container/container-min.js";
    static final String RES_JS_ELEMENT = "yui/element/element-beta-min.js";
    static final String RES_JS_BUTTON = "yui/button/button-min.js";
    static final String RES_JS_MENU = "yui/menu/menu-min.js";
    static final String RES_JS_DATASOURCE = "yui/datasource/datasource-min.js";
    static final String RES_JS_DRAGDROP = "yui/dragdrop/dragdrop-min.js";
    static final String RES_JS_DATATABLE = "yui/datatable/datatable-min.js";
    static final String RES_JS_PAGINATOR = "yui/paginator/paginator-min.js";
    static final String RES_JS_EVENT = "yui/event/event-min.js";
    static final String RES_JS_CONNECTION = "yui/connection/connection-min.js";
    static final String RES_JS_JSON = "yui/json/json-min.js";
    static final String RES_JS_DOM = "yui/dom/dom-min.js";
    static final String RES_JS_CALENDAR = "yui/calendar/calendar-min.js";
    static final String RES_JS_LOGGER = "yui/logger/logger-min.js";

    static String toYuiDate( final Date date, final String timeZoneId ) {
        SimpleDateFormat format = new SimpleDateFormat(YUI_DATE_FORMAT);

        if ( timeZoneId != null ) {
            format.setTimeZone( TimeZone.getTimeZone( timeZoneId ) );
        }
        
        return format.format( date );
    }

    //- PRIVATE

    private static final String YUI_DATE_FORMAT = "MM/dd/yyyy";
}
