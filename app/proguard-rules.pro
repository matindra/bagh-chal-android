# Release keep rules (R8). Libraries ship their own; add only what they miss.

# WorkManager (pulled in by Google Mobile Ads) creates its Room database by reflection
# (WorkDatabase_Impl); without this the release build crashes at startup.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
