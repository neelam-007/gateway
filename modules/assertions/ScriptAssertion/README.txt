
This is an .AAR file that should work with 4.2 and later Gateways that allows you to do arbitrary processing in
Javascript.  It uses the Bean Scripting Framework (BSF) so it's trivial to add support for other languages including
Ruby, TCL, Python, Groovy, or Java but I left them out for now to keep down the size of the .AAR file.  I included a
screenshot so you can see how it works without having to find a Gateway to dump the .AAR file onto.  I've tested it on
my own workstation, but not on a production build.

Be warned: scripts have complete, unfettered access to the system.  Don't install this .AAR file on any system where the
local security policy wouldn't be happy with policy admins (or even random clients, if a script is sloppily written)
being able to run arbitrary code on the Gateway at will.

When you pick a language from the drop-down it populates the text box with some example code so you can see roughly
what to do.  Unfortunately there is no actual documentation.  The Javascript engine is Rhino, so you can use any class
in the system by prefixing the full classname with "Package.".  In addition, the following four object references are
predefined for the script:

  assertion     the ScriptAssertion instance where you script came from

  policyContext the PolicyEnforcementContext, containing the request, response, context variables etc.

  auditor       the Auditor, in case you need to log/audit anything

  appContext    the Spring ApplicationContext, for access to the rest of the system (database, management, admin, etc)

Unfortunately there appears currently to be no way to precompile the script and call it with different arguments each
time so it has to eval every time and is likely to be fairly slow.  (There's some compile* methods but currently they
produce bytecode that, for each invocation, just creates a new engine and calls eval!?)

The policy validator currently does not warn you about errors in your script.  You have to actually send through a
request to find the errors.  This is tough to fix completely since to do a proper test you need to simulate the full
object model of a running Gateway (a problem with dynamic languages).

To enable this to work on 4.2 I had to hack the bsf.jar to work around a bug it exposed in the AAR file classloader.
(I've fixed the bug for 4.3.)
