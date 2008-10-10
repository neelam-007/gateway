;$Id$
;NSIS Modern User Interface version 1.63
;based on Basic Example Script, which was Written by Joost Verburg

!ifndef J2RE
  !define J2RE "jdk1.6.0_3-windows-i586-p-redist"  ;Name of directory containing JRE to copy from
!endif
!ifndef J2RE_DIR
  ;Windows mapped to drive X:
  !define J2RE_DIR "X:\"
!endif

!define COMPANY "Layer 7 Technologies"
!define MUI_PRODUCT "SecureSpan Gateway" ;Define your own software name here

!ifndef MUI_VERSION
  ; Do not edit this version number
  ; If you want to build a different version use a flag:
  ;  /DMUI_VERSION=XXX
  !define MUI_VERSION "%%%BUILD_VERSION%%%"
!endif

!ifndef BUILD_DIR
  !define BUILD_DIR "..\..\..\build" ;UneasyRooster\build dir, root of jar files and things
!endif

!include "MUI.nsh"

;--------------------------------
;Configuration

  ;General
  OutFile "${MUI_PRODUCT} ${MUI_VERSION} Installer.exe"

  ;Folder selection page
  InstallDir "$PROGRAMFILES\${COMPANY}\${MUI_PRODUCT}"

  ;Remember install folder
  InstallDirRegKey HKLM "Software\${COMPANY}\${MUI_PRODUCT}" ""

;--------------------------------
;Modern UI Configuration

  !define MUI_LICENSEPAGE
 ; !define MUI_COMPONENTSPAGE
  !define MUI_DIRECTORYPAGE
  !define MUI_STARTMENUPAGE

  !define MUI_ABORTWARNING

  !define MUI_UNINSTALLER
  !define MUI_UNCONFIRMPAGE

  !define MUI_HEADERBITMAP "${NSISDIR}\Contrib\Icons\modern-header 2.bmp"

  ;Remember the Start Menu Folder
  !define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKLM"
  !define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\${COMPANY}\${MUI_PRODUCT}"
  !define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "SecureSpan Gateway"

  !define TEMP $R0


;--------------------------------
;Languages

  !insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Language Strings

  ;Description
  LangString DESC_SecCopyUI ${LANG_ENGLISH} "Copy the SecureSpan Gateway files to the application folder."

;--------------------------------
;Data

  LicenseData "License.txt"

  ReserveFile "${NSISDIR}\Contrib\Icons\modern-header 2.bmp"

; checks for non-ascii characters in $INSTDIR [bugzilla #2005]
; "Look at this code, NSIS sucks" --franco
Function CheckInstallDir
  StrCpy $1 $INSTDIR

  Loop:
      StrCpy $2 $1 1
      StrCmp $2 "" Done
      StrCpy $1 $1 "" 1
      ; char value must be between 32 and 126 or " " and "~"
      StrCpy $3 32
      Loop2:
        IntFmt $4 %c $3
        StrCmp $2 $4 Loop
        IntOp $3 $3 + 1
        StrCmp $3 127 badness Loop2

  Goto Loop

  badness:
    MessageBox MB_OK "The character '$2' is not allowed in the installation directory. Aborting installation."
    Abort

  Done:
FunctionEnd

;--------------------------------
;Installer Sections

Section "SecureSpan Gateway" SecCopyUI
  Call CheckInstallDir
  ; check if ssg is already installed
  ReadRegStr ${TEMP} HKLM "Software\${COMPANY}\${MUI_PRODUCT}" ""
  StrCmp ${TEMP} "" cleaninstall
    DetailPrint "existing SSG installation detected at ${TEMP}"
    MessageBox MB_YESNO "The SecureSpan Gateway is already installed on this system. Would you like to stop the SSG and re-install over the existing installation?" IDNO endofinstall
      ; make sure existing ssg is not running before trying to overwrite files (bugzilla #1964)
      ExecWait 'net stop SSG' $0
      DetailPrint "net stop SSG returned with code $0"
      ; delete unwanted portions of the previous install
      IfFileExists "${TEMP}\tomcat\webapps\ROOT" 0 +2
        RMDir /r "${TEMP}\tomcat\webapps\ROOT"
        DetailPrint "Deleted the existing installation directory ${TEMP}\tomcat\webapps\ROOT"

  cleaninstall:

  ; make sure configurable properties files are not overwritten
  IfFileExists "$INSTDIR\etc\conf\hibernate.properties" 0 +2
    CopyFiles "$INSTDIR\etc\conf\hibernate.properties" "$INSTDIR\etc\conf\hibernate.properties.old"
  IfFileExists "$INSTDIR\etc\conf\keystore.properties" 0 +2
    CopyFiles "$INSTDIR\etc\conf\keystore.properties" "$INSTDIR\etc\conf\keystore.properties.old"
  IfFileExists "$INSTDIR\etc\conf\ssglog.properties" 0 +2
    CopyFiles "$INSTDIR\etc\conf\ssglog.properties" "$INSTDIR\etc\conf\ssglog.properties.old"
  IfFileExists "$INSTDIR\etc\conf\system.properties" 0 +2
    CopyFiles "$INSTDIR\etc\conf\system.properties" "$INSTDIR\etc\conf\system.properties.old"
  IfFileExists "$INSTDIR\jdk\jre\lib\security\java.security" 0 +2
    CopyFiles "$INSTDIR\jdk\jre\lib\security\java.security" "$INSTDIR\jdk\jre\lib\security\java.security.old"

  CreateDirectory "$INSTDIR\logs"
  CreateDirectory "$INSTDIR\bin"
  CreateDirectory "$INSTDIR\etc\conf"
  CreateDirectory "$INSTDIR\modules\lib"
  CreateDirectory "$INSTDIR\modules\assertions"

  SetOutPath "$INSTDIR"
  File /r "${BUILD_DIR}\install\ssg\tomcat"
  ;Windows mapped drive X:
  File /r "${J2RE_DIR}${J2RE}"
  Rename "$INSTDIR\${J2RE}" "$INSTDIR\jdk"
  ;etc/install.properties not having version as suffix to jdk 
  ;File /r "${BUILD_DIR}\install\ssg\jdk" this would include the linux jvm
  ; Windows installer has to remove the tarari_raxj.jar file
  Delete "$INSTDIR\tomcat\webapps\ROOT\WEB-INF\lib\tarari_raxj.jar"

  SetOutPath "$INSTDIR/bin"
  File "${BUILD_DIR}\..\native\win32\uptime\Release\uptime.exe"
  File "${BUILD_DIR}\..\native\win32\process\Release\process.exe"
  File "${BUILD_DIR}\..\native\win32\killproc\Release\killproc.exe"
  File "${BUILD_DIR}\..\native\win32\sysmem\Release\sysmem.exe"
  File "${BUILD_DIR}\..\etc\ssg.cmd"
  File "${BUILD_DIR}\..\etc\service.cmd"
  File "${BUILD_DIR}\..\etc\cleanup_services.cmd"
  File "${BUILD_DIR}\..\etc\remove_service.cmd"
  File "${BUILD_DIR}\..\etc\SSG.exe"
  File "${BUILD_DIR}\..\etc\ssgruntimedefs.cmd"
  File "${BUILD_DIR}\..\etc\GetShortName.cmd"

  SetOutPath "$INSTDIR/etc"
  File /r "${BUILD_DIR}\install\ssg\etc\conf"
  File /r "${BUILD_DIR}\install\ssg\etc\sql"
  File /r "${BUILD_DIR}\install\ssg\etc\ldapTemplates"

  SetOutPath "$INSTDIR/etc/conf"
  File "${BUILD_DIR}\..\etc\db\mysql\my.ini"

  SetOutPath "$INSTDIR/modules/lib"
  File "${BUILD_DIR}\ssg-uddi-module-systinetv3.jar"

  SetOutPath "$INSTDIR/modules"
  File /r "${BUILD_DIR}\install\ssg\modules\assertions"

  SetOutPath "$INSTDIR"
  File /r "${BUILD_DIR}\install\ssg\configwizard"
  File /r "${BUILD_DIR}\install\ssg\migration"

  SetOutPath "$INSTDIR/configwizard"
  File "${BUILD_DIR}\..\installer\server\win32\SecureSpanConfigWizard.exe"
  File "${BUILD_DIR}\..\installer\server\win32\SecureSpanConfigWizard.ini"

  ;Store install folder
  WriteRegStr HKLM "Software\${COMPANY}\${MUI_PRODUCT}" "" $INSTDIR

  !insertmacro MUI_STARTMENU_WRITE_BEGIN

  ; Create shortcuts
  CreateDirectory "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}"
  CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Uninstall SecureSpan Gateway.lnk" "$INSTDIR\Uninstall.exe"
  SetOutPath "$INSTDIR/configwizard"
  CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Configure SecureSpan Gateway.lnk" "$INSTDIR\configwizard\SecureSpanConfigWizard.exe"
  SetOutPath "$INSTDIR"

  !insertmacro MUI_STARTMENU_WRITE_END

  ;Register with Add/Remove programs
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT}" "DisplayName" "${MUI_PRODUCT} ${MUI_VERSION}"
  ; fix for bugzilla #3007
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT}" "DisplayIcon" "$INSTDIR\configwizard\SecureSpanConfigWizard.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT}" "UninstallString" "$INSTDIR\Uninstall.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT}" "Publisher" "${COMPANY}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT}" "URLInfoAbout" "http://www.layer7tech.com"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT}" "DisplayVersion" "${MUI_VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT}" "NoModify" "1"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT}" "NoRepair" "1"
  ; remove previous versions entries here if any so that only the latest version appears in control panel
  ; only version pre-3.5 need be listed here
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} 3.4.1"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} 3.4"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} 3.1"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} 3.0"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} HEAD"

  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"

  ; substitute overwritten config files
  IfFileExists "$INSTDIR\etc\conf\hibernate.properties.old" 0 +4
    CopyFiles "$INSTDIR\etc\conf\hibernate.properties" "$INSTDIR\etc\conf\hibernate.properties.new"
    CopyFiles "$INSTDIR\etc\conf\hibernate.properties.old" "$INSTDIR\etc\conf\hibernate.properties"
    Delete "$INSTDIR\etc\conf\hibernate.properties.old"
  IfFileExists "$INSTDIR\etc\conf\keystore.properties.old" 0 +4
    CopyFiles "$INSTDIR\etc\conf\keystore.properties" "$INSTDIR\etc\conf\keystore.properties.new"
    CopyFiles "$INSTDIR\etc\conf\keystore.properties.old" "$INSTDIR\etc\conf\keystore.properties"
    Delete "$INSTDIR\etc\conf\keystore.properties.old"
  IfFileExists "$INSTDIR\etc\conf\ssglog.properties.old" 0 +4
    CopyFiles "$INSTDIR\etc\conf\ssglog.properties" "$INSTDIR\etc\conf\ssglog.properties.new"
    CopyFiles "$INSTDIR\etc\conf\ssglog.properties.old" "$INSTDIR\etc\conf\ssglog.properties"
    Delete "$INSTDIR\etc\conf\ssglog.properties.old"
  IfFileExists "$INSTDIR\etc\conf\system.properties.old" 0 +4
    CopyFiles "$INSTDIR\etc\conf\system.properties" "$INSTDIR\etc\conf\system.properties.new"
    CopyFiles "$INSTDIR\etc\conf\system.properties.old" "$INSTDIR\etc\conf\system.properties"
    Delete "$INSTDIR\etc\conf\system.properties.old"
  IfFileExists "$INSTDIR\tomcat\conf\server.xml.old" 0 +4
    CopyFiles "$INSTDIR\tomcat\conf\server.xml" "$INSTDIR\tomcat\conf\server.xml.new"
    CopyFiles "$INSTDIR\tomcat\conf\server.xml.old" "$INSTDIR\tomcat\conf\server.xml"
    Delete "$INSTDIR\tomcat\conf\server.xml.old"
  IfFileExists "$INSTDIR\jdk\jre\lib\security\java.security.old" 0 +4
    CopyFiles "$INSTDIR\jdk\jre\lib\security\java.security" "$INSTDIR\jdk\jre\lib\security\java.security.new"
    CopyFiles "$INSTDIR\jdk\jre\lib\security\java.security.old" "$INSTDIR\jdk\jre\lib\security\java.security"
    Delete "$INSTDIR\jdk\jre\lib\security\java.security.old"

  ; run the gateway configurator
  SetOutPath "$INSTDIR/configwizard"
  ExecWait '"$INSTDIR\configwizard\ssgconfig.cmd" -partitionMigrate' $0
  DetailPrint "configwizard partition migration returned with code $0"

 ; install the service. We do this here so that the default_ partition already exists.
  ExecWait '"$INSTDIR\etc\conf\partitions\default_\service.cmd" install' $0
  DetailPrint "service.cmd install returned with code $0"
  
  ExecWait '"$INSTDIR\configwizard\ssgconfig.cmd"' $0
  DetailPrint "configwizard returned with code $0"

  ; ask user if he wants the service to be started now
  MessageBox MB_YESNO "Do you want to start the SecureSpan Gateway service now?" IDNO endofinstall

  ExecWait 'net start SSG' $0
  DetailPrint "net start SSG returned with code $0"

  endofinstall:
SectionEnd

;Display the Finish header
;Insert this macro after the sections if you are not using a finish page
!insertmacro MUI_SECTIONS_FINISHHEADER

;--------------------------------
;Descriptions

!insertmacro MUI_FUNCTIONS_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecCopyUI} $(DESC_SecCopyUI)
!insertmacro MUI_FUNCTIONS_DESCRIPTION_END

;--------------------------------
;Uninstaller Section

Section "Uninstall"

  ExecWait 'net stop SSG' $0
  DetailPrint "net stop SSG returned with code $0"

  ExecWait '"$INSTDIR\etc\conf\partitions\default_\service.cmd" uninstall' $0
  DetailPrint "service.cmd uninstall returned with code $0"

  ;clean up the services created since partitioning may have created many
  ExecWait '"$INSTDIR\bin\cleanup_services.cmd"' $0
  DetailPrint "SSG Services removal returned with code $0"

  ;Disabled: Might erase entire disk if INSTDIR is empty -- see Bug #5544
  ;RMDir /r "$INSTDIR"

  ; Remove shortcut
  ReadRegStr ${TEMP} "${MUI_STARTMENUPAGE_REGISTRY_ROOT}" "${MUI_STARTMENUPAGE_REGISTRY_KEY}" "${MUI_STARTMENUPAGE_REGISTRY_VALUENAME}"
  StrCmp ${TEMP} "" noshortcuts
  Delete "$SMPROGRAMS\${TEMP}\Uninstall SecureSpan Gateway.lnk"
  Delete "$SMPROGRAMS\${TEMP}\Configure SecureSpan Gateway.lnk"
  RMDir "$SMPROGRAMS\${TEMP}" ; Only if empty, so it won't delete other shortcuts

  noshortcuts:

  DeleteRegKey /ifempty HKLM "Software\${COMPANY}\${MUI_PRODUCT}"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT}"

  RMDir "$PROGRAMFILES\${COMPANY}"

  ;Display the Finish header
  !insertmacro MUI_UNFINISHHEADER

  IfRebootFlag 0 noreboot
    MessageBox MB_YESNO "A reboot is required to complete uninstallation. Do you wish to reboot now?" IDNO noreboot
  noreboot:
  
SectionEnd
