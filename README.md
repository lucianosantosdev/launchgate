# launchgate

First-launch gate for Compose Multiplatform apps: show **onboarding** to a brand-new user, a
**per-release changelog** to a returning one, and nothing at all to everyone else — decided once,
from one stored version code, so the two can never both appear.

The library ships **no content**: no copy, no strings, no colors, no version numbers. It provides
the rules, the storage and two carousel screens; the app supplies everything a user reads.

Targets: Android, iOS (arm64 + simulator), desktop JVM.

## Install

Either take it as a git submodule and build it from source:

```bash
git submodule add git@github.com:lucianosantosdev/launchgate.git launchgate
```

```kotlin
// settings.gradle.kts
include(":launchgate")

// consumer build.gradle.kts
commonMain.dependencies {
    implementation(projects.launchgate)
}
```

…or depend on a published build:

```kotlin
commonMain.dependencies {
    implementation("dev.lssoftware:launchgate:0.1.0")
}
```

Publishing:

```bash
./gradlew :launchgate:publishToMavenLocal                       # inner loop
./gradlew :launchgate:publishAllPublicationsToGitHubPackagesRepository
```

GitHub Packages credentials come from `GITHUB_ACTOR`/`GITHUB_TOKEN`, or `gpr.user`/`gpr.token` in
`~/.gradle/gradle.properties`. Consumers add the repository:

```kotlin
maven("https://maven.pkg.github.com/lucianosantosdev/launchgate") { credentials { /* … */ } }
```

## Define your content

```kotlin
val releaseNotes = listOf(
    ReleaseNote(
        versionCode = 42,                       // must not change once shipped
        versionName = "1.7.5",
        title = "Less tapping, more watching",
        changes = listOf("Hold an arrow and it keeps going", "Connect automatically"),
    ),
    ReleaseNote(versionCode = 36, versionName = "1.7.1", title = "Roku TVs", changes = listOf("…")),
)
```

A release that did unrelated things can carry **several pages**, each with its own headline —
better than one page of nine bullets. The single-page form above is shorthand for a one-page list:

```kotlin
ReleaseNote(
    versionCode = 42,
    versionName = "1.7.5",
    pages = listOf(
        ReleaseNotePage("Faster D-pad", listOf("Hold an arrow and it keeps going")),
        ReleaseNotePage("Smarter connect", listOf("Opens on the TV you use most")),
    ),
)
```

A page's body is a **composable slot**, so it is not limited to bullets — show a screenshot, an
animation, a before/after, anything:

```kotlin
ReleaseNotePage(title = "Faster D-pad") {
    Image(painterResource(R.drawable.dpad_demo), contentDescription = null)
    Spacer(Modifier.height(12.dp))
    ChangeList(listOf("Hold an arrow and it keeps going"))   // the default bullet renderer
}
```

The `changes` overload above is shorthand for exactly that slot filled with `ChangeList`. Custom
content renders inside the page's scrolling column under the title, and can read
`LocalWhatsNewColors` to match the screen it sits in without being passed anything.

An update spanning releases 37 (2 pages) and 38 (3 pages) shows all five, newest release first,
each page labelled with the release it belongs to. An entry with no pages is dropped rather than
rendered blank.

val onboardingPages = listOf(
    OnboardingPage(
        title = "One remote for every TV",
        description = "No extra hardware, no account.",
        illustration = { Icon(Icons.Default.Tv, contentDescription = null) },
    ),
)
```

Releases with nothing worth saying simply have no entry — the gate skips them rather than showing
a blank page.

## Wire up the gate

```kotlin
class StartupViewModel(dataStore: DataStore<Preferences>) : ViewModel() {

    private val gate = VersionGate.create(
        dataStore = dataStore,
        currentVersionCode = BuildConfig.VERSION_CODE,   // passed in; the library never reads it
        keyName = "myapp_last_seen_version",             // namespace it per app
        releaseNotes = { releaseNotes },                 // suspend: resolve localized copy here
        existingUserProbe = null,                        // see below
    )

    private val _start = MutableStateFlow<StartDestination?>(null)
    val start: StateFlow<StartDestination?> = _start.asStateFlow()

    init { viewModelScope.launch { _start.value = gate.resolve() } }

    fun onFinished() = viewModelScope.launch {
        gate.markSeen()
        _start.value = StartDestination.None
    }
}
```

Android apps with no DataStore of their own can use the `Context` overload, which creates one:

```kotlin
VersionGate.create(context, currentVersionCode = BuildConfig.VERSION_CODE, dataStoreName = "myapp_gate")
```

**Adding the gate to an app that already has users?** Pass an `existingUserProbe` — any evidence
only a real user would have. Without it, everyone looks brand new on the update that introduces the
gate and gets onboarded:

```kotlin
existingUserProbe = VersionGate.stringPreferenceProbe(dataStore, "saved_account")
```

## Route on the result

The library has no navigation dependency: it hands you a destination and calls back when done.

```kotlin
when (val destination = start) {
    null -> Unit                       // still reading — keep your splash up
    StartDestination.Onboarding -> OnboardingScreen(
        pages = onboardingPages,
        onFinished = { viewModel.onFinished(); goHome() },
        labels = CarouselLabels(next = stringResource(R.string.next), finish = stringResource(R.string.start)),
    )
    is StartDestination.WhatsNew -> WhatsNewScreen(
        releaseNotes = destination.releaseNotes,   // already filtered, newest first
        onFinished = { viewModel.onFinished(); goHome() },
        onSkip = { viewModel.onFinished(); goHome() },   // optional X in the corner
        labels = CarouselLabels(
            next = stringResource(R.string.next),
            finish = stringResource(R.string.got_it),
            skip = stringResource(R.string.close),       // accessibility label for the X
        ),
        title = stringResource(R.string.whats_new),
    )
    StartDestination.None -> Home()
}
```

If you route through a NavHost, resolve the start destination **once** and remember it: a NavHost
rebuilds its graph when `startDestination` changes, resetting the back stack the moment the gate
turns `None`.

## Theming

Every label is a parameter and every color defaults to a `MaterialTheme` role, so an app that
themes normally passes neither. Override per screen:

```kotlin
WhatsNewScreen(
    …,
    colors = WhatsNewColors.default().copy(versionLabel = MyBrand.Accent),
    indicator = IndicatorStyle.default().copy(activeWidth = 32.dp),
)
```

`PagerCarousel` is public too — build a third paged flow on it rather than reimplementing the
pager, dots and button.

Passing `onSkip` puts a dismiss control in the top corner that leaves the whole flow at once,
without paging to the end. It is a separate callback from `onFinished` so you can tell "read it"
from "skipped it"; most apps pass the same lambda, since either way the release has been offered.
Leave it out — the default — for a flow meant to be read through, such as onboarding. The X is
drawn, not imported, so it costs no Material-icons dependency.

## What the gate decides

| Stored version | Probe | Result |
|---|---|---|
| absent | absent/false | `Onboarding` |
| absent | true | `None`, and the version is recorded (an existing user from before the gate) |
| `>= current` | — | `None`, recorded (up to date, downgrade, reinstall, or a debug build) |
| `< current`, no entries in range | — | `None`, recorded (rebuilds, fix-only releases) |
| `< current`, entries in range | — | `WhatsNew(notes)`, newest first |

`markSeen()` is the only writer, and the mark is never cleared — so onboarding cannot come back.

## Version codes that are not stable

If your CI derives the version code per build (a run number, a timestamp), the same release rebuilt
gets a different code. Key each `ReleaseNote` on the **lowest code that release can carry**; the
half-open `(lastSeen, current]` range then treats a rebuild as "not news". lg-remote's
`releaseCode(tag)` is one example of that mapping — it belongs in the app, not here.
