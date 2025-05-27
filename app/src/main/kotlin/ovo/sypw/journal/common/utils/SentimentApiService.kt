package ovo.sypw.journal.common.utils

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 情感分析API服务
 * 使用百度千帆API进行情感分析
 */
@Singleton
class SentimentApiService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "SentimentApiService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // API相关配置
    companion object {
        private const val API_URL = "https://qianfan.baidubce.com/v2/chat/completions"
        private const val MODEL_NAME = "aemmxm1o_journal"
    }

    /**
     * 获取API密钥
     */
    fun getApiKey(): String? {
        val sharedPrefs = context.getSharedPreferences("journal_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getString("sentiment_api_key", null)
    }

    /**
     * 分析文本情感
     * @param text 要分析的文本内容
     * @return 情感分析结果
     */
    suspend fun analyzeSentiment(text: String): SentimentResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            throw IllegalStateException("API密钥未设置，请在设置中配置API密钥")
        }

        val prompt = """你是一个专业的情感分析AI，请严格按以下规则处理文本：
# 任务要求
1. 分析输入句子的情感倾向（label）和心情评分（score）
2. 输出必须为合法的JSON格式
3. 评分范围0-100（0最负面，100最正面）
# 情感标签分类标准
"非常负面"：含极端消极/攻击性内容（score≤20）
"负面"：表达不满/沮丧（20<score≤40）
"中性"：情感平淡/客观（40<score≤60）
"正面"：表达满足/愉悦（60<score≤80）
"非常正面"：表达强烈喜悦/感激（score>80）
# 输出示例
{"label": "正面", "score": 75}
# 当前待分析文本:
$text"""

        // 构建请求体，按照百度千帆API的要求
        val requestBody = JSONObject().apply {
            put("model", MODEL_NAME)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("enable_thinking", false)
        }.toString()

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            // 执行请求
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("响应为空")
            Log.d(TAG, "Response: $responseBody")

            if (!response.isSuccessful) {
                throw Exception("API请求失败: ${response.code} - $responseBody")
            }

            // 解析响应JSON
            val jsonResponse = JSONObject(responseBody)
            
            // 获取模型返回的内容
            val content = jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            
            Log.d(TAG, "Raw content: $content")
            
            // 处理可能包含Markdown格式的内容
            var jsonContent = content
            if (content.contains("```json")) {
                jsonContent = content.substringAfter("```json")
                    .substringBefore("```")
                    .trim()
                Log.d(TAG, "Extracted JSON: $jsonContent")
            } else if (content.contains("```")) {
                // 处理可能不带语言标识的代码块
                jsonContent = content.substringAfter("```")
                    .substringBefore("```")
                    .trim()
                Log.d(TAG, "Extracted code block: $jsonContent")
            }
            
            // 解析JSON内容
            try {
                val sentimentJson = JSONObject(jsonContent)
                val label = sentimentJson.getString("label")
                val score = sentimentJson.getInt("score")
                
                return@withContext SentimentResult(
                    label = label,
                    score = score
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse JSON: $jsonContent", e)
                throw Exception("情感分析结果解析失败: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed", e)
            throw Exception("情感分析失败: ${e.message}")
        }
    }

    /**
     * 批量分析多篇文本的情感
     * @param texts 要分析的文本列表
     * @param maxRetries 最大重试次数
     * @return 情感分析结果列表
     */
    suspend fun analyzeBatchSentiment(
        texts: List<String>,
        maxRetries: Int = 3
    ): List<SentimentResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            throw IllegalStateException("API密钥未设置，请在设置中配置API密钥")
        }

        if (texts.isEmpty()) {
            return@withContext emptyList()
        }

        // 构建批量分析prompt
        val prompt = """你是一个专业的情感分析AI，请严格按以下规则处理多条文本：
# 任务要求
1. 分析每条输入文本的情感倾向（label）和心情评分（score）
2. 输出必须为合法的JSON数组格式，每个元素包含label和score字段
3. 评分范围0-100（0最负面，100最正面）
# 情感标签分类标准
"非常负面"：含极端消极/攻击性内容（score≤20）
"负面"：表达不满/沮丧（20<score≤40）
"中性"：情感平淡/客观（40<score≤60）
"正面"：表达满足/愉悦（60<score≤80）
"非常正面"：表达强烈喜悦/感激（score>80）
# 输出格式示例
[
  {"label": "正面", "score": 75},
  {"label": "负面", "score": 30},
  {"label": "中性", "score": 50}
]
# 待分析文本列表（每行一条）:
${texts.mapIndexed { index, text -> "${index + 1}. $text" }.joinToString("\n")}"""

        // 构建请求体
        val requestBody = JSONObject().apply {
            put("model", MODEL_NAME)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("enable_thinking", false)
        }.toString()
        Log.d(TAG, "analyzeBatchSentiment: $requestBody")
        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        var retryCount = 0
        var lastException: Exception? = null
        
        // 重试机制
        while (retryCount < maxRetries) {
            try {
                // 执行请求
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: throw Exception("响应为空")
                Log.d(TAG, "Batch Response: $responseBody")

                if (!response.isSuccessful) {
                    throw Exception("API请求失败: ${response.code} - $responseBody")
                }

                // 解析响应JSON
                val jsonResponse = JSONObject(responseBody)
                
                // 获取模型返回的内容
                val content = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                
                Log.d(TAG, "Batch Raw content: $content")
                
                // 处理可能包含Markdown格式的内容
                var jsonContent = content
                if (content.contains("```json")) {
                    jsonContent = content.substringAfter("```json")
                        .substringBefore("```")
                        .trim()
                    Log.d(TAG, "Extracted JSON Array: $jsonContent")
                } else if (content.contains("```")) {
                    // 处理可能不带语言标识的代码块
                    jsonContent = content.substringAfter("```")
                        .substringBefore("```")
                        .trim()
                    Log.d(TAG, "Extracted code block: $jsonContent")
                }
                
                // 解析JSON数组内容
                try {
                    val resultsArray = JSONArray(jsonContent)
                    val results = mutableListOf<SentimentResult>()
                    
                    for (i in 0 until resultsArray.length()) {
                        val item = resultsArray.getJSONObject(i)
                        val label = item.getString("label")
                        val score = item.getInt("score")
                        results.add(SentimentResult(label, score))
                    }
                    
                    return@withContext results
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse JSON array: $jsonContent", e)
                    throw Exception("批量情感分析结果解析失败: ${e.message}")
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "批量分析尝试 ${retryCount + 1}/$maxRetries 失败: ${e.message}")
                retryCount++
                
                if (retryCount < maxRetries) {
                    // 指数退避策略，每次重试等待时间增加
                    val delayTime = 2000L * (1 shl retryCount)
                    Log.d(TAG, "等待 ${delayTime}ms 后重试...")
                    delay(delayTime)
                }
            }
        }
        
        // 所有重试都失败
        Log.e(TAG, "Batch analysis failed after $maxRetries retries", lastException)
        throw Exception("批量情感分析失败: ${lastException?.message}")
    }

    /**
     * 情感分析结果数据类
     */
    data class SentimentResult(
        val label: String,
        val score: Int
    )
} 