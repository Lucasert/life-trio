# Remember Forever

Android-first personal productivity app with three local-first modules:

- Memo: quick notes, Markdown-style rich text shortcuts, checkboxes, tags, pinning, search, image attachment, speech-to-text input, and memo-to-plan conversion.
- Ledger: fast expense/income entry, category chips, notes, monthly budget warning, pie chart, annual income/expense line chart.
- Plan: recurring plans for daily, weekly, monthly, every N days, and China legal workdays, with today tasks, completion, skip, carry-forward, heatmap, and local workday overrides.

## Tech Stack

- Kotlin
- Jetpack Compose
- Room
- Compose Navigation
- Android system speech recognizer

## Run

Open this folder in Android Studio, let Gradle sync, then run the `app` configuration on an emulator or Android device.

Useful commands when Gradle is available:

```powershell
gradle testDebugUnitTest
gradle assembleDebug
```

## Notes

- Data is stored locally in Room. There is no account system or cloud sync.
- Memo rich text is stored as Markdown-style text for a stable MVP.
- Images are copied into the app private files directory; Room stores the local path.
- The built-in China workday table covers 2026, and custom date overrides can be maintained from the Plan screen.
