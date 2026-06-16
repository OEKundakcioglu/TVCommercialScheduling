# Moves Package

## Optimized Attention Checks

Attention is only affected for the first 3 and last 3 positions in a sequence. Moves must not
re-check all commercials — only those whose attention status could actually change.

### InsertMove (insert at position `k`, old length `n`)

- Always check the inserted scheduling at position `k` with new length `n+1`.
- **F-type** (only if `k <= 2`): Commercials at old positions `[k, min(2, n-1)]` are shifted right,
  so check them at `pos+1` with length `n+1`.
- **L-type** (only if `k >= n-2`): Commercials at old positions `[max(n-3, 0), k-1]` are not shifted
  but the sequence grew, so check them at same `pos` with length `n+1`.

### General Principle

Other moves (RemoveMove, TransferMove, SwapMoves, etc.) should follow the same pattern: identify
which positions are affected by the move and only check attention for those positions. F-type
attention (F1, F12, F123) only matters for positions 0–2. L-type attention (L1, L12, L123) only
matters for the last 3 positions (length-1, length-2, length-3).
