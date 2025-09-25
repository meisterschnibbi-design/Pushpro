This delta includes all required sources and Gradle configuration for package `pro.pushpro.app`.
For CI to run `./gradlew`, you must regenerate the Gradle Wrapper JAR (binary) locally:

  ./gradlew wrapper --gradle-version 8.7 --distribution-type bin

This will create gradle/wrapper/gradle-wrapper.jar used by the scripts already included here.

Quick check:
  ./gradlew --version
  ./gradlew assembleDebug

Notes:
- Layout IDs: Webhook(inputUrl, btnSendTest, inputHttpMethod, inputContentType, inputHeaders, inputJsonTemplate),
  Email(switchEnableEmail, inputHost, inputPort, inputUser, inputPass, spinnerTls, inputRecipient, btnSendTestEmail),
  Telegram(inputToken, inputChatIds, parseModeDropdown, btnSendTestTelegram).
- endIconMode uses 'dropdown_menu'.
- No inputBody references; Default JSON lives in arrays.xml only.
- META-INF merge handled via packaging.pickFirsts in app/build.gradle.
