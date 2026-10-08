# Кастомные ProGuard/R8 правила.
# Compose, Room и прочие AndroidX/Google-библиотеки несут consumer-правила
# в самих AAR — дополнительные keep-правила для них не нужны.

# Читаемые стек-трейсы в Crashlytics — пара из доков Firebase:
# атрибуты имён/строк сохраняются, оригинальные имена файлов в трейсах
# скрыты константой SourceFile; деобфускация — через загруженный mapping
# (uploadCrashlyticsMappingFile* на release-сборке).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Crashlytics/Firebase: аннотации, исключения и generic-сигнатуры
# (нужны SDK для рефлексии).
-keepattributes *Annotation*
-keepattributes exceptions
-keepattributes signature
-dontwarn com.google.firebase.crashlytics.**
