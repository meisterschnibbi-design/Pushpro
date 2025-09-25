@ECHO OFF
SET CLASSPATH=%~dp0gradle\wrapper\gradle-wrapper.jar
IF EXIST "%JAVA_HOME%\bin\java.exe" (SET JAVA_EXE=%JAVA_HOME%\bin\java.exe) ELSE (SET JAVA_EXE=java.exe)
"%JAVA_EXE%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
