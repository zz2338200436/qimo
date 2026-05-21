@echo off
setlocal
set "JAR_DIR=D:\111\Distributed framework technology\JavaCode\majorassignment\assignment-service\target"
set "JAR_NAME=assignment-service-0.1.0-SNAPSHOT.jar"
set "OUT=D:\111\Distributed framework technology\JavaCode\majorassignment\assignment-service.log"
set "ERR=D:\111\Distributed framework technology\JavaCode\majorassignment\assignment-service.err.log"
pushd "%JAR_DIR%"
call java -jar "%JAR_NAME%" 1>"%OUT%" 2>"%ERR%"
set "EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %EXIT_CODE%
