# WakeIQ

**A free, open-source Android alarm app that wakes you gently, the way your body actually wants to wake up.**

> Screenshots coming soon.

---

## The problem with normal alarms

Every alarm app on the market does the same thing: it fires at a fixed time, at full volume, regardless of what your body is doing. If you happen to be in deep sleep (N3 slow-wave), you get yanked out of it. The result is **sleep inertia**: that thick, foggy, exhausted feeling that can follow you for hours into your morning.

This isn't a minor inconvenience. Research consistently shows that waking during deep sleep impairs cognitive performance, mood, and reaction time in ways that persist well after you feel "awake". The fix isn't a louder alarm or more coffee. It's waking at the right moment in your sleep cycle.

---

## How WakeIQ is different

Most alarm apps do one thing: fire at a fixed time, at full volume, regardless of what
your body is doing. If you happen to be in deep sleep, you get yanked out of it hard.
The result is sleep inertia - that thick, foggy, exhausted feeling that can persist for
30 minutes or more after waking, and in some cases for up to four hours.

WakeIQ's approach is grounded in six decades of peer-reviewed sleep science. The gradual
sound ramp is not a cosmetic feature. It is the mechanism.

### The science of auditory arousal thresholds

Your brain's sensitivity to sound during sleep is not constant. It varies dramatically
and predictably with sleep stage.

Auditory arousal thresholds (AATs) - the minimum sound intensity required to rouse a
sleeper - have been studied consistently since the 1960s. The findings have been
replicated many times and are not in dispute:

- **AATs increase progressively as NREM sleep deepens** from stage N1 through N2 to
  N3 (deep slow-wave sleep). The brain in N3 requires significantly louder sound to
  be roused than in N1 or N2.
- **N3 has the highest AATs of all sleep stages.** REM and N2 have comparable, lower
  thresholds.
- **AATs decline over the course of the night** as sleep pressure is satisfied,
  meaning the same person is harder to rouse in the early night and progressively
  easier toward morning.

Primary sources:
- Williams H.L. et al. (1964). Responses to auditory stimulation, sleep loss and the
  EEG stages of sleep. *Electroencephalography and Clinical Neurophysiology*, 16,
  269-279. [DOI: 10.1016/0013-4694(64)90109-9](https://doi.org/10.1016/0013-4694(64)90109-9)
  (PubMed: [14128872](https://pubmed.ncbi.nlm.nih.gov/14128872/))
- Rechtschaffen A., Hauri P., Zeitlin M. (1966). Auditory awakening thresholds in REM
  and NREM sleep stages. *Perceptual and Motor Skills*, 22(3), 927-942.
  [DOI: 10.2466/pms.1966.22.3.927](https://doi.org/10.2466/pms.1966.22.3.927)
- Ferrara M. et al. (2000). Auditory arousal thresholds after selective slow-wave sleep
  deprivation. *Clinical Neurophysiology*, 110(12), 2148-2152.
  [DOI: 10.1016/S1388-2457(99)00171-6](https://doi.org/10.1016/S1388-2457(99)00171-6)

### The science of sleep inertia

Waking from N3 is not merely unpleasant. It impairs measurable cognitive function.

Sleep inertia is the transient physiological state immediately after awakening,
characterised by reduced vigilance, slowed reaction time, impaired decision-making, and
degraded motor performance. It is worst when waking from deep slow-wave sleep (N3).

Key findings:
- Waking from N3 produces the most severe cognitive disruption of any sleep stage.
  fMRI studies show altered functional brain connectivity immediately post-N3 awakening,
  with reduced segregation between brain networks responsible for attention and
  sensory-motor control. These effects dissipate over the first 30 minutes but can
  persist for up to four hours.
- Participants waking from slow-wave sleep showed a **41% reduction in cognitive
  performance** immediately after waking, compared to those waking from N2 who showed
  performance similar to being already awake.

Sources:
- Trotti L.M. (2017). Waking up is the hardest thing I do all day: Sleep inertia and
  sleep drunkenness. *Sleep Medicine Reviews*, 35, 76-84.
  [DOI: 10.1016/j.smrv.2016.08.005](https://doi.org/10.1016/j.smrv.2016.08.005)
- Vallat R. et al. (2019). Hard to wake up? The cerebral correlates of sleep inertia
  assessed using combined behavioral, EEG and fMRI measures. *NeuroImage*, 184,
  266-278. [DOI: 10.1016/j.neuroimage.2018.09.033](https://doi.org/10.1016/j.neuroimage.2018.09.033)
- Hilditch C.J., McHill A.W. (2019). Sleep inertia: current insights. *Nature and
  Science of Sleep*, 11, 155-165.
  [DOI: 10.2147/NSS.S188911](https://doi.org/10.2147/NSS.S188911)
- Stampi C. (1992). cited in Hilditch & McHill 2019: participants waking from SWS
  showed 41% reduction in performance vs pre-nap baseline; those waking from N2
  showed no significant impairment.

### How the gradual ramp works

WakeIQ does not attempt to detect your sleep stage. It does not need to.

The gradual ramp exploits auditory arousal threshold variation passively:

1. The ramp starts at a barely audible level, well below the arousal threshold of any
   sleep stage including the lightest.
2. Volume rises slowly over the configured duration (5-30 minutes).
3. While you are in N3, the sound is below your brain's arousal threshold. It passes
   unnoticed. You are not disrupted and you do not wake.
4. As you naturally cycle into a lighter stage (N1, N2, or REM) your arousal threshold
   drops. The sound, now at a moderate level, is sufficient to rouse you gently.
5. You surface gradually, not abruptly. The brain has time to transition from sleep
   to wakefulness rather than being jolted.

You will always wake on time. If you remain in a lighter stage through the entire ramp,
the sound reaches full volume by your hard deadline. You do not need to be in any
particular sleep stage for this to work - the mechanism operates regardless.

### Sound choice matters too

WakeIQ offers melodic and natural sounds rather than beeps for a reason grounded in
research.

A 2020 peer-reviewed study (McFarlane et al.) found that alarm sounds rated as melodic
by participants showed a statistically significant relationship to reductions in
perceived sleep inertia, while neutral sounds (neither melodic nor unmelodic) showed a
significant relationship to *increases* in perceived sleep inertia.

A subsequent 2020 study (McFarlane et al., PLoS ONE) extended this finding, examining
the specific musical elements - melody and rhythm - that drive the effect.

Sources:
- McFarlane S.J. et al. (2020). Alarm tones, music and their elements: Analysis of
  reported waking sounds to counteract sleep inertia. *PLoS ONE*, 15(1), e0215788.
  [DOI: 10.1371/journal.pone.0215788](https://doi.org/10.1371/journal.pone.0215788)
  (PMC: [PMC6986749](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6986749/))
- McFarlane S.J. et al. (2020). Auditory countermeasures for sleep inertia: Exploring
  the effect of melody and rhythm in an ecological context. *PMC7445849*.
  [PMC: PMC7445849](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC7445849/)

The bundled sounds in WakeIQ - birdsong, rain, singing bowl, piano melody - are chosen
to be melodic and natural rather than harsh and repetitive. You can also choose any
audio file from your device. Familiar, meaningful sounds work particularly well: the
sleeping brain's arousal threshold in REM responds to stimulus meaningfulness (see
Rechtschaffen et al. 1966 above), so a personally chosen sound is more effective at the
same volume than a generic tone.

### Wear OS enhancement (optional)

If you pair a Wear OS watch, WakeIQ can use wrist accelerometer data during the smart
window to detect confirmed light-sleep movement and trigger the ramp slightly earlier -
catching you in a verified light phase rather than waiting for the ramp to reach the
right level. This is an enhancement on top of the ramp mechanism, not a replacement for
it. The ramp works fully and correctly without a watch. Wear OS detection gives you an
earlier, softer start if you are already in a light phase when the window opens.

Note for users placing their phone on or under the mattress: the phone's own
accelerometer can also detect movement transmitted through the mattress surface, similar
to the approach used by apps such as Sleep as Android. This is less accurate than wrist
detection but provides some additional signal. WakeIQ uses this only as a secondary
trigger, never as the primary mechanism.

---

## Features

- Recurring alarms by day of week, or one-shot
- Smart window: configurable width (10-30 min, default 20 min)
- Gradual sound ramp: configurable duration (5-30 min, default 15 min) - the primary
  wake mechanism, grounded in auditory arousal threshold science
- Screen brightness ramps from 0% to 100% alongside the sound
- Motion sensitivity: Low / Medium / High (Wear OS wrist detection or on-mattress phone
  accelerometer, used as an optional early-trigger enhancement)
- 21 bundled melodic and natural sounds, chosen for their sleep-inertia-reducing
  properties (see science above):
  * **Nature:** Birds & Light Rain, Forest Birdsong, Ocean Coast, Bubbling Stream, City Rain, Thunderstorm
  * **Farm:** Sheep Horses & Dogs, Crickets at Night, Morning Roosters, Mournful Cows, Summer Field at Dusk
  * **Music:** Lament of the Vaquero, Music Box, Piano, Indian Harp, Wind Chimes
  * **Ambient Places:** Cafe, Airport Terminal, Busy Street, Office, Train Station
- Sound preview: tap any sound in the editor to hear it before saving
- Custom audio: pick any file from your device storage
- Snooze support
- Warm colour themes for alarm cards (Amber, Rose, Sage, Ocean, Violet) with
  automatically matched text contrast
- Blue light reduction tint in the evenings (6 pm to 6 am)
- Two sensible default alarms on first launch (Weekdays 6:00 am, Weekends 7:00 am),
  both off until you turn them on

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
| `HIGH_SAMPLING_RATE_SENSORS` | Wrist/mattress accelerometer readings for optional early-trigger detection during the smart window | Optional Wear OS and on-mattress detection is less precise. Gradual ramp still works fully without it. |
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
