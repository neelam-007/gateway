@echo off
rem -------
rem Build Script for UneasyRooster 
rem -------
setlocal
if .%SRC_ROOT% == . goto srcRootMissing
if .%JAVA_HOME% == . goto javaHomeMissing
if .%TOMCAT_HOME% == . goto tomcatHomeMissing
rem - JDK classes -
set JDK_CLASSES=%JAVA_HOME%\jre\lib\rt.jar;%JAVA_HOME%\lib\tools.jar

set CLASSPATH=%JDK_CLASSES%;%CLASSPATH%
:antclasspath
set CLASSPATH=%SRC_ROOT%\lib\ant.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\optional.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\xercesImpl.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\xml-apis.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\junit.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\xmltask.jar;%CLASSPATH%
rem echo %CLASSPATH%
rem Execute ANT to perform the requested build target
java org.apache.tools.ant.Main -Dsrc.root=%SRC_ROOT% -Dtomcat.home=%TOMCAT_HOME% %1 %2 %3 %4 %5
goto end
:javaHomeMissing
@echo The environment variable JAVA_HOME is not present or invalid. Must point to the
@echo valid JDK home directory.
@echo Stop.
goto end
:srcRootmissing
@echo The environment variable SRC_ROOT is not present or invalid. Must point to the
@echo valid source root directory.
@echo Stop.
goto end
:tomcatHomeMissing
@echo The environment variable TOMCAT_HOME is not present or invalid. Must point to the
@echo valid tomcat root directory.
@echo Stop.
goto end
:end
endlocal


