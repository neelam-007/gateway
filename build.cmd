@echo off
rem -------
rem Build Script for UneasyRooster 
rem -------
setlocal
if "%SRC_ROOT%" == "" goto srcRootMissing
if "%JAVA_HOME%" == "" goto javaHomeMissing
rem - JDK classes -
set JDK_CLASSES=%JAVA_HOME%\jre\lib\rt.jar;%JAVA_HOME%\lib\tools.jar

set CLASSPATH=%JDK_CLASSES%;%CLASSPATH%
:antclasspath
set CLASSPATH=%SRC_ROOT%\lib\ant.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\ant-launcher.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\ant-nodeps.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\xercesImpl.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\xml-apis.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\junit.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\tools\junit-4.4.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\tools\ant-trax.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\ant-junit.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\ant-contrib-1.0b3.jar;%CLASSPATH%
set CLASSPATH=%SRC_ROOT%\lib\xalan-2.5.2.jar;%CLASSPATH%
rem echo %CLASSPATH%
rem Execute ANT to perform the requested build target
java org.apache.tools.ant.Main -Dsrc.root="%SRC_ROOT%" %1 %2 %3 %4 %5
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
:end
endlocal
