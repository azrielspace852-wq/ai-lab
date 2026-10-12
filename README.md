# Mini AI Lab — Android Native Port (Kotlin + Jetpack Compose)

This repository is a **native Android port work-in-progress** based on `mini-ai-lab-final-updated-v5(1).zip`. It does not include HTML/CSS/JavaScript assets and does not use WebView, a JS runtime, or a hybrid framework.

## Build
- Android Studio with JDK 17 and Android SDK Platform 36, or GitHub Actions.
- `gradle assembleDebug assembleRelease`
- Workflow: `.github/workflows/build.yml`
- Minimum SDK 30; target SDK 36.

## Native architecture
- Kotlin + Jetpack Compose UI.
- MVVM-style `LabViewModel`.
- Room / SQLite dataset persistence.
- Hilt dependency injection.
- Coroutines + Flow.
- Foreground service and partial WakeLock lifecycle scaffold.
- `.bin` v1 header/tensor parser and writer preserving `MALAB001` magic and little-endian Float32 tensor layout.

## Source feature inventory
The supplied web app contains these pages and capabilities:
- Main app: Dataset Explorer, nested folders, dataset editor, attachments, token counting, ZIP import/export, config, pre-training, fine-tuning, model list/import/export, chat, guide.
- Settings: language and dark/light theme.
- SDK guide / interactive SDK demo.
- Privacy policy.
- Smart pair deletion, autocomplete, line numbers, column ruler, fullscreen editor, long-press item actions, bulk move/delete, ZIP hierarchy and attachment association.
- Training architecture: context window, embedding dimension, hidden dimension, vocabulary size, batch size, learning rate, steps, seed; checkpoint and stop controls.
- JSON mask and per-dataset tokenization toggles.

## Important implementation status — read before use
This archive is a **native scaffold, not a feature-complete or production-ready port**. The UI routes are present for the source pages and common workflows, Room CRUD and basic text ZIP import/export are implemented, and the `.bin` v1 parser validates tensor bounds. However, parity is not complete:
- The neural-network training/backpropagation/Adam engine is not yet ported to Kotlin.
- Native inference/generation and actual model save/export from training are not implemented.
- Fine-tuning, checkpoint/resume, stop/cancel semantics, and real-time training progress are not wired to an optimizer.
- ZIP handling currently imports/exports text entries only and does not yet preserve nested folder relationships or media attachment associations.
- Media attachment CRUD, preview, tokenization, and object-storage policy are not implemented.
- JSON mask-based content extraction, tokenizer/autocomplete parity, full-screen editor, smart-pair deletion, long-press contextual actions, bulk move/delete, and line/column guides are not implemented.
- Settings persistence, complete English translations, and SDK runtime APIs are not complete.
- Room currently stores dataset text; large media and ZIP payloads are not separated into app-private object/file storage.
- The build has not been verified by a successful Android Gradle build in this environment. GitHub Actions should be treated as a build check, not proof of runtime correctness.

Do not treat this as satisfying the strict “100% feature parity” requirement until every item above is implemented and tested. No claim of zero bugs is made.
