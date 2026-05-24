# ── gomobile (age engine) ──────────────────────────────────
# libgojni resolves gomobile runtime classes and methods by exact Java names.
-keep class go.** { *; }
-keep class com.age.engine.ageengine.** { *; }

# ── Hilt ───────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

# ── Room ───────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# ── Kotlin Coroutines ──────────────────────────────────────
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ── Compose ────────────────────────────────────────────────
-dontwarn androidx.compose.**

# ── DataStore ──────────────────────────────────────────────
-keep class androidx.datastore.** { *; }

# ── Age app models (Room entities + serialization) ─────────
-keep class com.age.android.core.model.** { *; }
-keep class com.age.android.core.data.** { *; }
