# Structured Input Implementation ✅

## Overview

Implemented structured input format for LLM requests, separating **text prompts** from **JSON context data**. This makes the input more structured and easier for LLMs to parse.

## What Changed

### Before (Plain Text)
```
# INSTRUCTION
Player 'Steve' has written the message: mine stone

# ENVIRONMENT
## Nearby entities:
zombie, creeper
## Nearest blocks:
stone, dirt, grass
...
```

### After (Structured: Text + JSON)
```
Player 'Steve' has written the message: mine stone

=== CONTEXT DATA (JSON) ===
{
  "state": {
    "position": {"x": 100, "y": 64, "z": 200},
    "health": 20.0,
    "food": 20,
    "biome": "plains"
  },
  "inventory": {
    "hotbar": [{"type": "wooden_pickaxe", "count": 1, "slot": 0}],
    "mainInventory": [...],
    "armor": [...],
    "offHand": [...]
  },
  "nearbyBlocks": [
    {"type": "stone", "position": {"x": 101, "y": 64, "z": 200}, "mineLevel": "wood", "toolNeeded": "pickaxe"}
  ],
  "nearbyEntities": [
    {"id": 123, "name": "zombie", "isPlayer": false}
  ]
}
=== END CONTEXT ===
```

## Benefits

1. ✅ **Structured Data**: Context is now proper JSON, not text
2. ✅ **Easier Parsing**: LLMs can parse JSON more reliably than text
3. ✅ **Clear Separation**: Text instructions vs. structured data
4. ✅ **Type Safety**: JSON structure enforces data types
5. ✅ **Extensible**: Easy to add more context fields

## Implementation

### New File: `StructuredInputFormatter.java`

- Converts `WorldContext` to structured JSON
- Combines text prompt with JSON context
- Falls back to old format if JSON serialization fails

### Updated: `NPCEventHandler.kt`

- Now uses `StructuredInputFormatter.formatStructured()` instead of `PromptFormatter.format()`
- All user prompts now include structured JSON context

## Format Structure

```
[Text Prompt/Instruction]

=== CONTEXT DATA (JSON) ===
{
  "state": {...},
  "inventory": {...},
  "nearbyBlocks": [...],
  "nearbyEntities": [...]
}
=== END CONTEXT ===
```

## Context JSON Schema

```json
{
  "state": {
    "position": {"x": number, "y": number, "z": number},
    "health": number,
    "food": number,
    "biome": string
  },
  "inventory": {
    "hotbar": [{"type": string, "count": number, "slot": number}],
    "mainInventory": [...],
    "armor": [...],
    "offHand": [...]
  },
  "nearbyBlocks": [
    {
      "type": string,
      "position": {"x": number, "y": number, "z": number},
      "mineLevel": string,
      "toolNeeded": string
    }
  ],
  "nearbyEntities": [
    {
      "id": number,
      "name": string,
      "isPlayer": boolean
    }
  ]
}
```

## Backward Compatibility

- Falls back to old `PromptFormatter.format()` if JSON serialization fails
- Old format still works, but structured format is preferred

## Future Enhancements

Potential improvements:
- Use OpenAI's multimodal message format (text + JSON content parts)
- Add more context fields (time of day, weather, etc.)
- Compress large JSON contexts
- Cache context JSON for repeated requests

