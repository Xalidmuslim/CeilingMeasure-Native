# Android components referenced by the manifest are kept by the Android Gradle Plugin.
# The app's native drawing, geometry, pricing, JSON persistence and dialogs use
# direct Java calls; deliberately avoid broad -keep class ** rules that defeat R8.
# Preserve useful stack traces for regression diagnostics.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
