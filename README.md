# CraftAgent

A Minecraft Fabric mod that brings intelligent AI-powered NPCs to your world. NPCs use Large Language Models (LLMs) to understand context, make decisions, and interact with players.

## Features

- 🤖 **AI-Powered NPCs**: Uses LLMs (Ollama, LM Studio) for intelligent decision-making
- 🎮 **Autonomous Actions**: NPCs execute Minecraft commands to interact with the world
- 💬 **Conversation Memory**: Persistent conversation history in SQLite
- 🌍 **World Context Awareness**: NPCs understand surroundings (blocks, entities, inventory)
- 🔧 **Multi-LLM Support**: Ollama and LM Studio (OpenAI-compatible)
- 🎨 **Client-Server Architecture**: GUI configuration on client, NPC logic on server
- 🛠️ **Command System**: Uses Brigadier to discover and execute all Minecraft commands
- 🌐 **Web Dashboard**: Built-in web UI (http://localhost:8080) to monitor NPCs, view context, messages, and memory

## Requirements

- **Minecraft**: 1.20.1 or 1.21.8
- **Java**: 17 (1.20.1) or 21 (1.21.8+)
- **Fabric Loader**: 0.17.3+
- **LLM Service**: Ollama or LM Studio running locally

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
   git clone https://github.com/prskid1000/CraftAgent.git
   cd CraftAgent
   ```

2. Build the project:
   ```bash
   # Windows
   .\gradlew.bat :1.21.8:build
   
   # Linux/Mac
   ./gradlew :1.21.8:build
   ```

3. Find the compiled JAR in `versions/1.21.8/build/libs/`

## Building

```bash
# Build for specific version
.\gradlew.bat :1.21.8:build

# Build all versions
.\gradlew.bat build

# Run in development
.\gradlew.bat :1.21.8:runServer
```

## Usage

### Commands

- `/craftagent` - Open configuration GUI
- `/craftagent create <name> <llmType>` - Create new NPC
- `/craftagent remove <name>` - Remove NPC (keeps data)
- `/craftagent delete <name>` - Permanently delete NPC

### Web Dashboard

CraftAgent includes a built-in web dashboard for monitoring NPCs in real-time. The dashboard automatically starts when the Minecraft server starts.

**Access the Dashboard:**
1. Start your Minecraft server with CraftAgent installed
2. Open your web browser and navigate to: **http://localhost:8080**
3. The dashboard will display all active NPCs with their current status

**Dashboard Features:**
- **NPC Overview**: View all NPCs with their basic information (name, age, gender, health, food, position)
- **State Tab**: Real-time statistics with progress bars for health and food, position coordinates, and biome information
- **Context Tab**: Detailed world context including:
  - State information (position, health, food, biome)
  - Inventory details (hotbar, main inventory, armor, off-hand)
  - Nearby blocks table (type, position, mine level, tool needed)
  - Nearby entities table (name, type, ID, player status)
- **Messages Tab**: Conversation history with role badges and timestamps
- **Mail Tab**: Mail messages from other NPCs and players
- **Memory Tab**: NPC's persistent memory including:
  - Remembered locations with coordinates
  - Contacts with relationship levels
  - Shared book pages

**Note:** The dashboard auto-refreshes every 5 minutes. Use the refresh button in the NPC detail modal to manually update data.

### Creating an NPC

1. Start your LLM service (Ollama or LM Studio)
2. Use `/craftagent create <name> <llmType>` or open GUI with `/craftagent`
3. Configure the NPC:
   - **Name**: Display name
   - **LLM Type**: `ollama` or `lm_studio`
   - **Model**: Model name (e.g., `llama3`)
   - **URL**: LLM service URL (defaults: `http://localhost:11434` for Ollama, `http://localhost:1234/v1` for LM Studio)

## LLM Setup

### Ollama

1. Install from [ollama.ai](https://ollama.ai)
2. Start: `ollama serve`
3. Pull a model: `ollama pull llama3`
4. Configure NPC: LLM Type `ollama`, Model `llama3`, URL `http://localhost:11434`

### LM Studio

1. Install from [lmstudio.ai](https://lmstudio.ai)
2. Download a model and start local server (port 1234)
3. Configure NPC: LLM Type `lm_studio`, Model name, URL `http://localhost:1234/v1`

## Architecture

- **Command System**: Uses Brigadier to discover and execute all Minecraft commands (~115+ vanilla commands, plus mod commands)
- **Tool Calls**: Commands executed via `execute_command` tool
- **Structured I/O**: JSON format for reliable parsing of context and responses

## Configuration

Config files are in `config/craftagent/`:

- **base.json**: Global settings (timeout, chunk radius, debug mode)
- **npcs/{uuid}.json**: Per-NPC settings (name, LLM type, model, personality)

## Development

### Key Components

- **NPCService**: Manages NPC lifecycle
- **NPCEventHandler**: Processes events and LLM interactions
- **ContextProvider**: Gathers world state information
- **MinecraftCommandUtil**: Discovers and executes Minecraft commands via Brigadier
- **LLMClient**: Interface for LLM providers (Ollama, LM Studio)

## Troubleshooting

- **NPC not responding**: Check LLM service is running, verify URL/model in config
- **Commands not working**: Check server logs, ensure NPC has required permissions
- **LLM timeout**: Increase `llmTimeout` in base config or try a faster model

## Contributing

Contributions welcome! Fork, create a feature branch, and submit a pull request.

## License

This project is licensed under LGPL-3.0.

## Links

- [Modrinth](https://modrinth.com/mod/craftagent)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/craftagent)
- [GitHub](https://github.com/prskid1000/CraftAgent)

## Credits

- **Author**: prskid1000
- **Fabric**: Mod loader framework
- **Brigadier**: Command system (Mojang)
- **Ollama** & **LM Studio**: LLM providers
