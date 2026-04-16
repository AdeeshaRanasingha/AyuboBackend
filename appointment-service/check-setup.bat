@echo off
echo ========================================
echo Checking MySQL Connection...
echo ========================================

mysql -u root -pHimasha@2002 -e "SHOW DATABASES;" 2>nul

if %errorlevel% equ 0 (
    echo [OK] MySQL is running and credentials are correct
    echo.
    echo Creating ayubo_appointment_db database if not exists...
    mysql -u root -pHimasha@2002 -e "CREATE DATABASE IF NOT EXISTS ayubo_appointment_db;"
    echo [OK] Database ready
) else (
    echo [ERROR] Cannot connect to MySQL!
    echo Please check:
    echo 1. MySQL is running
    echo 2. Username: root
    echo 3. Password: Himasha@2002
    echo 4. Port: 3306
)

echo.
echo ========================================
echo Checking if port 8082 is free...
echo ========================================
netstat -ano | findstr :8082
if %errorlevel% equ 0 (
    echo [WARNING] Port 8082 is already in use!
    echo Please stop the process using this port.
) else (
    echo [OK] Port 8082 is available
)

echo.
pause
