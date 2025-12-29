# CraftAgent

A Minecraft Fabric mod that brings intelligent AI-powered NPCs to your world. NPCs can understand context, perform tasks, and interact with players using Large Language Models (LLMs).

> **Note**: This project takes inspiration from and includes code from [SecondBrain](https://github.com/sailex428/SecondBrain/tree/main/src/main).

## Features

- 🤖 **AI-Powered NPCs**: NPCs use LLMs (Ollama, LM Studio) to understand context and make decisions
- 🎮 **Autonomous Actions**: NPCs can mine, craft, build, fight, and interact with the world
- 💬 **Conversation Memory**: Persistent conversation history stored in SQLite
- 🌍 **World Context Awareness**: NPCs understand their surroundings (blocks, entities, inventory)
- 🔧 **Multi-LLM Support**: Supports Ollama and LM Studio (OpenAI-compatible APIs)
- 🎨 **Client-Server Architecture**: GUI configuration on client, NPC logic on server
- 🛠️ **Tool Calling**: Uses modern tool calling API for command execution
- 📊 **Structured I/O**: Structured input (text + JSON) and structured output for reliable parsing

## Requirements

- **Minecraft**: 1.20.1 or 1.21.8 (check `gradle.properties` for supported versions)
- **Java**: 17 (for 1.20.1) or 21 (for 1.21.8+)
- **Fabric Loader**: 0.17.3+
- **LLM Service**: Either Ollama or LM Studio running locally

## Installation

### For Players

1. Download the latest JAR from [Modrinth](https://modrinth.com/mod/craftagent) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/craftagent)
2. Place the JAR in your `mods` folder
3. Install and start either:
   - **Ollama**: Download from [ollama.ai](https://ollama.ai) and run `ollama serve`
   - **LM Studio**: Download from [lmstudio.ai](https://lmstudio.ai) and start the server
4. Launch Minecraft and enjoy!

### For Developers

1. Clone the repository:
   ```bash
   git clone https://github.com/sailex428/CraftAgent.git
   cd CraftAgent
   ```

2. Ensure you have the correct Java version:
   - For Minecraft 1.20.1: Java 17
   - For Minecraft 1.21.8+: Java 21

3. Build the project:
   ```bash
   # Windows
   .\gradlew.bat :1.21.8:build
   
   # Linux/Mac
   ./gradlew :1.21.8:build
   ```

4. Find the compiled JAR:
   ```
   versions/1.21.8/build/libs/craftagent-1.21.8-v3.1.5-alpha.jar
   ```

## Building

### Build for Specific Minecraft Version

```bash
# Build for 1.21.8
.\gradlew.bat :1.21.8:build

# Build for 1.20.1
.\gradlew.bat :1.20.1:build
```

### Build All Versions

```bash
.\gradlew.bat build
```

### Run in Development

```bash
# Run client
.\gradlew.bat :1.21.8:runClient

# Run server
.\gradlew.bat :1.21.8:runServer
```

## Usage

### Creating an NPC

1. Start your LLM service (Ollama or LM Studio)
2. In-game, open the config GUI (default key: check controls)
3. Click "Create NPC" or use command:
   ```
   /npc create <name> <llmType>
   ```
   - `name`: NPC's display name
   - `llmType`: `ollama` or `lm_studio`

### Configuring NPCs

1. Open the config GUI
2. Select an NPC from the list
3. Configure:
   - **Name**: Display name
   - **LLM Type**: Ollama or LM Studio
   - **Model**: Model name (e.g., `llama3`, `gpt-4`)
   - **Character Traits**: Personality description
   - **URL**: LLM service URL (defaults provided)

### Commands

- `/npc create <name> <llmType>` - Create a new NPC
- `/npc remove <name>` - Remove an NPC (keeps data)
- `/npc delete <name>` - Permanently delete NPC and data
- `/npc list` - List all NPCs

## LLM Setup

### Ollama

1. Install Ollama from [ollama.ai](https://ollama.ai)
2. Start the service:
   ```bash
   ollama serve
   ```
3. Pull a model:
   ```bash
   ollama pull llama3
   ```
4. Configure NPC with:
   - **LLM Type**: `ollama`
   - **Model**: `llama3` (or your preferred model)
   - **URL**: `http://localhost:11434` (default)

### LM Studio

1. Install LM Studio from [lmstudio.ai](https://lmstudio.ai)
2. Download a model in LM Studio
3. Start the local server (usually on port 1234)
4. Configure NPC with:
   - **LLM Type**: `lm_studio`
   - **Model**: Model name from LM Studio
   - **URL**: `http://localhost:1234/v1` (default)

## Architecture

The mod uses a hybrid approach for LLM interactions:

- **Tool Calls**: Commands are executed via `execute_command` tool
- **Structured Output**: Messages use structured JSON format
- **Structured Input**: Context data sent as JSON alongside text prompts

For detailed architecture documentation, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Configuration

### Base Config (Global Settings)

Located in `config/craftagent/base.json`:

- `llmTimeout`: LLM API timeout (1-300 seconds, default: 10)
- `contextChunkRadius`: Chunks to scan around NPC (1-7, default: 4)
- `contextVerticalScanRange`: Vertical scan range (4-16, default: 8)
- `chunkExpiryTime`: Block cache refresh interval (20-120 seconds, default: 60)
- `verbose`: Debug mode (true/false)

### NPC Config (Per-NPC Settings)

Located in `config/craftagent/npcs/{uuid}.json`:

- `npcName`: Display name
- `llmType`: `OLLAMA` or `LM_STUDIO`
- `llmModel`: Model name
- `llmCharacter`: Personality traits
- `ollamaUrl`: Ollama server URL
- `lmStudioUrl`: LM Studio URL
- `skinUrl`: Optional custom skin URL

## Development

### Project Structure

```
src/main/java/me/sailex/secondbrain/
├── common/          # NPC management (Service, Factory)
├── event/           # Event handling (NPCEventHandler)
├── llm/             # LLM clients (Ollama, LM Studio)
├── context/         # World context gathering
├── history/         # Conversation history
├── config/          # Configuration management
├── database/        # SQLite persistence
├── networking/      # Client-server communication
└── client/           # Client-side GUI
```

### Key Components

- **NPCService**: Manages NPC lifecycle
- **NPCEventHandler**: Processes events and LLM interactions
- **ContextProvider**: Gathers world state information
- **ConversationHistory**: Manages conversation context
- **LLMClient**: Interface for LLM providers

### Adding New LLM Provider

1. Create new class implementing `LLMClient`
2. Add `LLMType` enum value
3. Update `NPCFactory.initLLMClient()`
4. Add configuration fields in `NPCConfig`
5. Update GUI to show new option

## Troubleshooting

### NPC Not Responding

- Check LLM service is running
- Verify URL and model name in config
- Check server logs for errors
- Ensure NPC has permission to execute commands

### Commands Not Working

- Verify AltoClef is installed and working
- Check NPC has required items/tools
- Review error messages in chat

### LLM Timeout

- Increase `llmTimeout` in base config
- Check LLM service performance
- Try a smaller/faster model

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under LGPL-3.0.

## Links

- [Modrinth](https://modrinth.com/mod/craftagent)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/craftagent)
- [GitHub](https://github.com/prskid1000/CraftAgent)
- [Architecture Documentation](ARCHITECTURE.md)

## Credits

- **Author**: prskid1000
- **Project Inspiration & Code Source**: [SecondBrain](https://github.com/sailex428/SecondBrain/tree/main/src/main) - This project takes inspiration from and includes code from the SecondBrain project
- **AltoClef**: Pathfinding and command execution
- **Fabric**: Mod loader framework
- **Ollama**: Local LLM runtime
- **LM Studio**: Local LLM server
