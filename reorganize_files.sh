#!/bin/bash

# 项目重组脚本 - 基于MVVM架构
# 这个脚本会将现有项目文件重新组织到更符合MVVM架构的目录结构中

# 确保目标目录存在
mkdir -p app/src/main/kotlin/ovo/sypw/journal/presentation/screens
mkdir -p app/src/main/kotlin/ovo/sypw/journal/presentation/components
mkdir -p app/src/main/kotlin/ovo/sypw/journal/presentation/viewmodels
mkdir -p app/src/main/kotlin/ovo/sypw/journal/domain/usecases
mkdir -p app/src/main/kotlin/ovo/sypw/journal/domain/models
mkdir -p app/src/main/kotlin/ovo/sypw/journal/domain/repositories
mkdir -p app/src/main/kotlin/ovo/sypw/journal/data/local/database
mkdir -p app/src/main/kotlin/ovo/sypw/journal/data/local/preferences
mkdir -p app/src/main/kotlin/ovo/sypw/journal/data/remote/api
mkdir -p app/src/main/kotlin/ovo/sypw/journal/data/remote/models
mkdir -p app/src/main/kotlin/ovo/sypw/journal/data/repositories
mkdir -p app/src/main/kotlin/ovo/sypw/journal/common/utils
mkdir -p app/src/main/kotlin/ovo/sypw/journal/common/theme
mkdir -p app/src/main/kotlin/ovo/sypw/journal/common/di

# 移动文件 - 仅复制，不删除原始文件
# 这样可以确保在实际迁移前保留原始文件，迁移完成后再删除

# 1. 移动 Presentation 层文件

# 1.1 Screens
cp app/src/main/kotlin/ovo/sypw/journal/ui/screen/*.kt app/src/main/kotlin/ovo/sypw/journal/presentation/screens/

# 1.2 Components
cp app/src/main/kotlin/ovo/sypw/journal/ui/components/*.kt app/src/main/kotlin/ovo/sypw/journal/presentation/components/

# 1.3 ViewModels
cp app/src/main/kotlin/ovo/sypw/journal/viewmodel/*.kt app/src/main/kotlin/ovo/sypw/journal/presentation/viewmodels/

# 1.4 UI States
cp app/src/main/kotlin/ovo/sypw/journal/ui/list/JournalListState.kt app/src/main/kotlin/ovo/sypw/journal/presentation/screens/
cp app/src/main/kotlin/ovo/sypw/journal/ui/main/MainScreenState.kt app/src/main/kotlin/ovo/sypw/journal/presentation/screens/

# 2. 移动 Domain 层文件

# 2.1 Models - 理想情况下，应该创建专用的领域模型，而不是直接使用数据层模型
# 这里仅准备目录，实际项目中应该创建合适的领域模型

# 2.2 Repositories (interfaces)
# 创建接口文件（实际项目中应该有专门的仓库接口定义）
echo "package ovo.sypw.journal.domain.repositories

interface JournalRepository {
    // 领域层仓库接口方法定义
}
" > app/src/main/kotlin/ovo/sypw/journal/domain/repositories/JournalRepository.kt

# 3. 移动 Data 层文件

# 3.1 Local - 本地数据源
cp app/src/main/kotlin/ovo/sypw/journal/data/database/*.kt app/src/main/kotlin/ovo/sypw/journal/data/local/database/
cp app/src/main/kotlin/ovo/sypw/journal/data/JournalPreferences.kt app/src/main/kotlin/ovo/sypw/journal/data/local/preferences/

# 3.2 Remote - 远程数据源
cp app/src/main/kotlin/ovo/sypw/journal/data/api/*.kt app/src/main/kotlin/ovo/sypw/journal/data/remote/api/
cp app/src/main/kotlin/ovo/sypw/journal/data/APIKey.kt app/src/main/kotlin/ovo/sypw/journal/data/remote/

# 3.3 Models - 数据模型
cp app/src/main/kotlin/ovo/sypw/journal/data/model/*.kt app/src/main/kotlin/ovo/sypw/journal/data/remote/models/

# 3.4 Repositories - 实现
cp app/src/main/kotlin/ovo/sypw/journal/data/repository/*.kt app/src/main/kotlin/ovo/sypw/journal/data/repositories/
cp app/src/main/kotlin/ovo/sypw/journal/data/sync/*.kt app/src/main/kotlin/ovo/sypw/journal/data/repositories/

# 4. 移动公共模块文件

# 4.1 Utils - 工具类
cp app/src/main/kotlin/ovo/sypw/journal/utils/*.kt app/src/main/kotlin/ovo/sypw/journal/common/utils/

# 4.2 Theme - 主题
cp app/src/main/kotlin/ovo/sypw/journal/ui/theme/*.kt app/src/main/kotlin/ovo/sypw/journal/common/theme/
cp app/src/main/kotlin/ovo/sypw/journal/ui/theme/animiation/*.kt app/src/main/kotlin/ovo/sypw/journal/common/theme/

# 4.3 DI - 依赖注入
cp app/src/main/kotlin/ovo/sypw/journal/di/*.kt app/src/main/kotlin/ovo/sypw/journal/common/di/

# 5. 移动应用入口文件
cp app/src/main/kotlin/ovo/sypw/journal/MainActivity.kt app/src/main/kotlin/ovo/sypw/journal/
cp app/src/main/kotlin/ovo/sypw/journal/JournalApplication.kt app/src/main/kotlin/ovo/sypw/journal/

echo "文件重组完成。请检查新结构并调整包引用。"
echo "注意: 这只是文件的复制，原始文件仍然存在于旧位置。"
echo "在确认所有功能正常后，您可以安全地删除旧文件。" 