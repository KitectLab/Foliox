# Foliox

Foliox is a Compose component library that provides page turning animations. It is a Kotlin Multiplatform (KMP) project built with Compose Multiplatform and Gradle Kotlin DSL, focusing on shared UI and core logic across Android, iOS, JVM/Desktop, and Web/Wasm targets.

## Features

- Compose Multiplatform shared UI.
- Kotlin Multiplatform targets for Android, iOS, JVM/Desktop, and Web/Wasm.
- Built-in animations: Curl, Slide (Slider), and Cover.
- Customizable `PageAnimation` API for your own effects.

## Gradle Setup

Group and artifact are defined by the build logic:

- Group: `io.github.kitectlab`
- Artifact: `foliox-core`

Versioning is tag-based for releases. When the build is not marked as a release, it falls back to:

- Snapshot version: `0.1.0-SNAPSHOT`

### Release Dependency

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.kitectlab:foliox-core:<release-version>")
}
```

### Snapshot Dependency

Snapshots can be consumed from Sonatype:

```kotlin
repositories {
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

dependencies {
    implementation("io.github.kitectlab:foliox-core:0.1.0-SNAPSHOT")
}
```

## Using Foliox

Foliox is designed to be consumed from the shared layer. The animation implementations and state live in `foliox-core/`. Use `PageAnimationContent` to render pages and drive gestures, and provide your own page content for `PageType.PREVIOUS`, `PageType.CURRENT`, and `PageType.NEXT`.

```kotlin
import io.github.kitectlab.foliox.PageType
import io.github.kitectlab.foliox.animation.PageAnimation
import io.github.kitectlab.foliox.animation.PageAnimationContent
import io.github.kitectlab.foliox.animation.rememberPageAnimationState

@Composable
fun Reader(pageIndex: Int, onPageChange: (Int) -> Unit) {
    val state = rememberPageAnimationState()
    PageAnimationContent(
        state = state,
        animation = PageAnimation.curl(),
        onCurrentChange = { type ->
            when (type) {
                PageType.NEXT -> onPageChange(pageIndex + 1)
                PageType.PREVIOUS -> onPageChange(pageIndex - 1)
                PageType.CURRENT -> Unit
            }
        },
        background = { /* optional background per page type */ }
    ) { type ->
        when (type) {
            PageType.PREVIOUS -> PreviousPage()
            PageType.CURRENT -> CurrentPage()
            PageType.NEXT -> NextPage()
        }
    }
}
```

## Animation Types

- Curl
- Slide (Slider)
- Cover

## Supported Kotlin Targets

Based on the current build configuration, Foliox publishes for:

- Android
- iOS (iosArm64, iosSimulatorArm64)
- JVM
- JavaScript (JS, browser)
- WebAssembly (wasmJs, browser)
- macOS (macosArm64)

## PageAnimation Customization

Foliox exposes a `PageAnimation` API that can be customized in the shared layer. You can provide your own animation implementation by defining a new `PageAnimation` and wiring it into the state/renderer as needed for your app.

## Modules

- `foliox-core/` - core cross-platform logic and components.
- `sampleApp/` - sample applications:
  - `sampleApp/androidApp/` - Android sample.
  - `sampleApp/iosApp/` - iOS sample.
  - `sampleApp/jvmApp/` - JVM/Desktop sample.
  - `sampleApp/sharedApp/` - shared KMP layer (includes `wasmJsMain`).

## Page Curl Acknowledgements

The page curl simulation is inspired by the work of AnliaLee and the following references:

- https://github.com/AnliaLee
- https://juejin.cn/post/6844903529715335182
- https://juejin.im/post/5a32ade0f265da43252954b2
- https://juejin.im/post/5a32af566fb9a0450671a7c0

## Requirements

- JDK 17+
- Android Studio (latest stable) + Android SDK/NDK as needed
- Xcode 15+ (for iOS run/signing)

## Development (Windows PowerShell)

```powershell
# List available Gradle tasks
./gradlew.bat tasks

# Full build (all modules)
./gradlew.bat build
```

## Sample Apps (Optional)

The sample apps are for integration reference and manual testing. They are not required to use the library.

```powershell
# Run the JVM/Desktop sample
./gradlew.bat :sampleApp:jvmApp:run

# Install Android sample (Debug) to a connected device/emulator
./gradlew.bat :sampleApp:androidApp:installDebug

# iOS: open and run in Xcode
Start-Process "sampleApp/iosApp/iosApp.xcodeproj"

# Web/Wasm (if the task exists; confirm via `tasks --all` first)
./gradlew.bat :sampleApp:sharedApp:wasmJsBrowserRun
```

## Dependency Management

All dependency versions are managed in `gradle/libs.versions.toml`. Add or upgrade dependencies there and reference them via `libs.xxx` in module `build.gradle.kts` files.

## Notes

- Compose/Web and Kotlin/Wasm are experimental and may change.
- If a task is missing, run `./gradlew.bat tasks --all` to find the equivalent.
