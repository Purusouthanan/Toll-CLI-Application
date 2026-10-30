@echo off
@chcp 65001 > nul
if not exist bin (
    echo Building project first...
    call build.bat
)
java -Dfile.encoding=UTF-8 -cp bin com.toll.Main
