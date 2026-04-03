@echo off
echo Starting Appointment Service...
echo.

cd "D:\A sllit\3 Year\3Year 1Sem\DS\ayubo web\AyuboBackend\appointment-service"

echo Checking if MySQL is running...
netstat -ano | findstr :3306 >nul
if %errorlevel% neq 0 (
    echo [WARNING] MySQL may not be running on port 3306
    echo Please start MySQL first!
    echo.
)

echo Starting service on port 8082...
java -jar target\appointment-service-0.0.1-SNAPSHOT.jar

pause
