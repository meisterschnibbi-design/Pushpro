
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
  namespace = "pro.pushpro.app"
  compileSdk = 35
  defaultConfig {
    applicationId = "pro.pushpro.app"
    minSdk = 24
    targetSdk = 35
    versionCode = 98
    versionName = "v98"
    vectorDrawables { useSupportLibrary = true }
  }
  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    debug { isMinifyEnabled = false }
  }
  compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
  kotlinOptions { jvmTarget = "17" }
  buildFeatures { viewBinding = true }
  packaging {
    resources {
      excludes += setOf("/META-INF/{AL2.0,LGPL2.1}","META-INF/DEPENDENCIES","META-INF/LICENSE*","META-INF/NOTICE*")
    }
  }
}
dependencies {
  implementation("com.sun.mail:android-mail:1.6.7")
  implementation("com.sun.mail:android-activation:1.6.7")
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
  implementation("androidx.activity:activity-ktx:1.9.2")
  implementation("androidx.recyclerview:recyclerview:1.3.2")
}
