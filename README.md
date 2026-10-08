![BUILD](https://github.com/wojta/arch-patterns/actions/workflows/build.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/cz.sazel.arch/arch-patterns?color=blue)](https://central.sonatype.com/artifact/cz.sazel.arch/arch-patterns)

# arch-patterns

Kotlin Multiplatform architecture library providing lightweight abstractions for clean architecture: `Result`, `IUseCase`, `ISuspendingUseCase`, `ILogicHandler`, and `Cause`.

## Gradle setup

Add dependency to `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("cz.sazel.arch:arch-patterns:0.1.0")
        }
    }
}
```

## Core Patterns

### Result<OK, FAIL>

Explicit result type representing success (`Ok`) or failure (`Fail`) without throwing runtime exceptions.

- `Result.Ok(data)`: Holds success value `OK`.
- `Result.Fail(reason)`: Holds failure reason `FAIL`.
- `UnitResult<FAIL>`: Typealias for `Result<Unit, FAIL>`.

#### Key Operations
- **Transform**: `mapOk`, `mapFail`, `map`
- **Chain**: `onOk`, `onFail` (flat-map style for suspending and non-suspending operations)
- **Consume**: `fold(onSuccess, onFailure)`
- **Catch exceptions**: `Result.catching(call, onError)`
- **Unwrap**: `dataOrThrow`, `dataOrNull`, `failReasonOrNull`

### IUseCase & ISuspendingUseCase

Abstractions for single domain operations in Clean Architecture.

- `IUseCase<INPUT, OUTPUT, FAIL>`: Synchronous use case returning `Result<OUTPUT, FAIL>`.
- `ISuspendingUseCase<INPUT, OUTPUT, FAIL>`: Suspending use case returning `Result<OUTPUT, FAIL>`.
- Use `Unit` as `INPUT` when no input parameter is needed.

Use cases typically define a sealed interface `Fail` for typed domain errors.

### ILogicHandler<INPUT>

Presentation layer interface for single-step operations. Calls use cases and handles presentation output or error formatting.

### Cause

Flattened exception data structure (`type`, `message`, `stackTrace`) safe for passing across module and layer boundaries. Use `Cause.of(throwable)` to flatten any `Throwable`.

---

## Usage Examples

### Defining a Use Case

```kotlin
class RenameDeviceUC(
    private val store: PreferencesStore,
) : ISuspendingUseCase<DeviceName, DeviceName, RenameDeviceUC.Fail> {

    sealed interface Fail {
        data class Rejected(val fault: String) : Fail
        data class Unwritable(val cause: Cause?) : Fail
    }

    override suspend fun invoke(input: DeviceName): Result<DeviceName, Fail> =
        if (input.value.isBlank()) {
            Result.Fail(Fail.Rejected("Name cannot be empty"))
        } else {
            store.saveName(input)
                .mapFail { Fail.Unwritable(it.cause) }
                .mapOk { input }
        }
}
```

### Chaining Use Cases with `onOk` and `mapFail`

```kotlin
class RegisterUserUC(
    private val validateEmail: EmailValidatorUC,
    private val saveUser: SaveUserUC,
) : ISuspendingUseCase<Email, User, RegisterUserUC.Fail> {

    sealed interface Fail {
        data object InvalidEmail : Fail
        data class StorageError(val cause: Cause?) : Fail
    }

    override suspend fun invoke(input: Email): Result<User, Fail> =
        validateEmail(input)
            .mapFail { Fail.InvalidEmail }
            .onOk { validEmail ->
                saveUser(validEmail).mapFail { Fail.StorageError(it.cause) }
            }
}
```

### Presentation with `ILogicHandler` and `fold`

```kotlin
class GetConfigLogicHandler(
    private val terminal: Terminal,
    private val deviceNameSetting: DeviceNameSettingUC,
) : ILogicHandler<ConfigKey> {

    override suspend fun invoke(input: ConfigKey) = when (input) {
        ConfigKey.DEVICE_NAME -> deviceNameSetting(Unit).fold(
            onSuccess = { setting -> terminal.println(setting.configuredName) },
            onFailure = { error -> terminal.warning("Error loading config: $error") },
        )
    }
}
```

### Wrapping Exceptional Calls with `Result.catching`

```kotlin
suspend fun fetchDataFromNetwork(): Result<String, Cause> =
    Result.catching(
        call = { httpClient.get("https://api.example.com/data") },
        onError = { Cause.of(it) }
    )
```

## License

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License.
