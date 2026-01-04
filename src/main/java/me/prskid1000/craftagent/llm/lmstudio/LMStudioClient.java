package me.prskid1000.craftagent.llm.lmstudio;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.prskid1000.craftagent.exception.CraftAgentException;
import me.prskid1000.craftagent.util.LogUtil;
import me.prskid1000.craftagent.history.ConversationMessage;
import me.prskid1000.craftagent.llm.LLMClient;
import me.prskid1000.craftagent.llm.LLMResponse;
import me.prskid1000.craftagent.llm.LLMSchema;

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
	private volatile int timeout;
	private volatile HttpClient httpClient;
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
		this.httpClient = createHttpClient(timeout);
		this.objectMapper = new ObjectMapper();
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

	@Override
	public void checkServiceIsReachable() {
		try {
			if (baseUrl == null || baseUrl.isEmpty()) {
				throw CraftAgentException.llmService("LM Studio base URL is not set");
			}
			
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl + "/models"))
					.GET()
					.timeout(Duration.ofSeconds(timeout))
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			// Accept any 2xx or 3xx status codes, or 401 (unauthorized but server is reachable)
			int statusCode = response.statusCode();
			if (statusCode >= 200 && statusCode < 400) {
				// Success - server is reachable
				return;
			} else if (statusCode == 401) {
				// Unauthorized but server is reachable
				return;
			} else {
				// Unexpected status code
				throw CraftAgentException.llmService("LM Studio server returned status code: " + statusCode + ", response: " + response.body());
			}
		} catch (CraftAgentException e) {
			// Re-throw our own exceptions
			throw e;
		} catch (Exception e) {
			LogUtil.error("LM Studio connection error", e);
			throw CraftAgentException.llmService("LM Studio server is not reachable at: " + baseUrl + ". Error: " + e.getMessage(), e);
		}
	}

	@Override
	public LLMResponse chat(List<ConversationMessage> messages, net.minecraft.server.MinecraftServer server) {
		try {
			// Convert messages to OpenAI-compatible format (same as Ollama format)
			List<Map<String, String>> openaiMessages = new ArrayList<>();
			for (ConversationMessage msg : messages) {
				Map<String, String> openaiMsg = new HashMap<>();
				openaiMsg.put("role", msg.getRole());
				openaiMsg.put("content", msg.getMessage());
				openaiMessages.add(openaiMsg);
			}
			
			// Build request body
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("model", model);
			requestBody.put("messages", openaiMessages);
			requestBody.put("stream", false);
			// Add response_format for structured output
			// Use json_schema type (not json_object) - LM Studio only accepts "json_schema" or "text"
			Map<String, Object> responseFormat = new HashMap<>();
			responseFormat.put("type", "json_schema");
			Map<String, Object> jsonSchema = new HashMap<>();
			jsonSchema.put("name", "message_schema");
			jsonSchema.put("schema", LLMSchema.getMessageSchema());
			jsonSchema.put("strict", true);
			responseFormat.put("json_schema", jsonSchema);
			requestBody.put("response_format", responseFormat);
			
			String requestBodyJson = objectMapper.writeValueAsString(requestBody);

			LogUtil.debugInChat("LLM Request sent to LM Studio");

			// Create HTTP request
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl + "/chat/completions"))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
					.timeout(Duration.ofSeconds(timeout))
					.build();

			// Send request and get response
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			LogUtil.debugInChat("LLM Response received from LM Studio");

			if (response.statusCode() != 200) {
				throw CraftAgentException.llmService("LM Studio API returned status code: " + response.statusCode() + 
						", response: " + response.body());
			}

			// Parse response
			Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
			List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
			if (choices == null || choices.isEmpty()) {
				throw CraftAgentException.llmService("LM Studio API returned no choices in response");
			}

			Map<String, Object> firstChoice = choices.get(0);
			Map<String, Object> messageMap = (Map<String, Object>) firstChoice.get("message");
			String content = (String) messageMap.get("content");
			
			return new LLMResponse(content != null ? content : "");
		} catch (CraftAgentException e) {
			throw e;
		} catch (Exception e) {
			throw CraftAgentException.llmService("Could not generate Response for prompt: " + messages.get(messages.size() - 1).getMessage(), e);
		}
	}

	@Override
	public void stopService() {
		// There's nothing to stop for LM Studio
	}

}

