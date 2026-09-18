@echo off
REM AgriLoop Maven Wrapper Script
setlocal
if exist "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" (
    "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" %*
) else (
    mvn %*
)
endlocal
