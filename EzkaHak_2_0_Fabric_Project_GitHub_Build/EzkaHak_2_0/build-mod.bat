@echo off
setlocal
where java >nul 2>nul || (echo JDK 21 is required.& exit /b 1)
where gradle >nul 2>nul || (echo Gradle is not installed. Install Gradle, then run this script again.& exit /b 1)
java -version
gradle clean build
if errorlevel 1 exit /b %errorlevel%
echo.
echo Build output: %CD%\build\libs\ezkahak-2.0.0.jar
