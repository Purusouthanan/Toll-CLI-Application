@echo off
if not exist bin (
    echo Building project first...
    call build.bat
)
java -cp bin com.toll.test.Module2TestSuite
