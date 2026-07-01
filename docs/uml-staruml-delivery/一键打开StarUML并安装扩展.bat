@echo off
setlocal
cd /d "%~dp0"
echo Installing StarUML extension and opening StarUML...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-and-open-staruml.ps1"
if errorlevel 1 (
  echo.
  echo Failed. Please install StarUML first, then run this file again.
  pause
  exit /b 1
)
echo.
echo StarUML has been opened. In StarUML, click:
echo   Tools ^> 生成课程核心 9 张 UML 图
echo   or
echo   Tools ^> 生成推荐 24 张 UML 图
echo If the menu is not visible, close StarUML and run this file again.
echo.
pause
