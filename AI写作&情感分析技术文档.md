# AI写作与情感分析模块技术文档

## 项目概述

本文档详细介绍了基于Kotlin和Jetpack Compose开发的日记应用中的AI写作和情感分析功能模块。该应用采用MVVM架构模式，使用Hilt进行依赖注入，Room数据库进行本地存储，并集成了百度千帆API和本地情感分析模型。

## 技术架构

### 整体架构
- **开发语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构模式**: MVVM (Model-View-ViewModel)
- **依赖注入**: Hilt
- **数据库**: Room
- **网络请求**: OkHttp + Retrofit
- **异步处理**: Kotlin Coroutines

### 项目结构
```
/Users/sypw/StudioProjects/Journal/
├── app/src/main/kotlin/ovo/sypw/journal/
│   ├── ui/viewmodels/          # ViewModel层
│   ├── data/
│   │   ├── model/              # 数据模型
│   │   ├── repositories/       # 仓库实现
│   │   ├── repository/         # 仓库接口
│   │   └── local/database/     # 数据库实体和DAO
│   ├── common/
│   │   ├── utils/              # 工具类
│   │   └── di/                 # 依赖注入模块
│   └── di/                     # 全局依赖管理
```

## AI写作模块

### 核心组件

#### 1. AIWritingViewModel
**文件路径**: `app/src/main/kotlin/ovo/sypw/journal/ui/viewmodels/AIWritingViewModel.kt`

**主要功能**:
- 管理AI写作的UI状态
- 处理用户输入和AI模型选择
- 调用百度千帆API生成内容
- 支持基于历史日记和图片的内容生成

**核心特性**:
```kotlin
data class AIWritingUiState(
    val isLoading: Boolean = false,
    val generatedContent: String = "",
    val selectedModel: String = "qwen-turbo",
    val useHistoricalJournals: Boolean = false,
    val useImages: Boolean = false,
    val error: String? = null
)
```


#### 2. 智能提示词工程

**基础写作提示词**:
```
你是一个专业的日记写作助手。请根据用户的简单描述，帮助他们写一篇详细、生动、有感情的日记。
要求：
1. 语言自然流畅，富有感情色彩
2. 适当添加细节描述，让内容更加丰富
3. 保持积极正面的情感基调
4. 字数控制在200-500字之间
5. 使用第一人称视角
```

**历史日记增强提示词**:
```
你是一个专业的日记写作助手。请根据用户的描述和他们的历史日记风格，写一篇新的日记。

历史日记参考：
{历史日记内容}

请参考上述历史日记的写作风格、用词习惯和情感表达方式，为用户写一篇新的日记。
```

### API集成

**百度千帆API配置**:
- 支持多种大语言模型
- 自动token管理和刷新
- 错误处理和重试机制
- 流式响应支持

## 情感分析模块

### 架构设计

#### 1. 数据层 (Data Layer)

**SentimentEntity** - 数据库实体
```kotlin
@Entity(
    tableName = "sentiments",
    foreignKeys = [ForeignKey(
        entity = JournalEntity::class,
        parentColumns = ["id"],
        childColumns = ["journalId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("journalId", unique = true)]
)
data class SentimentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val journalId: Int,
    val sentimentType: String,
    val positiveScore: Float,
    val negativeScore: Float,
    val dominantEmotion: String,
    val confidence: Float,
    val timestamp: Long
)
```

**SentimentDao** - 数据访问对象
```kotlin
@Dao
interface SentimentDao {
    @Query("SELECT * FROM sentiments WHERE journalId = :journalId")
    suspend fun getSentimentByJournalId(journalId: Int): SentimentEntity?
    
    @Query("SELECT * FROM sentiments WHERE sentimentType = :sentimentType")
    suspend fun getSentimentsByType(sentimentType: String): List<SentimentEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentiment(sentiment: SentimentEntity): Long
    
    // 更多数据库操作...
}
```

#### 2. 仓库层 (Repository Layer)

**SentimentRepository** - 接口定义
```kotlin
interface SentimentRepository {
    suspend fun getSentimentByJournalId(journalId: Int): SentimentData?
    suspend fun getAllSentiments(): List<SentimentData>
    suspend fun getSentimentsByType(sentimentType: SentimentType): List<SentimentData>
    suspend fun saveSentiment(sentiment: SentimentData)
    suspend fun batchSaveSentiments(sentiments: List<SentimentData>)
    suspend fun deleteSentimentByJournalId(journalId: Int): Boolean
    suspend fun getSentimentCount(): Int
    suspend fun getJournalIdsByType(sentimentType: SentimentType): List<Int>
    suspend fun getJournalsWithSentiments(): List<JournalWithSentiment>
}
```

**LocalSentimentRepository** - 本地实现
- 实现SentimentRepository接口
- 处理数据库操作
- 提供数据转换功能
- 支持批量操作和缓存

#### 3. 业务逻辑层 (ViewModel Layer)

**SentimentViewModel**
```kotlin
@HiltViewModel
class SentimentViewModel @Inject constructor(
    private val sentimentRepository: SentimentRepository,
    private val sentimentApiService: SentimentApiService
) : ViewModel() {
    
    private val _sentimentCache = mutableMapOf<Int, SentimentData>()
    
    // 单条分析
    suspend fun analyzeSentiment(journalId: Int, content: String): SentimentData?
    
    // 批量分析
    suspend fun batchAnalyzeSentiments(
        journals: List<JournalData>,
        onProgress: (Int, Int) -> Unit
    ): List<SentimentData>
    
    // 按类型筛选
    suspend fun getSentimentsByType(type: SentimentType): List<SentimentData>
    
    // 时间段筛选
    suspend fun getSentimentsByPeriod(
        startTime: Long,
        endTime: Long
    ): List<SentimentData>
}
```

### API服务

#### SentimentApiService
**文件路径**: `app/src/main/kotlin/ovo/sypw/journal/common/utils/SentimentApiService.kt`

**核心功能**:
- 集成百度千帆API
- 自定义情感分析提示词
- 结构化输出处理
- 错误处理和重试机制

**API调用流程**:
1. 构建分析提示词
2. 调用百度千帆API
3. 解析JSON响应
4. 转换为SentimentData对象

**提示词模板**:
```
请分析以下文本的情感倾向，并按照指定格式返回结果。

文本内容：{用户输入文本}

请返回JSON格式的分析结果：
{
  "score": 数值(0-100，表示积极程度),
  "label": "情感标签(非常负面/负面/中性/正面/非常正面)"
}
```

### 情感类型定义

```kotlin
enum class SentimentType {
    POSITIVE,    // 积极情感
    NEGATIVE,    // 消极情感  
    NEUTRAL,     // 中性情感
    UNKNOWN      // 未知/错误
}
```

### 数据模型

#### SentimentData
```kotlin
data class SentimentData(
    val journalId: Int,
    val sentimentType: SentimentType,
    val positiveScore: Float,        // 积极情感得分 (0.0-1.0)
    val negativeScore: Float,        // 消极情感得分 (0.0-1.0)
    val dominantEmotion: String,     // 主要情绪
    val confidence: Float,           // 置信度
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromApiResult(journalId: Int, result: SentimentApiService.SentimentResult): SentimentData
    }
}
```

### UI组件

#### SentimentAnalysisDialog
**功能**:
- 显示情感分析结果
- 可视化情感得分
- 支持重新分析
- 历史记录查看

**主要特性**:
- Material Design 3风格
- 动画效果支持
- 响应式布局
- 无障碍访问支持

## 情感分析模型训练与部署

### 训练数据集
**位置**: `/Users/sypw/PycharmProjects/sentiment_data/`

**数据格式**:
```json
{
  "prompt": "今天醒来时，阳光透过窗帘缝隙洒在枕头上...",
  "response": {
    "label": "非常正面",
    "score": 95
  }
}
```

**数据集特点**:
- 695条标注样本
- 涵盖5种情感类别
- 真实日记场景数据
- 中文语境优化

### 模型架构

**基础模型**: Erlangshen-Roberta-110M-Sentiment
- 基于RoBERTa架构
- 1.1亿参数
- 中文情感分析优化
- 支持5分类情感识别

**模型输出**:
- 情感类别: 非常负面、负面、中性、正面、非常正面
- 置信度分数: 0-1之间的浮点数
- 心情评分: 0-100的整数评分

### 部署架构

#### 本地API服务
**文件**: `/Users/sypw/PycharmProjects/sentiment_data/app.py`

**技术栈**:
- Flask Web框架
- PyTorch深度学习框架
- Transformers模型库
- Gunicorn WSGI服务器

**API端点**:
- `GET /`: 服务信息
- `POST /analyze`: 单条文本分析
- `POST /batch-analyze`: 批量文本分析

#### Docker部署

**Dockerfile配置**:
```dockerfile
FROM python:3.9-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
ENV PORT=5000
EXPOSE 5000
CMD gunicorn --bind 0.0.0.0:$PORT app:app
```

**Docker Compose配置**:
```yaml
version: '3.8'
services:
  sentiment-api:
    build: .
    ports:
      - "5000:5000"
    volumes:
      - ./multilingual-sentiment-analysis:/app/multilingual-sentiment-analysis
    restart: unless-stopped
```

### 模型评估

**评估指标**:
- 准确率 (Accuracy)
- 精确率 (Precision)
- 召回率 (Recall)
- F1分数
- 混淆矩阵

**评估结果**:
- 基础模型准确率: 85%+
- 增强模型准确率: 90%+
- 支持错误分析和模型对比

## 依赖注入配置

### SentimentModule
**文件路径**: `app/src/main/kotlin/ovo/sypw/journal/common/di/SentimentModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SentimentModule {
    
    @Provides
    @Singleton
    fun provideSentimentApiService(
        @ApplicationContext context: Context
    ): SentimentApiService = SentimentApiService(context)
    
    @Provides
    @Singleton
    fun provideSentimentDao(database: JournalDatabase): SentimentDao =
        database.sentimentDao()
    
    @Provides
    @Singleton
    fun provideSentimentRepository(sentimentDao: SentimentDao): SentimentRepository =
        LocalSentimentRepository(sentimentDao)
}
```

### AppDependencyManager
**文件路径**: `app/src/main/kotlin/ovo/sypw/journal/di/AppDependencyManager.kt`

**功能**:
- 集中管理全局依赖
- 提供依赖项访问接口
- 处理初始化逻辑
- 支持依赖项验证

## 数据库设计

### JournalDatabase
**版本**: 6
**实体**: JournalEntity, SentimentEntity

**关系设计**:
- 一对一关系: Journal ↔ Sentiment
- 外键约束: 级联删除
- 索引优化: journalId唯一索引

### 数据迁移
- 支持破坏性迁移
- 版本控制管理
- 数据完整性保证

## 性能优化

### 缓存策略
- ViewModel层缓存
- 数据库查询优化
- API响应缓存
- 图片资源缓存

### 异步处理
- Kotlin Coroutines
- 后台线程处理
- UI线程保护
- 取消机制支持

### 内存管理
- 及时释放资源
- 避免内存泄漏
- 大数据分页处理
- 图片压缩优化

## 错误处理

### 网络错误
- 连接超时处理
- 重试机制
- 降级策略
- 用户友好提示

### 数据错误
- 数据验证
- 异常捕获
- 日志记录
- 恢复机制

### API错误
- 状态码处理
- 错误消息解析
- 限流处理
- 认证失败处理

## 安全考虑

### API密钥管理
- SharedPreferences存储
- 加密保护
- 动态配置
- 权限控制

### 数据隐私
- 本地数据加密
- 敏感信息脱敏
- 用户授权
- 数据清理

## 测试策略

### 单元测试
- ViewModel测试
- Repository测试
- 工具类测试
- 数据转换测试

### 集成测试
- API集成测试
- 数据库集成测试
- 端到端测试
- 性能测试

### UI测试
- Compose测试
- 用户交互测试
- 界面响应测试
- 无障碍测试

## 未来扩展

### 功能扩展
- 多语言情感分析
- 情感趋势分析
- 个性化推荐
- 社交分享功能

### 技术升级
- 模型版本更新
- API版本迁移
- 架构优化
- 性能提升

### 部署优化
- 云端部署
- CDN加速
- 负载均衡
- 监控告警

## 总结

本技术文档详细介绍了AI写作和情感分析模块的完整实现，包括架构设计、核心组件、API集成、数据库设计、模型训练部署等各个方面。该模块采用现代化的Android开发技术栈，具备良好的可扩展性和维护性，为用户提供智能化的日记写作和情感分析体验。

通过本地模型和云端API的结合，实现了高效、准确的情感分析功能，同时保证了用户数据的隐私和安全。未来可以根据用户反馈和技术发展，持续优化和扩展功能。
        