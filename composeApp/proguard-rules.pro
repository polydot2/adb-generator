# Ignorer les avertissements pour les dépendances courantes
-dontwarn androidx.compose.**
-dontwarn org.jetbrains.compose.**
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn io.ktor.**
-dontwarn org.eclipse.jgit.**

# Conserver les classes essentielles pour éviter leur suppression
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keep class io.ktor.** { *; }
-keep class org.eclipse.jgit.** { *; }

# Conserver les classes de ton application
-keep class com.poly.devtop.** { *; }

# Conserver les annotations et métadonnées Kotlin (important pour kotlinx.serialization)
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod

# Éviter l'optimisation agressive qui pourrait casser Compose
-dontoptimize

# Activer les logs détaillés pour déboguer (optionnel)
#-verbose

-keepattributes Signature

-dontwarn **