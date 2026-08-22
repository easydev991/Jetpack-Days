# opencode: `instructions` field does NOT support `{file:...}` substitution

## Invariant

In `~/.config/opencode/opencode.json`, the top-level `instructions: string[]` field expects **plain paths** (absolute or `~/...`), NOT `{file:...}` tokens.

```jsonc
// correct — works
"instructions": ["~/.config/opencode/includes/hound-usage.md"]

// WRONG — silently broken
"instructions": ["{file:/Users/Oleg991/.config/opencode/includes/hound-usage.md}"]
```

## Why

`ConfigVariable.substitute` (`packages/opencode/src/config/variable.ts:34-91`) runs on raw JSONC text BEFORE parsing, replacing `{file:...}` tokens with file body indiscriminately across all string fields.

Then `Instruction.systemPaths` (`packages/opencode/src/session/instruction.ts:135-150`) re-interprets each `instructions` entry: absolute path → `fs.glob(basename, {cwd: dirname})`, otherwise → `globUp` from project root. A substituted file-body string fails both → entry silently dropped from LLM context.

## Verifying

After editing, `jq '.instructions' ~/.config/opencode/opencode.json` must print literal paths, not the file body. Also check no `{file:...}` tokens survive in the loaded config.

## When this matters

- Configuring ambient prompt fragments via `~/.config/opencode/includes/`
- Debugging why shared instructions never reach the agent
- Reviewing opencode configs from others (this pattern is easy to copy from other tools that DO support `{file:...}`)

## Note on the `{file:...}` mechanism itself

It DOES work for fields that take literal strings (e.g. `username`, custom provider options). The breakage is specific to fields whose runtime parser re-interpret entries as paths/globs — `instructions` is one such field. Other opencode fields may be similarly affected; check the consumer code before using `{file:...}` there.
