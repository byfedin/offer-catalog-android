# Cold-process startup baseline

## Question

How long does the debug Activity take to display on a local API 36 emulator, and does that metric establish that catalog content is ready?

## Method

Measured on 2026-10-31 using the application implementation at `6c0b8d6`, with the host-side runner added in this change. Five runs, Android API 36, `sdk_gphone64_arm64`, local macOS ARM64 host, debug APK, default local fixtures. Each run force-stops the app, clears its task for a fresh launch, and records `TotalTime` from `am start -W`. It then waits for the first offer before starting the next run. The app process is cold; filesystem and OS caches are warm. No app data or compilation profiles are cleared between runs. There is no discarded warm-up sample.

## Observations

| Run | Activity display time |
| --- | ---: |
| 1 | 583 ms |
| 2 | 577 ms |
| 3 | 582 ms |
| 4 | 576 ms |
| 5 | 575 ms |

Median: **577 ms**. Range: **575–583 ms**. [Raw samples](measurements/startup-api36.json) and [reproduction script](../scripts/device_checks.py) are included.

## Interpretation

This measures Activity display, not time to a usable collection. The source deliberately delays requests by 450 ms, and the loading state may already be visible while the source is still working. Waiting for the first offer between samples verifies that loading completes; that polling interval is not used as a content-readiness timing metric.

Five debug-emulator samples establish a reproducible local observation, not a device performance budget. They do not establish release-build startup, frame pacing, jank, battery cost or performance on a physical low-end device. No optimization or before/after improvement is claimed.

## Next experiment

For a performance decision, measure a profileable release-equivalent build on a physical device with Macrobenchmark startup and frame timing metrics. Define catalog readiness separately from Activity display, collect traces, and compare the same workload and compilation mode before and after a specific change. The present measurements do not justify adding a baseline profile or claiming a speedup.
