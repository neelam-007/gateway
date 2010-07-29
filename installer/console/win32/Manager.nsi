;NSIS Modern User Interface version 1.63
;based on Basic Example Script, which was Written by Joost Verburg

!ifndef J2RE
  !define J2RE "jre1.6.0_01"  ;Name of directory containing JRE to copy from, usually passed from build script
!endif
!ifndef J2RE_DIR
  ;Windows mapped to drive X:
  !define J2RE_DIR "X:\"
!endif
!ifndef J2RE_PATH
  !define J2RE_PATH "${J2RE_DIR}${J2RE}"   ;Full path to directory containing JRE (at .nsi compile-time)
!endif
!define COMPANY "Layer 7 Technologies"
!define MUI_PRODUCT "Layer 7 Policy Manager" ;Define your own software name here

!ifndef MUI_VERSION
  ; Do not edit this version number
  ; If you want to build a different version use a flag:
  ;  /DMUI_VERSION=XXX 
  !define MUI_VERSION "%%%BUILD_VERSION%%%"
!endif

!ifndef BUILD_DIR
  !define BUILD_DIR "..\..\..\build" ;UneasyRooster\build dir, root of jar files and things
!endif

!ifndef PACKAGE_REL
  !define PACKAGE_REL "..\installer\Manager-${MUI_VERSION}"
!endif

!ifndef PACKAGE_DIR
  !define PACKAGE_DIR "${BUILD_DIR}\${PACKAGE_REL}"
!endif

!ifndef OUTPUT_DIR
  !define OUTPUT_DIR "."
!endif

!include "MUI.nsh"

;--------------------------------
;Configuration

  ;General
  OutFile "${OUTPUT_DIR}\${MUI_PRODUCT} ${MUI_VERSION} Installer.exe"

  ;Folder selection page
  InstallDir "$PROGRAMFILES\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}"
  
  ;Remember install folder
  InstallDirRegKey HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}" ""

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
  !define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKCU" 
  !define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}"
  !define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Layer 7 ${MUI_PRODUCT}"

  !define TEMP $R0


;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"
  
;--------------------------------
;Language Strings

  ;Description
  LangString DESC_SecCopyUI ${LANG_ENGLISH} "Copy the Policy Editor files to the application folder."

;--------------------------------
;Data
  
  LicenseData "License.txt"

  ReserveFile "${NSISDIR}\Contrib\Icons\modern-header 2.bmp"

; checks for previous installations and warns user if detected
Function CheckPreviousInstalls

  ReadRegStr ${TEMP} HKCU "Software\${COMPANY}\${MUI_PRODUCT}" ""
  StrCmp ${TEMP} "" 0 foundpreviousinstall

  ReadRegStr ${TEMP} HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}" ""
  StrCmp ${TEMP} "" 0 foundpreviousinstall

  ReadRegStr ${TEMP} HKCU "Software\${COMPANY}\${MUI_PRODUCT} 3.4.1" ""
  StrCmp ${TEMP} "" 0 foundpreviousinstall

  ReadRegStr ${TEMP} HKCU "Software\${COMPANY}\${MUI_PRODUCT} 3.4" ""
  StrCmp ${TEMP} "" 0 foundpreviousinstall

  ReadRegStr ${TEMP} HKCU "Software\${COMPANY}\${MUI_PRODUCT} 3.1" ""
  StrCmp ${TEMP} "" 0 foundpreviousinstall

  ReadRegStr ${TEMP} HKCU "Software\${COMPANY}\${MUI_PRODUCT} 3.0" ""
  StrCmp ${TEMP} "" 0 foundpreviousinstall

  ReadRegStr ${TEMP} HKCU "Software\${COMPANY}\${MUI_PRODUCT} HEAD" ""
  StrCmp ${TEMP} "" 0 foundpreviousinstall

  DetailPrint "No previous installation of ${MUI_PRODUCT} detected."
  Goto done

  foundpreviousinstall:
    DetailPrint "existing ${MUI_PRODUCT} installation detected at ${TEMP}"
    MessageBox MB_YESNO "The ${MUI_PRODUCT} appears to be already installed on this system at location ${TEMP}. Do you want to continue anyway?" IDNO abortinstall
      MessageBox MB_OK "Make sure the current version is not running and click OK to continue."
        DetailPrint "Previous version of product detected but user chooses to proceed anyway."
        Goto done

  abortinstall:
    DetailPrint "Installation of ${MUI_PRODUCT} aborted due to previous installation detection."
    Abort

  done:
FunctionEnd

;--------------------------------
;Installer Sections

Section "Policy Editor" SecCopyUI
  ; First, let's check that the product was not already installed.
  Call CheckPreviousInstalls

  ;ADD YOUR OWN STUFF HERE!

  SetOutPath "$INSTDIR"
  File "${MUI_PRODUCT}.exe"
  File "${MUI_PRODUCT}.ini"
  File "${MUI_PRODUCT}.bat"
  File "${PACKAGE_DIR}\Manager.jar"
  File /r "${J2RE_PATH}"
  Rename "$INSTDIR\${J2RE}" "$INSTDIR\jre"
  File "${PACKAGE_DIR}\logging.properties"
  File /r "${PACKAGE_DIR}\help"

  RMDir /r "$INSTDIR/lib"
  File /r "${PACKAGE_DIR}\lib"
  
  ;Store install folder
  WriteRegStr HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}" "" $INSTDIR
  WriteRegStr HKCU "Software\${COMPANY}\${MUI_PRODUCT}" "" $INSTDIR
  WriteRegStr HKCU "Software\${COMPANY}\${MUI_PRODUCT}" "version" ${MUI_VERSION}

  !insertmacro MUI_STARTMENU_WRITE_BEGIN
    
    ;Create shortcuts for all users
    SetShellVarContext all
    CreateDirectory "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}"
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\${MUI_PRODUCT}.lnk" "$INSTDIR\${MUI_PRODUCT}.exe" parameters "$INSTDIR\${MUI_PRODUCT}.exe" 0
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\${MUI_PRODUCT} in Troubleshooting Mode.lnk" "$INSTDIR\${MUI_PRODUCT}.bat" parameters "$INSTDIR\${MUI_PRODUCT}.exe" 1
    SetShellVarContext current

  !insertmacro MUI_STARTMENU_WRITE_END
  
  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"

  ;Register with Add/Remove programs
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "DisplayName" "${MUI_PRODUCT} ${MUI_VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "UninstallString" "$INSTDIR\Uninstall.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "Publisher" "${COMPANY}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "URLInfoAbout" "http://www.layer7tech.com"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "DisplayVersion" "${MUI_VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "NoModify" "1"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "NoRepair" "1"

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

  ;ADD YOUR OWN STUFF HERE!

  Delete "$INSTDIR\${MUI_PRODUCT}.bat"
  Delete "$INSTDIR\${MUI_PRODUCT}.exe"
  Delete "$INSTDIR\${MUI_PRODUCT}.ini"
  Delete "$INSTDIR\Manager.jar"
  RMDir /r "$INSTDIR\lib"
  RMDir /r "$INSTDIR\jre"
  RMDir /r "$INSTDIR\help"
  Delete "$INSTDIR\Uninstall.exe"
  Delete "$INSTDIR\logging.properties"

  ;Remove shortcut
  ReadRegStr ${TEMP} "${MUI_STARTMENUPAGE_REGISTRY_ROOT}" "${MUI_STARTMENUPAGE_REGISTRY_KEY}" "${MUI_STARTMENUPAGE_REGISTRY_VALUENAME}"

  StrCmp ${TEMP} "" noshortcuts

    SetShellVarContext all
    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT}.lnk"
    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT} in Troubleshooting Mode.lnk"
    Delete "$SMPROGRAMS\${TEMP}\Uninstall ${MUI_PRODUCT}.lnk"
    RMDir "$SMPROGRAMS\${TEMP}" ;Only if empty, so it won't delete other shortcuts
    SetShellVarContext current
    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT}.lnk"
    Delete "$SMPROGRAMS\${TEMP}\${MUI_PRODUCT} in Troubleshooting Mode.lnk"
    Delete "$SMPROGRAMS\${TEMP}\Uninstall ${MUI_PRODUCT}.lnk"
    RMDir "$SMPROGRAMS\${TEMP}" ;Only if empty, so it won't delete other shortcuts
    
  noshortcuts:

  RMDir "$INSTDIR"
  RMDir "$PROGRAMFILES\${COMPANY}"

  DeleteRegKey HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}"
  DeleteRegKey HKCU "Software\${COMPANY}\${MUI_PRODUCT}"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}"
  
  ;Display the Finish header
  !insertmacro MUI_UNFINISHHEADER

SectionEnd
