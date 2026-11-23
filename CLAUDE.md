# TraderLens

Paper trading simulator for the BTC/USD spot market. Live market data comes from the public Binance API (REST for historical klines, WebSocket for live prices). All trading is simulated locally and offline. The user starts with 10,000 USD and 0 BTC.

## Tech stack

* Kotlin Multiplatform: `sharedLogic` module targets Android and iOS
* Android UI: Jetpack Compose with Material 3 (in `androidApp`)
* Networking: Ktor (REST and WebSockets), kotlinx.serialization
* Local storage: SQLDelight
* Concurrency: Coroutines and Flow
* DI: Koin
* Navigation: Navigation 3 (multiplatform, with lifecycle ViewModel integration)
* Images: Coil 3 (coil-compose, coil-network-ktor3)
* Chart: custom Compose Canvas candlestick chart (no chart library)
* Dependencies are managed only through `gradle/libs.versions.toml`

## Architecture rules

Clean Architecture with SOLID principles. Package root: `com.ceylonapz.traderlens`

* `domain` (model, repository, usecase): pure Kotlin, no Ktor, SQLDelight, Android or platform imports
* `data` (remote, local, repository): implements domain repository interfaces
* `di`: Koin modules
* `presentation` (in androidApp): ViewModels expose a single immutable `UiState` via `StateFlow`; screens are stateless where possible
* Dependencies point inward only: presentation to domain, data to domain. Domain never depends on data
* One use case per class, one responsibility per class

## Money and trading rules (critical)

* Never use `Double` or `Float` for balances, prices or quantities
* Store USD as `Long` cents and BTC as `Long` satoshis (1 BTC = 100,000,000 sats), or use a decimal type; round BTC quantities down
* Data comes from BTCUSDT endpoints, but the UI always displays the quote currency as USD
* A trade must be atomic: wallet updates and the ledger entry happen in one database transaction
* The trading engine must be guarded against concurrent execution (Mutex)
* Market orders execute at the latest live WebSocket price, with no network request during execution
* Block trading when the price is stale or the socket is disconnected
* Balances can never go negative; validate amount, insufficient funds and minimum order size

## Edge cases to keep in mind

Network loss, WebSocket disconnects with exponential backoff and jitter, gap filling with REST after reconnect, kline merge by open time, offline launch with cached data, app background and foreground lifecycle, process death, rotation, high frequency price updates (throttle UI), double taps on trade buttons.

## Workflow rules for Claude

* Work in small steps: one logical change at a time
* After each step, stop, summarize what changed and why, and wait for my review
* Do not run `git commit` or `git push`. I review and commit myself. Suggest a Conventional Commits message (feat, fix, chore, test, refactor, docs)
* Do not add a dependency without asking first
* Write unit tests for domain logic and the trading engine
* Never use em dashes or en dashes in README or documentation text

## Code style

* Keep code and comments short. Write code like a senior engineer, not a tutorial
* Comments explain why, never what. Do not restate what the code does
* KDoc only for public functions whose behavior is not obvious from the
  name and signature, and keep it to one line
* No comments on obvious code, getters, simple models or tests
* Explain a design decision once, in the most relevant place, not everywhere
* Single-line comments use `//`; multi-line comments use `/** */`

## Commands

* Build Android: `./gradlew :androidApp:assembleDebug`
* Run shared tests: `./gradlew :sharedLogic:allTests`
* Release APK: `./gradlew :androidApp:assembleRelease` then copy to `dist/`
