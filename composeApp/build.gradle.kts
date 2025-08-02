import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kotlinx.coroutines.desktop)

            implementation(libs.jgit)

            implementation(libs.compose.material.icons.extended) // Pour les icônes

            implementation(libs.compose.desktop)
            implementation(libs.compose.material3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
//            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

compose.desktop {
    application {
        nativeDistributions {
            macOS {
                iconFile.set(project.file("icon.icns"))
            }
            windows {
                iconFile.set(project.file("icon.ico"))
            }
            linux {
                iconFile.set(project.file("icon.png"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.poly.devtop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.poly.devtop"
            packageVersion = "1.0.0"
            appResourcesRootDir.set(project.file("src/desktopMain/resources")) // Dossier des ressources
            windows {
                dirChooser = true // Active le sélecteur de dossiers
                iconFile.set(project.file("icon.ico"))
                menuGroup = "Poly Devtop" // Groupe dans le menu Démarrer
            }
            macOS {
                iconFile.set(project.file("icon.icns"))
            }
            linux {
                iconFile.set(project.file("icon.png"))
            }
            outputBaseDir.set(file("build/dist")) // Dossier de sortie
            modules("kotlinx-serialization-json") // Inclut kotlinx.serialization pour les JSON
            // Copie des dossiers configs/ et tags/
//            includeResources.set(listOf("configs/**", "tags/**"))
        }
    }
}
