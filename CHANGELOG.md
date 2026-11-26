# Changelog

## Unreleased

### Added

- `Client` with classpath scanning, manual registration and automatic application command registration.
- Annotations for slash commands, subcommand groups, options, context menus, component handlers, prefix commands, event listeners and guards.
- Option types inferred from Kotlin types, autocompletion through `Autocompleter`.
- Coroutine helpers: `RestAction.await()`, `JDA.await()` and `awaitModal()`.
- `kotdex-compose`: Components V2 messages written as composables, updated when their state changes.
- Builders for embeds, buttons, select menus, modals and messages.

### Changed

- Requires JDA 6 and Java 17.
- Split into the `kotdex-core` and `kotdex-compose` modules.

### Fixed

- Embeds without a title, text input builders recursing forever, text inputs limited to one character, select menu defaults being reset and timestamps in milliseconds.
