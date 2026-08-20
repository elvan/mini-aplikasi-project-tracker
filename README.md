# Mini Aplikasi Project Tracker

Aplikasi Android untuk mengelola *project* beserta *task*/*subtask* di dalamnya.
Status dan progres *project* dihitung otomatis dari status *task*, dengan dukungan
*dependency* antar-*task* maupun antar-*project*, filtering *task* berbasis
hierarki, serta validasi jadwal *project* agar tidak saling beririsan.

Dibangun sebagai aplikasi native Android dengan Kotlin (bukan Compose), mengikuti
pola arsitektur **MVVM** dengan penyimpanan data lokal menggunakan **Room**
(SQLite).

## Fitur Utama

- **CRUD Project** — nama, status, *completion progress*, `start date`, dan
  `end date`. Status dan progres bersifat *read-only* karena diturunkan dari data
  *task*, bukan diisi manual oleh pengguna.
- **CRUD Task** — nama, status, relasi ke *project*, `bobot` (integer), dan
  `parent task` opsional untuk mendukung *subtask*.
- **Perhitungan progres otomatis** — progres *project* = (total bobot *task*
  berstatus `Done` ÷ total bobot seluruh *task*) × 100%.
- **Status project otomatis** — `Draft` jika seluruh *task* `Draft`, `Done` jika
  seluruh *task* `Done`, selain itu `In Progress`.
- **Dependency task** — *task* tidak dapat berstatus `Done` selama ada
  *dependency*-nya yang belum `Done`; *circular dependency* (langsung maupun
  transitif) ditolak sistem.
- **Dependency project** — *project* tidak dapat berstatus `In Progress`/`Done`
  selama *project* *dependency*-nya belum `Done`; perubahan status *dependency*
  memicu validasi ulang secara *cascade*; *circular dependency* antar-*project*
  ditolak.
- **Hierarki & filtering task** — filter berdasarkan status maupun pencarian
  nama tetap menampilkan *parent task* dari *subtask* yang cocok, sehingga
  struktur *tree* tetap konsisten.
- **Validasi jadwal project** — rentang `start date`–`end date` antar-*project*
  tidak boleh beririsan; jika konflik terjadi saat simpan/ubah, sistem menolak
  proses dan menampilkan *project* mana saja yang menyebabkan konflik.

## Arsitektur

Aplikasi menggunakan pendekatan **Single Activity** (`MainActivity`) dengan satu
`NavHostFragment` yang berpindah antar `Fragment` melalui Navigation Component
(`res/navigation/nav_graph.xml`).

Struktur kode mengikuti alur `entity → dao → repository → domain (use case) →
viewmodel → fragment`, di bawah package `com.example.projecttracker`:

```
data/local/entity/       Entity Room: Project, Task, TaskDependency,
                          ProjectDependency, plus enum ProjectStatus/TaskStatus
data/local/dao/           DAO Room per entity/relasi
data/local/AppDatabase.kt Singleton Room database
data/repository/          Wrapper tipis di atas DAO (suspend function di
                          Dispatchers.IO, hasil baca berupa Flow)
domain/                   Use case untuk business rule: kalkulasi progres,
                          deteksi circular dependency, validasi dependency
                          task, filtering hierarki task, deteksi konflik
                          jadwal
viewmodel/                Satu ViewModel per fitur, mengekspos StateFlow
                          (tanpa framework DI — dirakit manual lewat
                          ViewModelProvider.Factory)
ui/                       Fragment dan RecyclerView adapter; form tambah/edit
                          memakai DialogFragment/BottomSheetDialogFragment
```

- **UI**: Kotlin + XML *views* (bukan Jetpack Compose), dengan `ViewBinding`.
- **Pembaruan data real-time**: `StateFlow` dari ViewModel diamati Fragment
  sehingga tampilan otomatis ter-*update* setelah operasi CRUD.
- **Penyimpanan data**: seluruh data *project*, *task*, *subtask*, dan
  *dependency* disimpan lokal via Room (SQLite), belum ada migrasi Room nyata
  (`fallbackToDestructiveMigration`).

## Tech Stack

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin |
| UI Toolkit | Android Views (XML) + ViewBinding |
| Arsitektur | MVVM + Single Activity |
| Navigasi | Navigation Component (Fragment) |
| Database lokal | Room (SQLite) |
| Asynchronous | Kotlin Coroutines + Flow |
| State UI | `StateFlow` (`viewModelScope`, `WhileSubscribed(5_000)`) |
| Build system | Gradle (Kotlin DSL) + KSP |

Target SDK: min **29**, target/compile **37**.

## Cara Menjalankan

Requirement: Android Studio (atau JDK 11+ dan Android SDK) dengan `ANDROID_HOME`
sudah ter-*setup*.

```bash
# Build APK debug
./gradlew assembleDebug

# Build penuh (compile + lint + unit test)
./gradlew build

# Menjalankan unit test (JVM, app/src/test)
./gradlew test

# Menjalankan satu unit test spesifik
./gradlew testDebugUnitTest --tests "com.example.projecttracker.NamaTest"

# Menjalankan instrumented test (butuh emulator/device, app/src/androidTest)
./gradlew connectedAndroidTest

# Menjalankan Android lint
./gradlew lint
```

Atau buka folder ini langsung di Android Studio dan jalankan konfigurasi run
default (`app`) ke emulator/device pilihan.

## Struktur Dokumentasi Terkait

Repo ini berisi kode aplikasi saja. Spesifikasi produk dan pemecahan
backlog-nya berada di repo terpisah `mini-aplikasi-project-documentation/`:

- `requirements/spesifikasi-aplikasi.md` — spesifikasi lengkap (Bahasa
  Indonesia).
- `backlogs/` — pemecahan spesifikasi menjadi backlog bernomor, dikerjakan
  bertahap per fase: Fondasi → Skema Room → Repository/DAO → CRUD UI Project →
  CRUD UI Task → Business Rules → Fitur Lanjutan.
