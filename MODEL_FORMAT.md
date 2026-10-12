# MAIB `.bin` format v1

All integers are big-endian (`DataOutputStream`).

1. Four bytes ASCII `MAIB`.
2. `int32` version (=1), `int32` vocabulary count, `int32` completed steps, `float64` final running-average metric.
3. For each vocabulary item: `int32` UTF-8 byte length, followed by UTF-8 bytes.
4. `int32` pair-count entries, then repeated `int64` pair key (`tokenA` in high 32 bits, `tokenB` in low 32 bits) and `int32` frequency.

This is a prototype token-pair statistics file, not a neural language-model tensor checkpoint.
