plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
    // Плагин Firebase. Активируется в app/build.gradle.kts только когда
    // добавлен google-services.json (см. README, раздел про Firebase).
    id("com.google.gms.google-services") version "4.4.2" apply false
}
