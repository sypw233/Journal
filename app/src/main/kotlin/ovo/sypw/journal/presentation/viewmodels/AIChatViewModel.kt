package ovo.sypw.journal.presentation.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import ovo.sypw.journal.common.APIKey
import ovo.sypw.journal.common.utils.ImageBase64Utils
import ovo.sypw.journal.di.AppDependencyManager
import ovo.sypw.journal.presentation.screens.ChatMessage
import java.io.IOException
import javax.inject.Inject

/**
 * AI聊天界面的UI状态
 */
data class AIChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedModel: String = "qvq-max",
    val availableModels: List<String> = listOf(
        "qvq-max",
        "qwen-vl-plus",
        "qwen-max",
        "qwen-turbo",
        "deepseek-r1",
        "deepseek-v3",
        "iam-sb"
    ),
    val thinking: String? = null,
    val contextEnabled: Boolean = true,  // 是否启用上下文功能
    val maxContextMessages: Int = 30     // 最大上下文消息数量
)

/**
 * AI聊天ViewModel，负责管理聊天状态和处理API请求
 */
@HiltViewModel
class AIChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dependencyManager: AppDependencyManager
) : ViewModel() {
    companion object {
        // 模型思考过程的键名
        private const val THINKING_KEY = "reasoning_content"

        // 默认系统提示语
        private const val DEFAULT_SYSTEM_PROMPT = "你是一个智能助理"

    }

    private val TAG = "AIChatViewModel"

    // UI状态
    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    // OkHttp客户端
    private val client = OkHttpClient.Builder().build()

    /**
     * 发送消息到AI服务
     */
    fun sendMessage(text: String, images: List<Uri>) {
        viewModelScope.launch {
            try {
                // 设置加载状态
                _uiState.update { it.copy(isLoading = true) }

                // 添加用户消息到聊天历史
                val userMessage = ChatMessage(
                    content = text,
                    images = images,
                    isUser = true
                )
                _uiState.update { it.copy(messages = it.messages + userMessage) }

                // 准备API请求
                if (images.isNotEmpty()) {
                    // 处理图片，实际实现中需要将Uri转换为Base64
                    sendMessageWithImages(text, images)
                } else {
                    // 纯文本消息
                    sendTextMessage(text)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                _uiState.update { it.copy(isLoading = false, error = "发送消息失败: ${e.message}") }
            }
        }
    }

    /**
     * 获取对话历史消息
     * @return 历史消息的JSONArray
     */
    private fun getHistoryMessages(): JSONArray {
        val messagesArray = JSONArray()

        // 添加系统消息
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        val systemContent = JSONArray()
        val systemTextContent = JSONObject()
        systemTextContent.put("type", "text")
        systemTextContent.put("text", DEFAULT_SYSTEM_PROMPT)
        systemContent.put(systemTextContent)
        systemMessage.put("content", systemContent)
        messagesArray.put(systemMessage)

        // 如果启用了上下文
        if (_uiState.value.contextEnabled) {
            // 获取历史消息（排除最后一条用户消息，因为它会在调用方法中单独添加）
            val history = _uiState.value.messages.dropLast(1)

            // 限制消息数量，只取最近的N条消息
            val maxMessages = _uiState.value.maxContextMessages
            val contextMessages = if (history.size > maxMessages) {
                history.takeLast(maxMessages)
            } else {
                history
            }

            // 将历史消息添加到请求中
            for (message in contextMessages) {
                val role = if (message.isUser) "user" else "assistant"
                val messageObj = JSONObject()
                messageObj.put("role", role)

                // 处理消息内容
                if (message.images.isNotEmpty() && message.isUser) {
                    // 图片消息需要特殊处理
                    val content = JSONArray()

                    // 添加图片
                    for (imageUri in message.images) {
                        val imageBase64 = ImageBase64Utils.uriToBase64(context, imageUri)
                        if (imageBase64 != null) {
                            val imageContent = JSONObject()
                            imageContent.put("type", "image_url")
                            val imageUrlObj = JSONObject()
                            imageUrlObj.put("url", imageBase64)
                            imageContent.put("image_url", imageUrlObj)
                            content.put(imageContent)
                        }
                    }

                    // 添加文本
                    if (message.content.isNotBlank()) {
                        val textContent = JSONObject()
                        textContent.put("type", "text")
                        textContent.put("text", message.content)
                        content.put(textContent)
                    }

                    messageObj.put("content", content)
                } else {
                    // 纯文本消息
                    val content = JSONArray()
                    val textContent = JSONObject()
                    textContent.put("type", "text")
                    textContent.put("text", message.content)
                    content.put(textContent)
                    messageObj.put("content", content)
                }

                messagesArray.put(messageObj)
            }
        }

        return messagesArray
    }

    /**
     * 发送带图片的消息
     */
    private fun sendMessageWithImages(text: String, images: List<Uri>) {
        // 获取历史消息，包括系统提示
        val messagesArray = getHistoryMessages()

        // 添加当前用户消息
        val userMessage = JSONObject()
        userMessage.put("role", "user")
        val userContent = JSONArray()

        // 添加图片（将Uri转换为Base64）
        for (imageUri in images) {
            val imageContent = JSONObject()
            imageContent.put("type", "image_url")
            val imageUrlObj = JSONObject()

            // 将Uri转换为Base64格式
            val base64Image = ImageBase64Utils.uriToBase64(context, imageUri)

            if (base64Image != null) {
                // 使用转换后的Base64字符串
                imageUrlObj.put("url", base64Image)
                imageContent.put("image_url", imageUrlObj)
                userContent.put(imageContent)
            } else {
                Log.e(TAG, "Failed to convert image to Base64: $imageUri")
                _uiState.update { it.copy(error = "图片处理失败，请重试") }
            }
        }

        // 添加文本
        if (text.isNotBlank()) {
            val textContent = JSONObject()
            textContent.put("type", "text")
            textContent.put("text", text)
            userContent.put(textContent)
        }

        userMessage.put("content", userContent)
        messagesArray.put(userMessage)

        // 构建请求体
        val requestBody = JSONObject()
        requestBody.put("model", _uiState.value.selectedModel)
        requestBody.put("messages", messagesArray)
        requestBody.put("stream", true)

        val streamOptions = JSONObject()
        streamOptions.put("include_usage", true)
        requestBody.put("stream_options", streamOptions)

        // 添加思考过程参数
        val parameters = JSONObject()
        parameters.put(THINKING_KEY, true)
        parameters.put("vl_high_resolution_images", true)
        requestBody.put("parameters", parameters)

        // 发送请求
        sendRequest(requestBody.toString())
    }

    /**
     * 发送纯文本消息
     */
    private fun sendTextMessage(text: String) {
        // 获取历史消息，包括系统提示
        val messagesArray = getHistoryMessages()

        // 添加当前用户消息
        val userMessage = JSONObject()
        userMessage.put("role", "user")
        val userContent = JSONArray()

        // 添加文本
        val textContent = JSONObject()
        textContent.put("type", "text")
        textContent.put("text", text)
        userContent.put(textContent)

        userMessage.put("content", userContent)
        messagesArray.put(userMessage)

        // 构建请求体
        val requestBody = JSONObject()
        requestBody.put("model", _uiState.value.selectedModel)
        requestBody.put("messages", messagesArray)
        requestBody.put("stream", true)

        val streamOptions = JSONObject()
        streamOptions.put("include_usage", true)
        requestBody.put("stream_options", streamOptions)

        // 添加思考过程参数
        val parameters = JSONObject()
        parameters.put(THINKING_KEY, true)
        requestBody.put("parameters", parameters)

        // 发送请求
        sendRequest(requestBody.toString())
    }

    /**
     * 发送API请求
     */
    private fun sendRequest(requestBodyJson: String) {
//        Log.d(TAG, "sendRequest: $requestBodyJson")
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestBodyJson.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${APIKey.DASHSCOPE_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        // 创建一个StringBuilder来收集流式响应
        val contextBuilder = StringBuilder()
        val thinkingBuilder = StringBuilder()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "API request failed", e)
                _uiState.update { it.copy(isLoading = false, error = "API请求失败: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e(TAG, "API request unsuccessful: ${response.code}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "API请求不成功: ${response.code}"
                        )
                    }
                    return
                }

                try {
                    // 处理流式响应
                    response.body?.source()?.let { source ->
                        // 初始化AI回复消息
                        val aiMessage = ChatMessage(
                            content = "\n",
                            isUser = false,
                            thinking = null
                        )
                        _uiState.update {
                            it.copy(
                                messages = it.messages + aiMessage,
                                thinking = null
                            )
                        }

                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: continue
                            if (line.isEmpty()) continue
//                            Log.d(TAG, "onResponse: $line")
                            if (line.startsWith("data: ")) {
                                val jsonData = line.substring(6)
                                if (jsonData == "[DONE]") {
                                    // 流结束
                                    Log.d(TAG, "Thinking: $thinkingBuilder")
                                    Log.d(TAG, "Context: $contextBuilder")
                                    break
                                }

                                try {
                                    val jsonObject = JSONObject(jsonData)
                                    if (jsonObject.has("choices")) {
                                        val choices = jsonObject.getJSONArray("choices")
                                        if (choices.length() > 0) {
                                            val choice = choices.getJSONObject(0)
                                            // 处理内容
                                            if (choice.has("delta")) {
                                                val delta = choice.getJSONObject("delta")
                                                if (delta.has(THINKING_KEY) && delta.getString(
                                                        THINKING_KEY
                                                    ) != "null"
                                                ) {
                                                    val content = delta.getString(THINKING_KEY)
                                                    thinkingBuilder.append(content)
                                                    // 更新UI状态中的最后一条消息
                                                    _uiState.update { currentState ->
                                                        val updatedMessages =
                                                            currentState.messages.toMutableList()
                                                        val lastIndex = updatedMessages.size - 1
                                                        if (lastIndex >= 0) {
                                                            val lastMessage =
                                                                updatedMessages[lastIndex]
                                                            updatedMessages[lastIndex] =
                                                                lastMessage.copy(
                                                                    thinking = thinkingBuilder.toString()
                                                                )
                                                        }
                                                        currentState.copy(messages = updatedMessages)
                                                    }
                                                } else if (delta.has("content")) {
                                                    val content = delta.getString("content")
                                                    contextBuilder.append(content)

                                                    // 更新UI状态中的最后一条消息
                                                    _uiState.update { currentState ->
                                                        val updatedMessages =
                                                            currentState.messages.toMutableList()
                                                        val lastIndex = updatedMessages.size - 1
                                                        if (lastIndex >= 0) {
                                                            val lastMessage =
                                                                updatedMessages[lastIndex]
                                                            updatedMessages[lastIndex] =
                                                                lastMessage.copy(
                                                                    content = contextBuilder.toString()
                                                                )
                                                        }
                                                        currentState.copy(messages = updatedMessages)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing JSON", e)
                                }
                            }
                        }

                        // 完成加载
                        _uiState.update { it.copy(isLoading = false) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing response", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "处理响应失败: ${e.message}"
                        )
                    }
                } finally {
                    response.close()
                }
            }
        })
    }

    /**
     * 重置聊天历史
     */
    fun resetChat() {
        _uiState.update { it.copy(messages = emptyList(), error = null) }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 更新选择的模型
     */
    fun updateSelectedModel(model: String) {
        if (_uiState.value.availableModels.contains(model)) {
            _uiState.update { it.copy(selectedModel = model) }
        }
    }

    /**
     * 切换上下文功能
     * @param enabled 是否启用上下文
     */
    fun toggleContext(enabled: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(contextEnabled = enabled)
        }
    }

    /**
     * 设置最大上下文消息数量
     * @param count 最大消息数量
     */
    fun setMaxContextMessages(count: Int) {
        if (count > 0) {
            _uiState.update { it.copy(maxContextMessages = count) }
        }
    }
}