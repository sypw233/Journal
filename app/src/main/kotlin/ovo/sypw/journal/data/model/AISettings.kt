package ovo.sypw.journal.data.model

/**
 * AI设置数据模型
 * 包含AI写作功能的所有配置项
 */
data class AISettings(
    // 模型设置
    val modelType: String = "qwen-turbo", // 默认使用通义千问-turbo模型
    
    // 历史参考设置
    val useHistoricalJournalsDefault: Boolean = true, // 默认使用历史日记参考
    val historicalJournalsCountDefault: Int = 5, // 默认参考5篇历史日记
    
    // 生成设置
    val maxContentLength: Int = 500, // 内容最大长度
    val defaultPromptTemplate: String = "", // 默认提示词模板
    
    // 高级设置
    val showAdvancedSettingsDefault: Boolean = false // 默认不显示高级设置
)

/**
 * 可用的语言模型列表
 */
object AIModels {
    // 模型ID和对应显示名称的映射
    val AVAILABLE_MODELS = mapOf(
        // 通义千问系列
        "qwen-turbo" to "QWEN-Turbo",
        "qwen-plus" to "QWEN-Plus",
        "qwen-max" to "QWEN-Max",
        "qwen-max-1201" to "QWEN-Max-12B",
        "qwen-max-longcontext" to "QWEN-Max-LongContext",
        "qvq-max" to "QWEN-VQ-Max",
        "qwen-vl-plus" to "QWEN-VL-Plus",
        
        // DeepSeek系列
        "deepseek-r1" to "DeepSeek-R1",
        "deepseek-v3" to "DeepSeek-V3",
        
        // Llama3系列
        "llama3-8b-instruct" to "Llama3-8B",
        "llama3-70b-instruct" to "Llama3-70B"
    )
    
    // 获取模型显示名称
    fun getModelDisplayName(modelId: String): String {
        return AVAILABLE_MODELS[modelId] ?: "未知模型"
    }
    
    // 获取模型按类别分组
    fun getModelsByCategory(): Map<String, Map<String, String>> {
        return mapOf(
            "通义千问系列" to AVAILABLE_MODELS.filterKeys { it.startsWith("qwen") || it.startsWith("qvq") },
            "DeepSeek系列" to AVAILABLE_MODELS.filterKeys { it.startsWith("deepseek") },
            "Llama3系列" to AVAILABLE_MODELS.filterKeys { it.startsWith("llama3") }
        )
    }
} 