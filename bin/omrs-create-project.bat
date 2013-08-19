

@echo off
rem Make sure we can set environment variables
if "%OS%" == "Windows_NT" setlocal enableextensions enabledelayedexpansion



rem ######
rem Lets find out if user is checking for help command
rem ######

if /I "%1"=="help" goto displayhelp
if /I "%1"=="-help" goto displayhelp
if /I "%1"=="--help" goto displayhelp
if /I "%1"=="/help" goto displayhelp
if /I "%1"=="-?" goto displayhelp
if /I "%1"=="/?" goto displayhelp
if /I "%1"=="/h" goto displayhelp
if /I "%1"=="-h" goto displayhelp

goto continue

:displayhelp
echo.
echo Usage: omrs-create-project [options]
echo.
echo Creates an OpenMRS Project
goto end

:continue

rem ######
rem Get SDK path
rem ######

set PROGDIR=%~dp0
set CURRENTDIR=%cd%
cd /d %PROGDIR%..
set SDK_HOME=%cd%
cd /d %CURRENTDIR%


rem ######
rem get maven location
rem ######

set MVN_HOME=%SDK_HOME%\apache-maven
set MAVEN_EXECUTABLE="%MVN_HOME%\bin\mvn.bat"
set SDK_VERSION="1.0.1"


rem ---------------------------------------------------------------
rem Transform Parameters into Maven Parameters
rem
rem NOTE: in DOS, all the 'else' statements must be on the same
rem line as the closing bracket for the 'if' statement.
rem
rem Courtesy of Atlassian
rem ---------------------------------------------------------------


set ARGV=.%*
call :parse_argv
if ERRORLEVEL 1 (
  echo Cannot parse arguments
  endlocal
  exit /B 1
)

set MAVEN_OPTS=-Xmx768M -XX:MaxPermSize=256M 
set MVN_PARAMS=-s "%MVN_HOME%\conf\settings.xml" -DinteractiveMode=false

set ARGI = 0

:loopstart
set /a ARGI = !ARGI! + 1
set /a ARGN = !ARGI! + 1

if !ARGI! gtr %ARGC% goto loopend

call :getarg !ARGI! ARG
call :getarg !ARGN! ARGNEXT

    if /I "%ARG%"=="--interactive" (
                    set MVN_PARAMS=%MVN_PARAMS% -DinteractiveMode=true
            set /a ARGI = !ARGI! + 1
                goto loopstart
    )  else (
            if /I "%ARG%"=="-i" (
                                    set MVN_PARAMS=-s "%MVN_HOME%\conf\settings.xml" -DinteractiveMode=true
                    set /a ARGI = !ARGI! + 1
                                goto loopstart
            )  

set MVN_PARAMS=%MVN_PARAMS% %ARG%
shift
goto loopstart

        )
        )
        )
        )
        )
        )
        )
        )
        )
        )
        )
        )
        )
        )
        )
        )
        )
        )
        )
        )
    :loopend


rem ######
rem Executing maven
rem ######

echo Executing: %MAVEN_EXECUTABLE% module-wizard:generate %MVN_PARAMS% -DarchetypeCatalog=local -DarchetypeArtifactId=devel-module-archetype-archetype -DarchetypeGroupId=org.openmrs.sdk -DarchetypeVersion=1.0 -DartifactId=openmrs-project -DgroupId=org.openmrs -Dversion=1.0.0-SNAPSHOT -Dpackage=org.openmrs
%MAVEN_EXECUTABLE% module-wizard:generate %MVN_PARAMS% -DarchetypeCatalog=local -DarchetypeArtifactId=devel-module-archetype-archetype -DarchetypeGroupId=org.openmrs.sdk -DarchetypeVersion=1.0 -DartifactId=openmrs-project -DgroupId=org.openmrs -Dversion=1.0.0-SNAPSHOT -Dpackage=org.openmrs

rem ---------------------------------------------------------------
rem (AMPS-197) The batch routines below for correct handling 
rem parameters containing of = and ; are from Skypher's excellent 
rem blog: 
rem http://skypher.com/index.php/2010/08/17/batch-command-line-arguments/
rem ---------------------------------------------------------------

:parse_argv
  SET PARSE_ARGV_ARG=[]
  SET PARSE_ARGV_END=FALSE
  SET PARSE_ARGV_INSIDE_QUOTES=FALSE
  SET /A ARGC = 0
  SET /A PARSE_ARGV_INDEX=1
  :PARSE_ARGV_LOOP
  CALL :PARSE_ARGV_CHAR !PARSE_ARGV_INDEX! "%%ARGV:~!PARSE_ARGV_INDEX!,1%%"
  IF ERRORLEVEL 1 (
    EXIT /B 1
  )
  IF !PARSE_ARGV_END! == TRUE (
    EXIT /B 0
  )
  SET /A PARSE_ARGV_INDEX=!PARSE_ARGV_INDEX! + 1
  GOTO :PARSE_ARGV_LOOP
 
  :PARSE_ARGV_CHAR
    IF ^%~2 == ^" (
      SET PARSE_ARGV_END=FALSE
      SET PARSE_ARGV_ARG=.%PARSE_ARGV_ARG:~1,-1%%~2.
      IF !PARSE_ARGV_INSIDE_QUOTES! == TRUE (
        SET PARSE_ARGV_INSIDE_QUOTES=FALSE
      ) ELSE (
        SET PARSE_ARGV_INSIDE_QUOTES=TRUE
      )
      EXIT /B 0
    )
    IF %2 == "" (
      IF !PARSE_ARGV_INSIDE_QUOTES! == TRUE (
        EXIT /B 1
      )
      SET PARSE_ARGV_END=TRUE
    ) ELSE IF NOT "%~2!PARSE_ARGV_INSIDE_QUOTES!" == " FALSE" (
      SET PARSE_ARGV_ARG=[%PARSE_ARGV_ARG:~1,-1%%~2]
      EXIT /B 0
    )
    IF NOT !PARSE_ARGV_INDEX! == 1 (
      SET /A ARGC = !ARGC! + 1
      SET ARG!ARGC!=%PARSE_ARGV_ARG:~1,-1%
      IF ^%PARSE_ARGV_ARG:~1,1% == ^" (
        SET ARG!ARGC!_=%PARSE_ARGV_ARG:~2,-2%
        SET ARG!ARGC!Q=%PARSE_ARGV_ARG:~1,-1%
      ) ELSE (
        SET ARG!ARGC!_=%PARSE_ARGV_ARG:~1,-1%
        SET ARG!ARGC!Q="%PARSE_ARGV_ARG:~1,-1%"
      )
      SET PARSE_ARGV_ARG=[]
      SET PARSE_ARGV_INSIDE_QUOTES=FALSE
    )
    EXIT /B 0

:getarg
  SET %2=!ARG%1!
  SET %2_=!ARG%1_!
  SET %2Q=!ARG%1Q!
  EXIT /B 0

:getargs
  SET %3=
  FOR /L %%I IN (%1,1,%2) DO (
    IF %%I == %1 (
      SET %3=!ARG%%I!
    ) ELSE (
      SET %3=!%3! !ARG%%I!
    )
  )
  EXIT /B 0


:end



