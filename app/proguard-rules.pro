# Room 2.2.5 creates WorkManager's generated database implementation by reflection.
-keepclassmembers class androidx.work.impl.WorkDatabase_Impl {
    public <init>();
}
