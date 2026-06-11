# WakeIQ

**A free, open-source Android alarm app that wakes you gently, the way your body actually wants to wake up.**

> Screenshots coming soon.

---

## The problem with normal alarms

Every alarm app on the market does the same thing: it fires at a fixed time, at full volume, regardless of what your body is doing. If you happen to be in deep sleep (N3 slow-wave), you get yanked out of it. The result is **sleep inertia**: that thick, foggy, exhausted feeling that can follow you for hours into your morning.

This isn't a minor inconvenience. Research consistently shows that waking during deep sleep impairs cognitive performance, mood, and reaction time in ways that persist well after you feel "awake". The fix isn't a louder alarm or more coffee. It's waking at the right moment in your sleep cycle.

---

## How WakeIQ is different

### The science

Human sleep cycles last approximately 90 minutes and pass through distinct stages:

| Stage | Type | Waking quality |
|-------|------|----------------|
| N1 | Light NREM | Easy, refreshed |
| N2 | Light NREM | Good |
| N3 | Deep slow-wave | Hard (sleep inertia) |
| REM | Dreaming | Vivid, moderate ease |

Waking during N1, N2, or REM feels natural. Waking during N3 feels like being pulled out of concrete.

### Smart Wake

You set a desired wake time and a **smart window** (default 20 minutes, configurable 10-30 min). During that window, WakeIQ monitors your device's accelerometer. More movement correlates with lighter sleep stages. When your motion crosses the configured threshold, the alarm fires early, catching you in a lighter phase before you sink back into N3.

If you sleep through the entire window without surfacing, the alarm fires at your hard deadline regardless. You will always wake up on time.

### Gradual escalation

Even at the hard deadline, WakeIQ never blasts you. Sound starts at a barely-audible whisper and rises slowly over a configurable ramp (5-30 min). Screen brightness mirrors this, climbing from 0% to 100% over the same period. The effect is closer to sunlight coming through a window than a fire alarm going off in your ear.

No proprietary hardware required. The accelerometer already in your phone is sufficient. This is the same approach used by established sleep apps with millions of users.

---

## Features

- Recurring alarms by day of week, or one-shot
- Smart Wake window with configurable width and motion sensitivity (Low / Medium / High)
- Gradual sound escalation with configurable ramp duration
- Screen brightness ramps from 0% to 100% alongside the sound
- 12 bundled sounds across two categories:
  - **Nature:** Birds Chirping, Rain, Thunderstorm, Ocean Waves, Farm Animals, Rooster
  - **Ambient:** Train Station, Airport, Cafe, Office Morning, Piano Melody, Singing Bowl
- Sound preview: tap any sound in the editor to hear it before saving
- Custom audio: pick any file from your device storage
- Snooze support
- Warm colour themes for alarm cards (Amber, Rose, Sage, Ocean, Violet) with automatically matched text contrast
- Blue light reduction tint in the evenings (6 pm to 6 am)
- Two sensible default alarms on first launch (Weekdays 6:00 am, Weekends 7:00 am), both off until you turn them on

---

## Free. Open source. Private. Forever.

**WakeIQ costs nothing.** There is no premium tier, no subscription, no locked features. Everything in the app is available to everyone, always.

**Your data never leaves your device.** WakeIQ collects no personal information, no usage analytics, no crash telemetry, and no sensor data. There are no remote servers. Nothing is ever transmitted anywhere. Your sleep patterns, your alarm schedule, and your habits are yours alone. We have no interest in them and no ability to access them even if we did.

**We will never sell, share, or monetise your information.** There is nothing to sell. The app has no analytics SDK, no advertising SDK, and no third-party data pipeline of any kind.

**You are never expected to pay.** If WakeIQ helps you wake up better, that's the entire point.

---

## Permissions

| Permission | Why it's needed | If denied |
|-----------|----------------|-----------|
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Fire the alarm at precisely the time you set | Alarm may be late by several minutes |
| `RECEIVE_BOOT_COMPLETED` | Re-register alarms after the device restarts | Alarms are lost on reboot |
| `WAKE_LOCK` | Keep the CPU alive so the alarm fires with the screen off | Alarm may not fire when the phone is idle |
| `FOREGROUND_SERVICE` | Run the alarm as a foreground service the OS won't kill | OS may silence the alarm mid-play |
| `POST_NOTIFICATIONS` | Show the persistent alarm notification | No visible notification while alarm is active |
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | Load custom audio files you choose | Cannot use custom sounds |
| `WRITE_SETTINGS` | Control screen brightness for the ramp effect | Brightness ramp is disabled |
| `HIGH_SAMPLING_RATE_SENSORS` | Accurate accelerometer readings for Smart Wake | Smart Wake detection is less precise |
| `DISABLE_KEYGUARD` | Show the alarm screen over the lock screen | Alarm shows behind the lock screen |

Every permission is explained in plain language before it is requested. If you deny one, the relevant feature degrades gracefully and the rest of the app continues to work normally.

---

## Building

Requires JDK 17 and Android SDK (compile SDK 35).

```bash
# Debug APK
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Instrumented tests (requires connected device or emulator)
./gradlew connectedDebugAndroidTest

# Lint + style checks
./gradlew ktlintCheck detekt lintDebug
```

The signed release APK is built automatically by the [release workflow](.github/workflows/release.yml) when you push a `v*` tag.

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Database | Room |
| Audio | Media3 / ExoPlayer |
| Async | Coroutines + Flow |
| Build | Gradle 8 (Kotlin DSL) |

---

## License

[MIT](LICENSE)
