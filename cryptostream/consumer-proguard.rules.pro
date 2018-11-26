# Redundant with Android's default proguard rules, but it's
# important here in case consumers neglect to include them
# for some reason.
-keepclasseswithmembernames class * {
    native <methods>;
}