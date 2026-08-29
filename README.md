# 黎喵记录 (LiMiaoNotes)

原生 Android 版 · v0.1

**技术栈**：Kotlin + Jetpack Compose + DataStore + 本地存储（纯离线，无网络）

## 功能
- **花销**：记账（支出/收入）、分类、备注、日期时间、月统计、按天流水、编辑/删除
- **经期**：日历标记（设为开始/结束）、流量/症状按天记录、经期段自动合并、周期预测
- **随手记**：笔记增删改、置顶、标签
- **设置**：导出（JSON）/ 导入合并 / 清空、周期参数、关于

## 构建
用 Android Studio 打开本目录 → 等 Gradle Sync → Run 到模拟器/真机

## 目录
```
app/src/main/java/com/limiao/notes/
├── MainActivity.kt       导航 + 底部栏 + 抽屉
├── data/
│   ├── Models.kt         数据模型 + JSON 序列化 + 常量
│   └── Store.kt          DataStore 存储
└── ui/
    ├── Theme.kt          配色（粉色主题）
    ├── Common.kt         月份工具
    ├── HomeScreen.kt     花销页
    ├── PeriodScreen.kt   经期页
    ├── NotesScreen.kt    随手记
    └── SettingsScreen.kt 设置
```
