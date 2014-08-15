Jetty Patch Info

The 6.1.26 version of Jetty is patched to fix the CVE-2011-4461 (a DOS issue)
The original source is available here: http://dist.codehaus.org/jetty/jetty-6.1.26/ and is also checked into our svn: jetty-6.1.26-src.zip

The patch is manually applied from this changeset: https://github.com/eclipse/jetty.project/commit/085c79d7d6cfbccc02821ffdb64968593df3e0bf
The affected files are: 
    jetty\modules\jetty\src\main\java\org\mortbay\jetty\Request.java
    jetty\modules\jetty\src\main\java\org\mortbay\jetty\handler\ContextHandler.java
    jetty\modules\util\src\main\java\org\mortbay\util\UrlEncoded.java
    jetty\modules\util\src\test\java\org\mortbay\util\UrlEncodedTest.java
    
The modified files are zipped here: L7Patch.zip
