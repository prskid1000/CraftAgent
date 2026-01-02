package me.prskid1000.craftagent.llm.ollama;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.prskid1000.craftagent.exception.LLMServiceException;
import me.prskid1000.craftagent.util.LogUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.prskid1000.craftagent.history.Message;
import me.prskid1000.craftagent.llm.LLMClient;
import me.prskid1000.craftagent.llm.StructuredOutputSchema;
import me.prskid1000.craftagent.llm.ToolCallResponse;
import me.prskid1000.craftagent.llm.ToolDefinitions;

public class OllamaClient implements LLMClient {

	private final String model;
	private final String url;
	private volatile int timeout;
	private volatile HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public OllamaClient(
        String model,
		String url,
		int timeout,
		boolean verbose
	) {
		this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
		this.model = model;
		this.timeout = timeout;
		
		// Configure ObjectMapper to ignore unknown properties (like "thinking" field)
		this.objectMapper = new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		this.httpClient = createHttpClient(timeout);
	}
	
	/**
	 * Updates the timeout value and recreates the HTTP client in real-time.
	 * This allows configuration changes to take effect without restarting.
	 */
	public void updateTimeout(int newTimeout) {
		this.timeout = newTimeout;
		this.httpClient = createHttpClient(newTimeout);
	}
	
	private HttpClient createHttpClient(int timeoutSeconds) {
		return HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.connectTimeout(Duration.ofSeconds(timeoutSeconds))
				.build();
	}

	/**
	 * Check if the service is reachable.
	 * @throws LLMServiceException if server is not reachable
	 */
	@Override
	public void checkServiceIsReachable() {
		try {
			// Simple health check using the /api/tags endpoint
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url + "/api/tags"))
					.GET()
					.timeout(Duration.ofSeconds(timeout))
					.build();
			
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			if (response.statusCode() != 200) {
				throw new LLMServiceException("Ollama server is not reachable at: " + url);
			}
		} catch (Exception e) {
			throw new LLMServiceException("Ollama server is not reachable at: " + url, e);
		}
	}

//    private void initModels(String defaultPrompt) {
//        pullRequiredModels();
//        createModelWithPrompt(defaultPrompt);
//    }
//
//    private void pullRequiredModels() {
//        try {
//            Set<String> modelNames = ollamaAPI.listModels().stream()
//                    .map(Model::getModelName).collect(Collectors.toSet());
//            boolean requiredModelsExist = modelNames.containsAll(REQUIRED_MODELS);
//            if (!requiredModelsExist) {
//                for (String requiredModel : REQUIRED_MODELS) {
//                    LogUtil.debugInChat("Pulling model: " + requiredModel);
//                    ollamaAPI.pullModel(requiredModel);
//                }
//            }
//        } catch (Exception e) {
//            throw new LLMServiceException("Could not required models: " + REQUIRED_MODELS,  e);
//        }
//    }
//
//    private void createModelWithPrompt(String defaultPrompt) {
//        try {
//            LogUtil.debugInChat("Init model: " + model);
//            ollamaAPI.createModel(CustomModelRequest.builder()
//                    .from(LLAMA_MODEL_NAME)
//                    .model(model)
//                    .system(defaultPrompt)
//                    .license("MIT")
//                    .build());
//        } catch (Exception e) {
//            throw new LLMServiceException("Could not create model: " + model, e);
//        }
//    }
//
//    /**
//     * Removes current model.
//     */
//    public void removeModel() {
//        try {
//            LogUtil.debugInChat("Removing model: " + model);
//            ollamaAPI.deleteModel(model, true);
//        } catch (Exception e) {
//            Thread.currentThread().interrupt();
//            throw new LLMServiceException("Could not remove model: " + model, e);
//        }
//    }

	@Override
	public ToolCallResponse chatWithTools(List<Message> messages, net.minecraft.server.MinecraftServer server) {
		try {
			// Convert messages to Ollama format
			List<Map<String, String>> ollamaMessages = new ArrayList<>();
			for (Message msg : messages) {
				Map<String, String> ollamaMsg = new HashMap<>();
				ollamaMsg.put("role", msg.getRole());
				ollamaMsg.put("content", msg.getMessage());
				ollamaMessages.add(ollamaMsg);
			}
			
			// Build request body with tool calling (hybrid approach)
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("model", model);
			requestBody.put("messages", ollamaMessages);
			requestBody.put("stream", false);
			// Use tools for command execution (actions) - with command information from server
			requestBody.put("tools", ToolDefinitions.getTools(server));
			// Use format for message output (structured output for simple data)
			requestBody.put("format", ToolDefinitions.getMessageSchema());
			// Set temperature to 0 for deterministic outputs
			requestBody.put("temperature", 0);
			
			String requestBodyJson = objectMapper.writeValueAsString(requestBody);
			
			// Log request
			LogUtil.info("LLM Request (Ollama): " + requestBodyJson);
			LogUtil.debugInChat("LLM Request sent to Ollama");
			
			// Create HTTP request
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url + "/api/chat"))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
					.timeout(Duration.ofSeconds(timeout))
					.build();
			
			// Send request and get response
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			// Log response
			LogUtil.info("LLM Response (Ollama): " + response.body());
			LogUtil.debugInChat("LLM Response received from Ollama");
			
			if (response.statusCode() != 200) {
				throw new LLMServiceException("Ollama API returned status code: " + response.statusCode() + 
						", response: " + response.body());
			}
			
			// Parse response
			Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
			Map<String, Object> messageMap = (Map<String, Object>) responseMap.get("message");
			
			if (messageMap == null) {
				throw new LLMServiceException("Ollama API response missing 'message' field");
			}
			
			String content = (String) messageMap.get("content");
			
			// Parse tool calls if present
			List<ToolCallResponse.ToolCall> toolCalls = new ArrayList<>();
			Object toolCallsObj = messageMap.get("tool_calls");
			if (toolCallsObj != null && toolCallsObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> toolCallsList = (List<Map<String, Object>>) toolCallsObj;
				for (Map<String, Object> toolCallMap : toolCallsList) {
					String id = (String) toolCallMap.get("id");
					Map<String, Object> functionMap = (Map<String, Object>) toolCallMap.get("function");
					if (functionMap != null) {
						String name = (String) functionMap.get("name");
						Object argumentsObj = functionMap.get("arguments");
						Map<String, Object> arguments = new HashMap<>();
						if (argumentsObj != null) {
							// Handle both cases: arguments can be a Map (already parsed) or a String (JSON to parse)
							if (argumentsObj instanceof Map) {
								// Already a Map, use it directly
								@SuppressWarnings("unchecked")
								Map<String, Object> parsedArgs = (Map<String, Object>) argumentsObj;
								arguments = parsedArgs;
							} else if (argumentsObj instanceof String) {
								// It's a JSON string, parse it
								String argumentsStr = (String) argumentsObj;
								try {
									@SuppressWarnings("unchecked")
								Map<String, Object> parsedArgs = objectMapper.readValue(argumentsStr, Map.class);
								arguments = parsedArgs;
							} catch (Exception e) {
								// If parsing fails, try to extract command directly
								if (argumentsStr.contains("\"command\"")) {
									// Simple extraction
									int start = argumentsStr.indexOf("\"command\"");
									if (start >= 0) {
										start = argumentsStr.indexOf("\"", start + 9) + 1;
										int end = argumentsStr.indexOf("\"", start);
										if (end > start) {
											String cmd = argumentsStr.substring(start, end);
											arguments.put("command", cmd);
											}
										}
									}
								}
							}
						}
						toolCalls.add(new ToolCallResponse.ToolCall(id, name, arguments));
					}
				}
			}
			
			return new ToolCallResponse(content != null ? content : "", toolCalls);
		} catch (LLMServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new LLMServiceException("Could not generate Response for last prompt: " + messages.get(messages.size() - 1).getMessage(), e);
		}
	}

	@Override
	public void stopService() {
		// Nothing to stop
	}
}
