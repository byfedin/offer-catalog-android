# Offer Catalog

An independent Android demo by [Evgenii Fedin](https://github.com/byfedin). Browse a fictional collection of learning, travel, digital and wellbeing offers. Search, filter, explore details and return to where you left off.

The project focuses on a small set of engineering problems: cancelling obsolete work, preserving useful content when pagination fails, restoring search parameters and testing asynchronous behaviour. It is not a commercial product or a client for a live service.

<p>
  <img src="docs/screenshots/catalog-light.png" width="240" alt="Catalog in the light theme" />
  <img src="docs/screenshots/detail-light.png" width="240" alt="Offer details in the light theme" />
  <img src="docs/screenshots/catalog-dark.png" width="240" alt="Catalog in the dark theme" />
</p>

## Run

- JDK 17.
- Android SDK Platform 37, Build Tools 36.0.0 and accepted SDK licences.
- An Android 8.0 / API 26 or newer emulator or device.
- Android Studio with AGP 9.2 support, or the command line.

```sh
git clone https://github.com/byfedin/offer-catalog-android.git
cd offer-catalog-android
# Set ANDROID_HOME to your SDK, or let Android Studio create local.properties.
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Open the repository root in Android Studio and run the `app` configuration. The first build downloads dependencies; the installed application requires no network access, accounts, keys or backend. There is no INTERNET permission.

Versions are pinned in `gradle/libs.versions.toml`: AGP 9.2.1, Kotlin / Compose compiler 2.4.21 and Compose BOM 2026.09.00, with Gradle 9.4.1. AGP's built-in Kotlin support is used; its compiler dependency is explicitly upgraded to match the Compose compiler. The combination follows the [AGP compatibility table](https://developer.android.com/build/releases/agp-9-2-0-release-notes) and [Kotlin compatibility table](https://kotlinlang.org/docs/gradle-configure-project.html). The current Compose libraries require compile SDK 37.

## What to try

- Type quickly in search: only the final distinct query loads after 300 ms.
- Choose a category while loading: the category applies immediately with the current query.
- Scroll down: pages load near the end; an explicit **Load more** action is also available.
- Open an offer and go back: the existing results and scroll position remain.
- Rotate or recreate the activity: the query and category are restored.
- Search for something absent: reset the filters from the empty state.
- Switch the device between light and dark themes, or increase its font size.

Loading, initial failure, append failure, empty collection, no matches, unknown offer and end-of-list states are distinct. Error paths are exercised with controlled repositories in tests; the normal demo intentionally has no random failures or hidden failure switches.

## Data and architecture

One Gradle module, organised by responsibility:

```text
app/src/main/java/dev/byfedin/offercatalog/
  catalog/     catalog state, search and pagination, catalog UI
  detail/      detail state and UI
  data/        repository boundary, fixtures and asynchronous source
  model/       offers, categories, filters and page results
  ui/          theme, reusable status UI and local illustrations
```

```text
UI events → ViewModel → OfferRepository → local fixtures
                ↓
           StateFlow → lifecycle-aware Compose UI
                ↕
         SavedStateHandle (query + category)
```

`FixtureOfferRepository` contains 32 fictional offers with stable slugs and a fixed order. It filters before slicing pages of eight, returns an explicit continuation offset and simulates a fixed 450 ms delay. Work runs on an injected dispatcher and cancellation propagates. Tests can replace the repository, fixtures, page size, dispatcher and latency.

### Decisions worth discussing

1. **A small, catalogue-specific paginator.** A single request job and a generation token are enough for this finite local collection. There is no database, remote mediator or reusable paging framework. Paging 3 would be preferable if the application grew to require persistent caching, refresh keys or large datasets.
2. **Invalidate before debounce.** Changing normalized parameters increments the generation and cancels the job immediately. A cancellable 300 ms delay debounces typing; changing the category bypasses it. Results and expected failures are applied only when their generation still matches, including responses from a source that ignores cancellation. Different selections cannot mix pages.
3. **Explicit content states.** Initial loading and failure are separate from results. Results own their continuation offset and append status, so an append error keeps the visible collection. The busy flag is set before launching a request; repeated scroll or retry events cannot enqueue the same page. Appended offers are deduplicated by stable ID.
4. **Small dependency graph.** The activity supplies one repository through ViewModel factories. There is no DI framework, pass-through use-case layer or base class hierarchy. The repository interface is the asynchronous boundary used by the tests.
5. **Navigation owns screen lifetime.** Each destination owns its ViewModel. Lifecycle-aware collection stops unnecessary UI observation. The catalog remains on the back stack. Its saveable lazy-list state is owned above `NavHost`, so disposing the catalog composition while a page arrives cannot recreate its scroll position. Search parameters live in `SavedStateHandle`. Cancellation is never converted to a visible error; only the expected catalog-unavailable exception becomes a retry state.

## Checks

```sh
./gradlew spotlessCheck :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
# With an emulator or device connected:
./gradlew :app:connectedDebugAndroidTest
# Apply the agreed Kotlin format:
./gradlew spotlessApply
```

Unit tests use coroutine virtual time, controlled completions and no real sleeps. They cover rapid input, equivalent queries, immediate cancellation, stale success and failure, category changes, pagination reset, duplicate scroll events and IDs, failed append plus retry, retained results, terminal pages, empty data, restored parameters and unknown IDs. A deliberately cancellation-resistant fake checks the generation guard.

The search reset test checks the exact editable value. Scroll restoration checks both the visible anchor position and the scroll offset after a non-zero scroll.

Instrumented Compose tests exercise search → detail → back, scroll retention, recreation, empty-search recovery, initial/append failures and the unknown-offer action. GitHub Actions runs formatting, unit tests, lint, a debug build and instrumented tests. Test reports are uploaded on completion, including failures.

## Device lifecycle and startup measurements

With the debug APK installed and an explicit emulator serial:

```sh
python3 scripts/device_checks.py --serial emulator-5554 --mode process-death --output app/build/reports/device-checks/process-death.json
python3 scripts/device_checks.py --serial emulator-5554 --mode startup --iterations 5 --output app/build/reports/device-checks/startup.json
```

Python 3 and `adb` on PATH are required. These commands control the selected test device and restart this app; use a dedicated emulator. The process-death check backgrounds a filtered catalog, confirms it has left the foreground, terminates its debug process with `run-as … kill -9`, and returns through the launcher intent to the retained task. It asserts a new process, the restored query and selected category, and the expected offer. It runs outside the app process, separately from Activity recreation tests, and is included in CI on API 26 and 36. Device polling uses bounded wall-clock waits; coroutine unit tests continue to use virtual time.

The startup command records individual Activity display times and their median. See [the startup study](docs/startup-study.md) for actual results, measurement scope and limitations. It is a diagnostic baseline, with no performance pass/fail threshold.

## Boundaries

- The source is local and read-only. This is not an offline-first implementation and has no remote synchronisation, persistence or cache invalidation.
- Loaded pages stay in memory while the catalog ViewModel lives. Process death restores search parameters and reloads from page one; it does not persist the whole collection or promise deep-scroll restoration after process death.
- Search matches titles, ignoring case and surrounding whitespace. There is no ranking, full-text index or locale-specific collation.
- Paging is forward-only, and loaded pages are retained in memory. That is suitable for 32 records, not an unbounded feed.
- The interface and fixtures are English-only. The layout accommodates larger text and centres content on wider screens; it does not implement a tablet master-detail layout.
- The fictional offers are informational. There are no purchase, registration or reward actions.

## Artwork and licence

The geometric illustrations are drawn locally in `ui/OfferArtwork.kt`; they use no downloaded photographs, fonts or external image URLs. The illustrations and fictional fixture text are part of this project and are covered by the [MIT licence](LICENSE). AndroidX, Kotlin and the Gradle Wrapper retain their respective licences.
