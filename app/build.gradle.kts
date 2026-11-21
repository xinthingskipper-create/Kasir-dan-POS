import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.poskasir.wrytxq"
    minSdk = 23
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      all {
        it.jvmArgs("-Djava.awt.headless=true")
      }
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Disable unnecessary KSP processing on unit test source sets to prevent AWT-EventQueue NPE in headless environments
tasks.matching { it.name.startsWith("ksp") && it.name.contains("Test") }.configureEach {
  enabled = false
}


// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

abstract class EnsureGoogleServicesJsonTask : org.gradle.api.DefaultTask() {
  @get:org.gradle.api.tasks.Input
  abstract val packageName: org.gradle.api.provider.Property<String>

  @get:org.gradle.api.tasks.Internal
  abstract val rootFile: org.gradle.api.file.RegularFileProperty

  @get:org.gradle.api.tasks.Internal
  abstract val debugFile: org.gradle.api.file.RegularFileProperty

  @get:org.gradle.api.tasks.Internal
  abstract val releaseFile: org.gradle.api.file.RegularFileProperty

  @org.gradle.api.tasks.TaskAction
  fun ensure() {
    val target = rootFile.get().asFile
    val dbg = debugFile.get().asFile
    val rel = releaseFile.get().asFile
    if ((target.exists() && target.length() > 0L) ||
      (dbg.exists() && dbg.length() > 0L) ||
      (rel.exists() && rel.length() > 0L)
    ) {
      return
    }
    val appId = packageName.get()
    target.writeText(
      """
      {
        "project_info": {
          "project_number": "000000000000",
          "project_id": "offline-pos-placeholder",
          "storage_bucket": "offline-pos-placeholder.firebasestorage.app"
        },
        "client": [
          {
            "client_info": {
              "mobilesdk_app_id": "1:000000000000:android:0000000000000000",
              "android_client_info": {
                "package_name": "$appId"
              }
            },
            "oauth_client": [
              {
                "client_id": "000000000000-placeholder.apps.googleusercontent.com",
                "client_type": 3
              }
            ],
            "api_key": [
              {
                "current_key": "AIzaSyPlaceholderKeyForOfflineBuildOnly000"
              }
            ],
            "services": {
              "appinvite_service": {
                "other_platform_oauth_client": []
              }
            }
          }
        ],
        "configuration_version": "1"
      }
      """.trimIndent() + "\n"
    )
    logger.warn("app/google-services.json tidak ditemukan; konfigurasi fallback dibuat agar build tetap berjalan.")
  }
}

val ensureGoogleServicesJson = tasks.register<EnsureGoogleServicesJsonTask>("ensureGoogleServicesJson") {
  packageName.set(android.defaultConfig.applicationId ?: "com.example.app")
  rootFile.set(layout.projectDirectory.file("google-services.json"))
  debugFile.set(layout.projectDirectory.file("src/debug/google-services.json"))
  releaseFile.set(layout.projectDirectory.file("src/release/google-services.json"))
}

tasks.matching { it.name.startsWith("process") && it.name.endsWith("GoogleServices") }.configureEach {
  dependsOn(ensureGoogleServicesJson)
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.play.services.auth)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
