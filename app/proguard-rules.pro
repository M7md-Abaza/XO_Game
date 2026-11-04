# R8 rules for the release build. Libraries (Compose, Hilt, Navigation,
# kotlinx.serialization, DataStore, Play Billing, Google Mobile Ads, UMP)
# ship their own consumer rules, so only app-specific rules belong here.

# Readable crash stack traces in Play Console. The AAB carries mapping.txt,
# which Play uses to restore the class and method names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Play In-App Review (review-ktx 2.0.2) references this Play services annotation,
# which isn't shipped in any library. It is only an annotation, so ignore it.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
