@ECHO OFF
SET DIR=%~dp0
IF EXIST "%DIR%gradle\wrapper\gradle-wrapper.jar" (
  java -jar "%DIR%gradle\wrapper\gradle-wrapper.jar" %*
) ELSE (
  ECHO gradle-wrapper.jar missing. Android Studio will regenerate it on Sync.
  gradle %*
)
