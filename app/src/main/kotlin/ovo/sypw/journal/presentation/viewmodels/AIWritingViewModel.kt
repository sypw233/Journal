package ovo.sypw.journal.presentation.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import ovo.sypw.journal.common.utils.SnackBarUtils
import ovo.sypw.journal.data.model.AIModels
import ovo.sypw.journal.data.model.JournalData
import ovo.sypw.journal.data.repository.JournalRepository
import ovo.sypw.journal.di.AppDependencyManager
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

/**
 * AI写作界面的UI状态
 */
data class AIWritingUiState(
    val isThinking: Boolean = false,
    val isLoading: Boolean = false,
    val generatedContent: String = "",
    val error: String? = null,
    val selectedModel: String = "qwen-turbo", // 使用更轻量的模型来生成日记
    val useHistoricalJournals: Boolean = true, // 是否使用历史日记作为参考
    val historicalJournalsCount: Int = 5, // 使用的历史日记数量
    val thinking: String? = null, // 思考过程
    val images: List<Uri> = emptyList(), // 用于生成的图片
    val useImages: Boolean = false, // 是否使用图片进行创作
    val canCancel: Boolean = false // 是否可以取消生成过程
)

/**
 * AI写作ViewModel，负责管理AI内容生成
 */
class AIWritingViewModel(
    private val dependencyManager: AppDependencyManager
) : ViewModel() {

    companion object {
        private const val TAG = "AIWritingViewModel"

        // 模型思考过程的键名
        private const val THINKING_KEY = "reasoning_content"

        // 默认系统提示语
        private const val DEFAULT_SYSTEM_PROMPT =
            "你是一位专业的日记生成助手，专注于帮助用户生成个人日记内容。请遵循以下规则：\n" +
                    "1. 根据用户提供的提示生成自然、流畅的日记内容\n" +
                    "2. 使用第一人称视角，口语化、自然的表达\n" +
                    "3. 字数控制在100-300字之间\n" +
                    "4. 不要包含标题或日期\n" +
                    "5. 风格要生活化、真实，有适当的情感表达\n" +
                    "6. 不要加入任何提示、建议或总结性语句\n" +
                    "7. 如果用户要求生成Markdown格式，请使用适当的Markdown语法，但不要过于复杂\n" +
                    "8. 直接输出日记内容，不要有任何多余的解释或前导语"

        // 带历史日记的系统提示语
        private const val SYSTEM_PROMPT_WITH_HISTORY =
            "你是一位专业的日记生成助手，专注于帮助用户生成个人日记内容。我将提供用户的历史日记作为参考，请遵循以下规则：\n" +
                    "1. 参考用户的历史日记，学习用户的写作风格、语气和关注点\n" +
                    "2. 根据用户提供的提示以及历史日记风格，生成新的日记内容\n" +
                    "3. 使用与历史日记一致的人称视角，保持一致的语气和表达方式\n" +
                    "4. 字数控制在100-300字之间\n" +
                    "5. 不要包含标题或日期\n" +
                    "6. 风格要与用户历史日记保持一致，体现个人特色\n" +
                    "7. 不要加入任何提示、建议或总结性语句\n" +
                    "8. 如果用户要求使用Markdown格式，请适当使用Markdown语法\n" +
                    "9. 直接输出日记内容，不要有任何多余的解释或前导语\n" +
                    "10. 专注于模仿用户的写作风格，而不是简单地复制历史内容"

        // 带图片的系统提示语
        private const val SYSTEM_PROMPT_WITH_IMAGE =
            "你是一位专业的日记生成助手，专注于帮助用户根据图片生成个人日记内容。请遵循以下规则：\n" +
                    "1. 仔细观察并描述用户提供的图片内容\n" +
                    "2. 根据图片内容和用户提示（如果有）生成自然、流畅的日记内容\n" +
                    "3. 使用第一人称视角，口语化、自然的表达，就像用户亲身经历了图片中的场景\n" +
                    "4. 字数控制在100-300字之间\n" +
                    "5. 不要包含标题或日期\n" +
                    "6. 风格要生活化、真实，有适当的情感表达\n" +
                    "7. 不要直接说明你在描述图片，而是将图片内容自然融入日记中\n" +
                    "8. 如果用户要求使用Markdown格式，请适当使用Markdown语法\n" +
                    "9. 直接输出日记内容，不要有任何多余的解释或前导语\n" +
                    "10. 如果用户没有提供文字提示，请完全基于图片内容创作日记"

        // 带图片和历史日记的系统提示语
        private const val SYSTEM_PROMPT_WITH_IMAGE_AND_HISTORY =
            "你是一位专业的日记生成助手，专注于帮助用户根据图片和历史日记风格生成个人日记内容。请遵循以下规则：\n" +
                    "1. 仔细观察并描述用户提供的图片内容\n" +
                    "2. 参考用户的历史日记，学习用户的写作风格、语气和关注点\n" +
                    "3. 根据图片内容、用户提示（如果有）以及历史日记风格，生成新的日记内容\n" +
                    "4. 使用与历史日记一致的人称视角，保持一致的语气和表达方式\n" +
                    "5. 字数控制在100-300字之间\n" +
                    "6. 不要包含标题或日期\n" +
                    "7. 风格要与用户历史日记保持一致，体现个人特色\n" +
                    "8. 不要直接说明你在描述图片，而是将图片内容自然融入日记中\n" +
                    "9. 如果用户要求使用Markdown格式，请适当使用Markdown语法\n" +
                    "10. 直接输出日记内容，不要有任何多余的解释或前导语\n" +
                    "11. 如果用户没有提供文字提示，请完全基于图片内容和历史日记风格创作日记"
    }

    // 从设置获取默认配置
    private val preferences = dependencyManager.preferences
    private val aiSettings = preferences.getAISettings()

    // UI状态
    private val _uiState = MutableStateFlow(
        AIWritingUiState(
            selectedModel = aiSettings.modelType,
            useHistoricalJournals = aiSettings.useHistoricalJournalsDefault,
            historicalJournalsCount = aiSettings.historicalJournalsCountDefault
        )
    )
    val uiState: StateFlow<AIWritingUiState> = _uiState.asStateFlow()

    // OkHttp客户端
    private val client = OkHttpClient.Builder().build()

    // 当前API请求的Call引用，用于取消请求
    private var currentCall: Call? = null

    /**
     * 取消当前正在进行的内容生成
     */
    fun cancelGeneration() {
        currentCall?.let { call ->
            if (!call.isCanceled()) {
                call.cancel()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        canCancel = false,
                        error = "内容生成已取消"
                    )
                }
                SnackBarUtils.showSnackBar("已取消内容生成")
            }
        }
        currentCall = null
    }

    /**
     * 生成日记内容
     */
    fun generateContent(prompt: String, useMarkdown: Boolean = false) {
        viewModelScope.launch {
            try {
                // 取消之前的请求（如果有）
                cancelGeneration()

                // 设置加载状态
                _uiState.update { it.copy(isLoading = true, error = null, canCancel = true) }

                // 获取历史日记
                val historicalJournals = if (_uiState.value.useHistoricalJournals) {
                    getHistoricalJournals(_uiState.value.historicalJournalsCount)
                } else {
                    emptyList()
                }

                // 构建请求体
                val requestBody =
                    if (_uiState.value.useImages && _uiState.value.images.isNotEmpty()) {
                        buildRequestBodyWithImages(prompt, useMarkdown, historicalJournals)
                    } else {
                        buildRequestBody(prompt, useMarkdown, historicalJournals)
                    }

                // 发送API请求
                sendRequest(requestBody)
            } catch (e: Exception) {
                Log.e(TAG, "生成内容失败", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        canCancel = false,
                        error = "生成内容失败: ${e.message}"
                    )
                }
                SnackBarUtils.showSnackBar("生成内容失败，请重试")
            }
        }
    }

    /**
     * 获取历史日记数据
     */
    private suspend fun getHistoricalJournals(count: Int): List<JournalData> =
        withContext(Dispatchers.IO) {
            try {
                val repository = dependencyManager.getJournalRepository()
                val journals = repository.getAllJournals().first()

                // 过滤有文本内容的日记
                journals.filter { !it.text.isNullOrBlank() }
                    .take(count)  // 限制数量
            } catch (e: Exception) {
                Log.e(TAG, "获取历史日记失败", e)
                emptyList()
            }
        }

    /**
     * 格式化历史日记为文本
     */
    private fun formatHistoricalJournals(journals: List<JournalData>): String {
        if (journals.isEmpty()) return ""

        val dateFormatter = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE)
        val builder = StringBuilder("以下是用户的历史日记样本，请学习风格：\n\n")

        journals.forEachIndexed { index, journal ->
            builder.append("【历史日记${index + 1}】\n")
            builder.append("日期：${dateFormatter.format(journal.date ?: Date())}\n")
            journal.location?.name?.let { builder.append("位置：$it\n") }
            builder.append("内容：${journal.text}\n")
            builder.append("是否使用Markdown：${if (journal.isMarkdown) "是" else "否"}\n\n")
        }

        return builder.toString()
    }

    /**
     * 构建API请求体
     */
    private fun buildRequestBody(
        prompt: String,
        useMarkdown: Boolean,
        historicalJournals: List<JournalData>
    ): String {
        val messagesArray = JSONArray()

        // 添加系统消息
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")

        // 根据是否需要Markdown格式和历史日记调整提示语
        val historicalJournalsText = formatHistoricalJournals(historicalJournals)
        val systemPrompt = if (historicalJournals.isNotEmpty()) {
            val markdownPrompt = if (useMarkdown) {
                "\n请使用Markdown格式生成内容，合理使用标记语法增强可读性。"
            } else {
                ""
            }
            SYSTEM_PROMPT_WITH_HISTORY + markdownPrompt + "\n\n" + historicalJournalsText
        } else {
            if (useMarkdown) {
                "$DEFAULT_SYSTEM_PROMPT\n请使用Markdown格式生成内容，合理使用标记语法增强可读性。"
            } else {
                DEFAULT_SYSTEM_PROMPT
            }
        }

        val systemContent = JSONArray()
        val systemTextContent = JSONObject()
        systemTextContent.put("type", "text")
        systemTextContent.put("text", systemPrompt)
        systemContent.put(systemTextContent)
        systemMessage.put("content", systemContent)
        messagesArray.put(systemMessage)

        // 添加用户消息
        val userMessage = JSONObject()
        userMessage.put("role", "user")
        val userContent = JSONArray()
        val textContent = JSONObject()
        textContent.put("type", "text")
        textContent.put("text", prompt)
        userContent.put(textContent)
        userMessage.put("content", userContent)
        messagesArray.put(userMessage)

        // 构建完整请求体
        val requestBody = JSONObject()
        requestBody.put("model", _uiState.value.selectedModel)
        requestBody.put("messages", messagesArray)
        requestBody.put("stream", true)

        val streamOptions = JSONObject()
        streamOptions.put("include_usage", true)
        requestBody.put("stream_options", streamOptions)

        // 添加最大字符数限制
        val maxContent = aiSettings.maxContentLength
        if (maxContent > 0) {
            requestBody.put("max_tokens", maxContent)
        }

        // 添加思考过程参数
        val parameters = JSONObject()
        parameters.put(THINKING_KEY, true)
        requestBody.put("parameters", parameters)

        return requestBody.toString()
    }

    /**
     * 构建带图片的API请求体
     */
    private fun buildRequestBodyWithImages(
        prompt: String,
        useMarkdown: Boolean,
        historicalJournals: List<JournalData>
    ): String {
        val messagesArray = JSONArray()

        // 添加系统消息
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")

        // 根据是否需要Markdown格式和历史日记调整提示语
        val historicalJournalsText = formatHistoricalJournals(historicalJournals)
        val systemPrompt = if (historicalJournals.isNotEmpty()) {
            val markdownPrompt = if (useMarkdown) {
                "\n请使用Markdown格式生成内容，合理使用标记语法增强可读性。"
            } else {
                ""
            }
            SYSTEM_PROMPT_WITH_IMAGE_AND_HISTORY + markdownPrompt + "\n\n" + historicalJournalsText
        } else {
            if (useMarkdown) {
                "$SYSTEM_PROMPT_WITH_IMAGE\n请使用Markdown格式生成内容，合理使用标记语法增强可读性。"
            } else {
                SYSTEM_PROMPT_WITH_IMAGE
            }
        }

        val systemContent = JSONArray()
        val systemTextContent = JSONObject()
        systemTextContent.put("type", "text")
        systemTextContent.put("text", systemPrompt)
        systemContent.put(systemTextContent)
        systemMessage.put("content", systemContent)
        messagesArray.put(systemMessage)

        // 添加用户消息（包含图片和文本）
        val userMessage = JSONObject()
        userMessage.put("role", "user")
        val userContent = JSONArray()

        // 添加图片
        for (imageUri in _uiState.value.images) {
            val imageContent = JSONObject()
            imageContent.put("type", "image_url")
            val imageUrlObj = JSONObject()

            // 将Uri转换为Base64格式
            val context = dependencyManager.getContext()
            val base64Image = ImageBase64Utils.uriToBase64(context, imageUri)

            if (base64Image != null) {
                // 使用转换后的Base64字符串
                imageUrlObj.put("url", base64Image)
                imageContent.put("image_url", imageUrlObj)
                userContent.put(imageContent)
            } else {
                Log.e(TAG, "Failed to convert image to Base64: $imageUri")
                throw IOException("图片处理失败，请重试")
            }
        }

        // 处理提示词
        var finalPrompt = prompt

        // 当使用qvq-max时，确保提示词不为空，并将系统提示添加到提示词前面
        if (_uiState.value.selectedModel == "qvq-max") {
            // 如果用户没有提供提示词，则创建一个简单的默认提示
            if (prompt.isBlank()) {
                finalPrompt = "描述这张图片"
            }

            // 将系统提示词添加到用户提示词前面
            // 创建简短版的系统提示
            val shortSystemPrompt =
                "你是一位专业的日记生成助手。请根据图片内容，生成自然、流畅的日记，使用第一人称视角，100-300字，不需标题或日期。"
            finalPrompt = "$shortSystemPrompt\n\n$finalPrompt"
        }

        // 添加文本
        val textContent = JSONObject()
        textContent.put("type", "text")
        textContent.put("text", finalPrompt)
        userContent.put(textContent)

        userMessage.put("content", userContent)
        messagesArray.put(userMessage)

        // 构建完整请求体
        val requestBody = JSONObject()
        requestBody.put("model", _uiState.value.selectedModel)
        requestBody.put("messages", messagesArray)
        requestBody.put("stream", true)

        val streamOptions = JSONObject()
        streamOptions.put("include_usage", true)
        requestBody.put("stream_options", streamOptions)

        // 添加最大字符数限制
        val maxContent = aiSettings.maxContentLength
        if (maxContent > 0) {
            requestBody.put("max_tokens", maxContent)
        }

        // 添加思考过程参数
        val parameters = JSONObject()
        parameters.put(THINKING_KEY, true)
        parameters.put("vl_high_resolution_images", true)  // 启用高分辨率图像处理
        requestBody.put("parameters", parameters)

        return requestBody.toString()
    }

    /**
     * 发送API请求
     */
    private fun sendRequest(requestBodyJson: String) {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestBodyJson.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${APIKey.DASHSCOPE_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
        Log.d(TAG, "sendRequest: $requestBodyJson")
        // 创建一个StringBuilder来收集流式响应
        val contentBuilder = StringBuilder()
        val thinkingBuilder = StringBuilder()

        // 创建新的Call并保存引用
        currentCall = client.newCall(request)

        currentCall?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (call.isCanceled()) {
                    // 请求已被取消，由cancelGeneration处理
                    return
                }

                Log.e(TAG, "API请求失败", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        canCancel = false,
                        error = "API请求失败: ${e.message}"
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (call.isCanceled()) {
                    // 请求已被取消，由cancelGeneration处理
                    response.close()
                    return
                }

                if (!response.isSuccessful) {
                    Log.e(TAG, "API请求不成功: ${response.code}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            canCancel = false,
                            error = "API请求不成功: ${response.code}"
                        )
                    }
                    response.close()
                    return
                }

                try {
                    // 处理流式响应
                    response.body?.source()?.let { source ->
                        while (!source.exhausted() && !call.isCanceled()) {
                            val line = source.readUtf8Line() ?: continue
                            if (line.isEmpty()) continue

                            if (line.startsWith("data: ")) {
                                val jsonData = line.substring(6)
                                if (jsonData == "[DONE]") {
                                    // 流结束
                                    Log.d(TAG, "生成完成: $contentBuilder")
                                    Log.d(TAG, "思考过程: $thinkingBuilder")
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            canCancel = false,
                                            generatedContent = contentBuilder.toString().trim(),
                                            thinking = thinkingBuilder.toString().trim()
                                        )
                                    }
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
                                                    val thinking = delta.getString(THINKING_KEY)
                                                    thinkingBuilder.append(thinking)

                                                    // 更新UI状态中的思考过程
                                                    _uiState.update {
                                                        it.copy(
                                                            thinking = thinkingBuilder.toString(),
                                                            isThinking = true
                                                        )
                                                    }
                                                } else if (delta.has("content")) {
                                                    val content = delta.getString("content")
                                                    contentBuilder.append(content)

                                                    // 更新UI状态
                                                    _uiState.update {
                                                        it.copy(
                                                            isThinking = false,
                                                            generatedContent = contentBuilder.toString()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "解析JSON错误", e)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (!call.isCanceled()) {
                        Log.e(TAG, "处理响应失败", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                canCancel = false,
                                error = "处理响应失败: ${e.message}"
                            )
                        }
                    }
                } finally {
                    response.close()
                    // 请求完成，清除引用
                    if (currentCall == call) {
                        currentCall = null
                    }
                }
            }
        })
    }

    /**
     * 切换是否使用历史日记
     */
    fun toggleUseHistoricalJournals() {
        _uiState.update { it.copy(useHistoricalJournals = !it.useHistoricalJournals) }
    }

    /**
     * 设置历史日记数量
     */
    fun setHistoricalJournalsCount(count: Int) {
        if (count in 1..10) { // 限制范围为1-10篇
            _uiState.update { it.copy(historicalJournalsCount = count) }
        }
    }

    /**
     * 设置AI模型类型
     */
    fun setAIModel(modelType: String) {
        _uiState.update { it.copy(selectedModel = modelType) }
    }

    /**
     * 清除已生成的内容
     */
    fun clearGeneratedContent() {
        _uiState.update { it.copy(generatedContent = "", error = null, thinking = null) }
    }

    /**
     * 添加图片
     */
    fun addImages(images: List<Uri>) {
        if (images.isEmpty()) return

        _uiState.update { it.copy(images = images, useImages = true) }

        // 如果当前模型不支持图片，自动切换到支持图片的模型
        if (!isModelSupportImage(_uiState.value.selectedModel)) {
            // 默认使用 qvq-max 模型
            setAIModel("qvq-max")
            SnackBarUtils.showSnackBar("已自动切换到支持图片的模型")
        }
    }

    /**
     * 清除图片
     */
    fun clearImages() {
        _uiState.update { it.copy(images = emptyList(), useImages = false) }
    }

    /**
     * 切换是否使用图片
     */
    fun toggleUseImages() {
        val newUseImages = !_uiState.value.useImages
        _uiState.update { it.copy(useImages = newUseImages) }

        // 如果启用图片但没有选择图片，不做任何操作
        if (newUseImages && _uiState.value.images.isEmpty()) {
            _uiState.update { it.copy(useImages = false) }
            SnackBarUtils.showSnackBar("请先添加图片")
        }

        // 如果启用图片但当前模型不支持图片，切换到支持图片的模型
        if (newUseImages && !isModelSupportImage(_uiState.value.selectedModel)) {
            // 默认使用 qvq-max 模型
            setAIModel("qvq-max")
        }
    }

    /**
     * 检查模型是否支持图片
     */
    fun isModelSupportImage(modelId: String): Boolean {
        return AIModels.AVAILABLE_IMAGE_MODELS.containsKey(modelId)
    }

    /**
     * 清除整个状态
     */
    fun clearState() {
        // 取消正在进行的请求
        cancelGeneration()

        _uiState.update {
            AIWritingUiState(
                selectedModel = aiSettings.modelType,
                useHistoricalJournals = aiSettings.useHistoricalJournalsDefault,
                historicalJournalsCount = aiSettings.historicalJournalsCountDefault
            )
        }
    }
} 