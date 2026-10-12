# Feature Parity Checklist — source: supplied Mini AI Lab v5 ZIP

Legend: `[x]` implemented in this scaffold; `[ ]` not yet parity-complete.

## Pages
- [x] Main app shell and Dataset page
- [x] Configuration page
- [ ] Pre-Train with actual training engine, loss/progress, checkpoint and cancellation
- [ ] Fine-Tune with selectable base model and save-new/update modes
- [ ] Model list, persistent metadata, delete-all, refresh, model export
- [ ] Chat inference and multiformat output
- [x] Guide page route
- [x] Settings route and basic theme/language toggles
- [x] SDK native guide route
- [x] Privacy policy route

## Dataset management
- [x] Room-backed text dataset CRUD
- [ ] Nested folders and breadcrumb navigation
- [ ] Media attachments and per-attachment caption/tokenization controls
- [ ] JSON mask editing and content extraction
- [ ] Token count using the source tokenizer
- [ ] ZIP nested structure preservation and attachment linking
- [ ] ZIP threshold policy (<2 MB SQLite, >=2 MB app-private object/file storage)
- [ ] Bulk selection, bulk move/delete
- [ ] Long-press contextual action menu
- [ ] Fullscreen editor
- [ ] Smart-pair delete for (), {}, [], single and double quotes
- [ ] Autocomplete parity, line numbers and column ruler

## Model / training
- [x] `.bin` v1 header and Float32 tensor bounds validation
- [ ] Kotlin tokenizer parity
- [ ] TinyLM forward/backward + Adam training port
- [ ] Model generation / sampling parity
- [ ] Model persistence, import/export lifecycle
- [ ] Fine-tuning, checkpoint/resume, stop/cancel
- [ ] Background training progress wired to the actual optimizer
- [ ] JSONL metadata/content separation verified with tests
- [ ] Native SDK usable directly by other Android apps

## Quality gates
- [ ] `./gradlew assembleDebug` passes
- [ ] `./gradlew assembleRelease` passes
- [ ] Unit tests for `.bin` valid/corrupt/truncated cases
- [ ] Round-trip ZIP hierarchy + attachment tests
- [ ] Golden tests comparing tokenizer/inference with web implementation
- [ ] Device test for background execution, notification permission, and screen-off training
