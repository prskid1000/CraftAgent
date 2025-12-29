package me.prskid1000.craftagent.llm.lmstudio;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sashirestela.openai.domain.chat.ChatMessage;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import me.prskid1000.craftagent.exception.LLMServiceException;
import me.prskid1000.craftagent.history.Message;
import me.prskid1000.craftagent.history.MessageConverter;
import me.prskid1000.craftagent.llm.LLMClient;
import me.prskid1000.craftagent.llm.StructuredOutputSchema;
import me.prskid1000.craftagent.llm.ToolCallResponse;
import me.prskid1000.craftagent.llm.ToolDefinitions;

import java.util.ArrayList;
import java.util.HashMap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class LMStudioClient implements LLMClient {

	private final String model;
	private final String baseUrl;
	private final int timeout;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	/**
	 * Constructor for LMStudioClient.
	 * LM Studio provides an OpenAI-compatible API endpoint.
	 *
	 * @param model the model name to use
	 * @param baseUrl the base URL for LM Studio (typically http://localhost:1234/v1)
	 * @param timeout the timeout in seconds
	 */
	public LMStudioClient(String model, String baseUrl, int timeout) {
		this.model = model;
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		this.timeout = timeout;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(timeout))
				.build();
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public void checkServiceIsReachable() {
		try {
			if (baseUrl == null || baseUrl.isEmpty()) {
				throw new LLMServiceException("LM Studio base URL is not set");
			}
			// Try to make a simple health check request
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl.replace("/v1", "") + "/health"))
					.GET()
					.timeout(Duration.ofSeconds(5))
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			// If we get any response (even 404), the server is reachable
		} catch (Exception e) {
			throw new LLMServiceException("LM Studio server is not reachable at: " + baseUrl, e);
		}
	}

	@Override
	public ToolCallResponse chatWithTools(List<Message> messages) {
		try {
			ChatRequest chatRequest = ChatRequest.builder()
					.model(model)
					.messages(messages.stream()
							.map(MessageConverter::toChatMessage)
							.toList())
					.build();

			// Serialize the request to JSON
			String requestBodyJson = objectMapper.writeValueAsString(chatRequest);
			
			// Parse to Map to add tools and response_format for hybrid approach
			Map<String, Object> requestMap = objectMapper.readValue(requestBodyJson, Map.class);
			// Add tools for command execution (actions)
			requestMap.put("tools", ToolDefinitions.getTools());
			// Add response_format for message output (structured output for simple data)
			requestMap.put("response_format", Map.of("type", "json_object"));
			// Set temperature to 0 for deterministic outputs
			requestMap.put("temperature", 0);
			
			// Re-serialize with tools and response_format
			String requestBody = objectMapper.writeValueAsString(requestMap);

			// Create HTTP request
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl + "/chat/completions"))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(requestBody))
					.timeout(Duration.ofSeconds(timeout))
					.build();

			// Send request and get response
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new LLMServiceException("LM Studio API returned status code: " + response.statusCode() + 
						", response: " + response.body());
			}

			// Parse response
			Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
			List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
			if (choices == null || choices.isEmpty()) {
				throw new LLMServiceException("LM Studio API returned no choices in response");
			}

			Map<String, Object> firstChoice = choices.get(0);
			Map<String, Object> messageMap = (Map<String, Object>) firstChoice.get("message");
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
						String argumentsStr = (String) functionMap.get("arguments");
						Map<String, Object> arguments = new HashMap<>();
						if (argumentsStr != null) {
							try {
								@SuppressWarnings("unchecked")
								Map<String, Object> parsedArgs = objectMapper.readValue(argumentsStr, Map.class);
								arguments = parsedArgs;
							} catch (Exception e) {
								// If parsing fails, try to extract command directly
								if (argumentsStr.contains("\"command\"")) {
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
						toolCalls.add(new ToolCallResponse.ToolCall(id, name, arguments));
					}
				}
			}
			
			return new ToolCallResponse(content != null ? content : "", toolCalls);
		} catch (LLMServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new LLMServiceException("Could not generate Response for prompt: " + messages.get(messages.size() - 1).getMessage(), e);
		}
	}

	@Override
	public void stopService() {
		// There's nothing to stop for LM Studio
	}

}

