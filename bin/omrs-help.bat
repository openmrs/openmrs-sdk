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
echo Usage: omrs-help [options]
echo.
echo Displays version and command information for the OpenMRS Module SDK.
goto finish

:continue

set SDK_VERSION="1.0.1"

echo.
echo OMRS Version:    %SDK_VERSION%
echo Commands available: 
echo. 
echo omrs-version - Displays the version of SDK and runtime info regarding Java,
echo                Maven and important paths.
echo.
echo omrs-run - runs an embedded instance of OpenMRS
echo.
echo omrs-create-project - creates a project which will allow the user to create
echo                       multiple moduels and deploy them to an embedded instance of OpenMRS.
echo.
echo omrs-create-module - creates a basic module
echo.


:finish




