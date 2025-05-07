# Journal App MVVM 重组方案

## 项目架构说明

本重组方案遵循 MVVM (Model-View-ViewModel) 架构，并按照清晰的职责分离原则进行目录组织。

### 结构概览

```
app/src/main/kotlin/ovo/sypw/journal/
├── presentation/           # UI 层，包含界面和视图模型
│   ├── screens/            # 各个页面/屏幕
│   ├── components/         # 可复用UI组件
│   └── viewmodels/         # 视图模型，处理UI逻辑与状态
│
├── domain/                 # 业务逻辑层，纯Kotlin，不依赖Android框架
│   ├── usecases/           # 用例，表示应用程序的业务逻辑
│   ├── models/             # 领域模型，表示业务实体
│   └── repositories/       # 仓库接口，定义数据操作契约
│
├── data/                   # 数据层，负责获取和存储数据
│   ├── local/              # 本地数据源
│   │   ├── database/       # Room数据库
│   │   └── preferences/    # SharedPreferences
│   ├── remote/             # 远程数据源
│   │   ├── api/            # API服务接口
│   │   └── models/         # API数据模型
│   └── repositories/       # 仓库实现
│
└── common/                 # 公共模块
    ├── utils/              # 工具类
    ├── theme/              # 应用主题
    └── di/                 # 依赖注入
```

## MVVM架构优势

1. **关注点分离**：UI、业务逻辑和数据访问明确分离
2. **可测试性**：各层可独立测试，尤其是业务逻辑层
3. **可维护性**：代码组织清晰，便于维护和扩展
4. **可重用性**：领域层和数据层可在不同UI实现间共享

## 各层职责说明

### Presentation层 (UI层)

- **Screens**：完整的应用程序屏幕/页面
- **Components**：可复用的UI组件，如卡片、对话框等
- **ViewModels**：管理UI状态和处理UI逻辑，连接域层和UI层

### Domain层 (业务逻辑层)

- **Use Cases**：应用程序的业务规则，每个用例执行一个特定的业务操作
- **Models**：业务实体模型，表示核心业务概念
- **Repositories (interfaces)**：定义数据操作的接口，隔离业务逻辑和数据源

### Data层 (数据层)

- **Local**：本地数据存储，如数据库和SharedPreferences
- **Remote**：远程数据源，如网络API
- **Repositories (implementations)**：实现Domain层定义的仓库接口，协调本地和远程数据源

### Common (公共模块)

- **Utils**：全局工具类和扩展函数
- **Theme**：应用的主题相关代码
- **DI**：依赖注入设置，提供各组件实例

## 数据流

1. UI层通过ViewModel请求数据或执行操作
2. ViewModel调用相应的UseCase
3. UseCase通过Repository接口获取或处理数据
4. Repository实现协调本地和远程数据源
5. 数据按照相同路径返回到UI层

## 迁移说明

使用提供的`reorganize_files.sh`脚本执行初步文件迁移。迁移后需要：

1. 调整所有文件的包路径声明
2. 更新导入语句以反映新的包结构
3. 确保依赖注入正确配置
4. 逐一测试功能确保正常工作

## 注意事项

1. 脚本只复制文件，不会删除原始文件，以确保迁移过程安全
2. 迁移完成并测试无误后，可以删除原始目录和文件
3. 领域模型应该独立于数据模型，通常需要定义专用的领域实体
4. 确保仓库实现正确地映射数据模型和领域模型 