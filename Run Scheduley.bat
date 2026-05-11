@echo off
setlocal

cd /d "%~dp0"

echo Launching Scheduley...
echo.

mvn javafx:run

if errorlevel 1 (
    echo.
    echo Scheduley failed to launch. Make sure Java 21 and Maven are installed and available on PATH.
    echo You can also try running this command from the project folder:
    echo mvn javafx:run
    echo.
    pause
)
