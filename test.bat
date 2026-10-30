@echo off
if not exist bin (
    echo Building project first...
    call build.bat
)
echo ===============================================================================
echo EXECUTING MODULE 2 TEST SUITE (VEHICLE AND FASTAG ACCOUNTS)
echo ===============================================================================
java -cp bin com.toll.test.Module2TestSuite
echo.
echo ===============================================================================
echo EXECUTING MODULE 3 TEST SUITE (TOLL PROCESSING ENGINE)
echo ===============================================================================
java -cp bin com.toll.test.Module3TestSuite
echo.
echo ===============================================================================
echo EXECUTING COMPLETE INTEGRATED SYSTEM SUITE (ALL 5 MODULES)
echo ===============================================================================
java -cp bin com.toll.test.AllModulesTestSuite
