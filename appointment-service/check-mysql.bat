@echo off
echo ========================================
echo MySQL Service Checker
echo ========================================
echo.

echo Checking if MySQL service is running...
sc query MySQL80 | findstr "RUNNING" >nul
if %errorlevel% equ 0 (
    echo [OK] MySQL80 service is RUNNING
) else (
    echo [!] MySQL80 service is NOT running
    echo.
    echo Starting MySQL80 service...
    net start MySQL80
    if %errorlevel% equ 0 (
        echo [OK] MySQL80 started successfully
    ) else (
        echo [ERROR] Failed to start MySQL80
        echo Try running this script as Administrator
    )
)

echo.
echo ========================================
echo Testing MySQL Connection
echo ========================================
echo.

echo Testing password: Indiwari@2002
mysql -u root -pIndiwari@2002 -e "SELECT 'SUCCESS' as Status;" 2>nul
if %errorlevel% equ 0 (
    echo [OK] Password Indiwari@2002 works!
    echo.
    echo Correct configuration:
    echo   username: root
    echo   password: Indiwari@2002
    echo   port: 3306
) else (
    echo [!] Password Indiwari@2002 failed
    echo.
    echo Testing password: Himasha@2002
    mysql -u root -pHimasha@2002 -e "SELECT 'SUCCESS' as Status;" 2>nul
    if %errorlevel% equ 0 (
        echo [OK] Password Himasha@2002 works!
        echo.
        echo Correct configuration:
        echo   username: root
        echo   password: Himasha@2002
        echo   port: 3306
    ) else (
        echo [ERROR] Neither password works!
        echo Please reset your MySQL root password
    )
)

echo.
echo ========================================
echo Checking Port 3306
echo ========================================
netstat -ano | findstr :3306
if %errorlevel% equ 0 (
    echo [OK] Port 3306 is in use (MySQL is listening)
) else (
    echo [!] Port 3306 is NOT in use
    echo MySQL may not be running
)

echo.
pause
