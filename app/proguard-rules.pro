# DocMask Proguard / R8 Configuration

# Optimization flags
-repackageclasses
-allowaccessmodification

# Kotlin & Coroutines
-dontwarn kotlin.**
-dontwarn kotlinx.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Koin Dependency Injection
-dontwarn org.koin.**
-keep class org.koin.** { *; }

# Google ML Kit (Bundled models: Text Recognition & Face Detection)
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar

# Google Play Billing Library
-dontwarn com.android.billingclient.**
-keep class com.android.billingclient.** { *; }

# AndroidX Biometric & Security
-dontwarn androidx.biometric.**
-keep class androidx.biometric.** { *; }

# AndroidX DataStore
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }

# Domain, Core Models and UI State Classes
-keep class com.feryaeljustice.docmask.core.model.** { *; }
-keep class com.feryaeljustice.docmask.domain.model.** { *; }
-keep class com.feryaeljustice.docmask.presentation.** { *; }
