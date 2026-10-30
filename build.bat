@echo off
echo =======================================================
echo Compiling Toll Management System (FASTag)...
echo =======================================================

if not exist bin mkdir bin
dir /s /b src\*.java > sources.txt
javac -d bin @sources.txt

if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Compilation complete. Classes compiled to bin/
) else (
    echo [ERROR] Compilation failed.
)
if exist sources.txt del sources.txt
