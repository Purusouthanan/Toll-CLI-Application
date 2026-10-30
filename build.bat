@echo off
echo =======================================================
echo Compiling Toll Management System (FASTag)...
echo =======================================================

if not exist bin mkdir bin
powershell -NoProfile -Command "$files = Get-ChildItem -Recurse -Filter *.java src | Select-Object -ExpandProperty FullName; javac -d bin $files"

if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Compilation complete. Classes compiled to bin/
) else (
    echo [ERROR] Compilation failed.
)
