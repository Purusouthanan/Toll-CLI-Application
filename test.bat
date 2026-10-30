@echo off
if not exist bin (
    echo Building project first...
    call build.bat
)
echo ===============================================================================
echo EXECUTING MODULE 2 TEST SUITE
echo ===============================================================================
java -cp bin com.toll.test.Module2TestSuite
echo.
echo ===============================================================================
echo EXECUTING MODULE 3 TEST SUITE
echo ===============================================================================
java -cp bin com.toll.test.Module3TestSuite
