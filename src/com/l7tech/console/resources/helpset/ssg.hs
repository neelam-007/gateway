<?xml version='1.0' encoding='ISO-8859-1' ?>
<helpset version="1.0">

<!--title -->
<title>Layer7 SSG Management Console Help</title>

<!--maps-->
<maps>
    <homeID>using_console</homeID>
    <mapref location="Map.jhm"/>
</maps>

<!-- view -->
<view>
    <name>TOC</name>
    <label>SSG Management Console Help</label>
    <type>javax.help.TOCView</type>
    <data>ssgToc.xml</data>
</view>

<view>
    <name>Index</name>
    <label>Index</label>
    <type>javax.help.IndexView</type>
    <data>ssgHelpIndex.xml</data>
</view>

<view>
    <name>Search</name>
    <label>Serach</label>
    <type>javax.help.SearchView</type>
    <data engine="com.sun.java.help.search.DefaultSearchEngine">
        JavaHelpSearch
    </data>
</view>
</helpset>
