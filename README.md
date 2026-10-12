# Mini AI Lab — Native Android

Pure native Android application (Kotlin + Jetpack Compose), minimum Android API 30 (Android 11). No WebView, HTML app shell, JavaScript runtime, or network backend is used by the app.

## Build with GitHub Actions
1. Push this repository to GitHub.
2. Open **Actions** and run **Android CI** (also runs on pushes and pull requests).
3. Download the `mini-ai-lab-debug-apk` artifact from a successful run.

The workflow runs `gradle :app:assembleDebug` using JDK 17. Android SDK components are installed by the hosted runner.

## Data and storage
- Dataset metadata and dataset text up to 2 MiB are stored in the app-private SQLite database.
- ZIP import preserves each dataset's relative directory as a `path` column. Media files and imported files larger than 2 MiB are placed in app-private `filesDir/object-storage/` (no cloud service or external storage permission).
- Model files are written to app-private `filesDir/models/` using `.bin` extension.
- App data is not exported to external storage unless a future explicit export feature is added. Uninstalling the app removes its private data.

## JSONL filtering
The training source parser extracts content from common JSONL shapes (`messages` with user/assistant/system roles, and `text`, `content`, `prompt`, `completion`, `response`, `output`, `instruction`, `input`). JSON keys and JSON serialization syntax are not sent to the tokenizer. Malformed object/array lines are skipped rather than trained as raw JSON. Plain-text lines are preserved.

## Training and model format limitation
Training runs in a foreground service with an ongoing progress notification and a partial wake lock (maximum four hours per acquisition; Android can still restrict background work). Notification permission is requested on Android 13+. Stop is cooperative and checked between training steps.

The included CPU trainer creates a compact `MAIB` version 1 `.bin` file containing a vocabulary and token-pair counts. This is a native mobile prototype format, **not** compatible with the browser app's tensor checkpoint format, PyTorch, ONNX, or a production transformer model. Training currently estimates pair statistics; it is not a neural-network backpropagation implementation. Do not treat its output as a trained GPT-like model. The format and trainer must be replaced/ported if strict binary compatibility with an existing `.bin` checkpoint is required.

## Build requirements
- JDK 17
- Android Gradle Plugin 8.7.3 / Gradle 8.9+
- Android SDK 35 (GitHub-hosted runner)
- Minimum runtime API 30
