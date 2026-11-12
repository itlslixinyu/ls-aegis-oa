import java.net.URI

plugins { id("com.android.application") }

val configuredEntry = providers.gradleProperty("itlsEngineerEntryUrl").orElse("http://127.0.0.1:25685/engineer/login").get()
val entry = URI(configuredEntry)
require(entry.userInfo == null && entry.fragment == null && entry.query == null && entry.host != null &&
    (entry.scheme == "https" || (entry.scheme == "http" && entry.host == "127.0.0.1"))) { "App入口必须为HTTPS；仅回环开发入口允许HTTP" }
val lanResources = providers.gradleProperty("itlsLanResources").orNull
require(!gradle.startParameter.taskNames.any { it.contains("Release", true) } ||
    (entry.scheme == "https" && lanResources == null)) { "正式包需HTTPS入口和系统可信证书，不能使用本机调试CA" }

android {
    namespace = "com.lsaegis.eoms.engineer.demo"
    compileSdk = 35
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets.getByName("main").java.srcDir("../shared/src/main/java")
    buildFeatures { buildConfig = true }
    if (lanResources != null) sourceSets.getByName("debug").res.srcDir(lanResources)

    defaultConfig {
        applicationId = "com.lsaegis.eoms.engineer.demo"
        minSdk = 24
        targetSdk = 34
        versionCode = 8
        versionName = "1.2.0"
        buildConfigField("String", "ENTRY_URL", "\"${entry.toASCIIString()}\"")
        manifestPlaceholders["mobileNetworkSecurityConfig"] = if (lanResources == null) "@xml/network_security_config" else "@xml/mobile_lan_network_security_config"
    }
}

// Narrow content-URI sharing for delegated system-camera capture.
dependencies { implementation("androidx.core:core:1.15.0") }
