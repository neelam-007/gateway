This is the Win32 bootstrap utility.  If you load this project into VC.NET
and build the Startup.exe, then copy it to "f:\blah\blee bar\mumble foo.exe"
and then run it, it will do the following:

  - Look for a file "f:\blah\blee bar\mumble foo.ini" which should contain:
       [startup]
       jre=j2re1.4.2\bin\javaw.exe
       commandline=javaw -jar ClientProxy.jar
    (but with no whitespace at the front of the lines).

    If this file exists and has the proper format, Starter will run the
    specified executable passing it the specified command line.


  - Otherwise, it tries to run the executable "jre\bin\javaw.exe" with
    the command line "javaw -jar Program.jar".

  - If the program starts, Starter itself will immediately and silently
    exit.  Bug: it will silently exit even if the JVM immediately dies
    with an error of some kind; it's error will get printed to the 
    (non-existent) console and hence will go nowhere.

  - If the CreateProcess call fails, Starter will display a generic
    error message and then exit.



