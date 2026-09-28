@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------

@echo off
@setlocal

set MAVEN_CMD_LINE_ARGS=%*

if "%JAVA_HOME%" == "" (
  for /f "tokens=2*" %%i in ('reg query "HKLM\SOFTWARE\JavaSoft\JDK" /v CurrentVersion 2^>nul') do set JDK_VER=%%j
  if defined JDK_VER (
    for /f "tokens=2*" %%i in ('reg query "HKLM\SOFTWARE\JavaSoft\JDK\%JDK_VER%" /v JavaHome 2^>nul') do set JAVA_HOME=%%j
  )
)

if not exist "C:\Tools\apache-maven-3.9.9\bin\mvn.cmd" (
  echo Maven not found at C:\Tools\apache-maven-3.9.9\bin\mvn.cmd. Please run Maven or set PATH.
  exit /b 1
)

"C:\Tools\apache-maven-3.9.9\bin\mvn.cmd" %*
