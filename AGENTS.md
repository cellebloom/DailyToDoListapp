# Agent instructions — DailyToDoListapp

**Read [`docs/PRD.md`](docs/PRD.md) before changing anything.** It is the canonical spec:
requirements, an evidence-backed status matrix, invariants, and the work order. If it is
not in that file, it is not agreed.

## Non-negotiables

- **Java only.** No Kotlin. Min SDK 24, target/compile 34.
- **Never report a test as passing unless it ran in your session.** Past output in
  `app/build/test-results/` is not evidence for the current tree.
- **Task identity is the UUID** (`TaskWithDate.id`). Never address a task by list index;
  that was the data-corruption bug the current storage was built to remove.
- **`-1` means Anytime.** In Java `-1 / 60 == 0`, so check `isAnytime()` before deriving an
  hour from `startMinute`.
- **Display must never mutate storage.** Timeline clamping is a view concern.
- **New UI uses theme attributes and colour resources**, never hex. There are 169 legacy
  hex literals to remove; do not add more.
- **Every new user-facing string goes in `values/strings.xml` *and* `values-zh/strings.xml`.**
  The two key sets must stay identical (currently 181 / 181).
- Update the status matrix in `docs/PRD.md` in the same change that alters behaviour.

## Building

The bundled Android Studio JBR is broken on this machine, so Gradle needs the standalone
JDK. See `docs/PRD.md` §0.2 for the exact commands and the current sandbox caveat: Gradle
needs write access to `%USERPROFILE%\.gradle\native\*`, which the default workspace-write
sandbox denies. If you cannot get that approval, say the tests were not run.

## Working alongside other sessions

Several sessions may work in this repo at once. Pick one stream from `docs/PRD.md` §6 and
stay inside its files. `MainActivity.java` is the hot file — only one stream should touch
it at a time. Do not "tidy up" files owned by another stream in the same turn.
