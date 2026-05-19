# 灵光收集箱 Native

这是“灵光收集箱”的原生 Android MVP，按 `spark-inbox-preview/index.html` 当前方向实现。

范围：

- 首页快速记录。
- 首页最多展示 3 条进行中记录。
- 记录状态：未处理、进行中、已完成、已放弃。
- 圆环快捷推进状态：未处理 -> 进行中 -> 已完成；已完成/已放弃重新打开。
- 查看页支持搜索正文和状态筛选。
- 记录内容可编辑。
- 使用 SharedPreferences 本地保存。

暂不做账号、同步、系统提醒、标签、主题、导出和复杂项目管理。

## 构建

运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
```

APK 输出到：

```text
dist\spark-inbox-debug.apk
```
