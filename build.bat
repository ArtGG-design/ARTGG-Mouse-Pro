@echo off
set JAVA_HOME=C:\DevTools\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d C:\Users\Administrador\Desktop\claude\artgg-mouse-pro
call gradlew.bat assembleDebug --no-daemon -Djava.net.preferIPv4Stack=true