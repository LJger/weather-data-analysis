# 个性化主题系统说明

本系统通过 Pinia Store、CSS 变量和 Element Plus 变量覆盖实现主题切换。主题设置主要在“个性化服务”页面维护，也可通过布局组件中的主题入口快速调整。

## 1. 功能范围

- 预设主题。
- 浅色、深色、跟随系统。
- 主色、辅助色、成功色、警告色、错误色。
- 字体大小。
- 圆角大小。
- 动画开关。
- localStorage 持久化。

## 2. 关键文件

| 文件 | 作用 |
|---|---|
| `src/stores/theme.ts` | 主题状态、预设主题、应用主题、持久化 |
| `src/styles/theme.css` | 全局 CSS 变量和主题样式 |
| `src/components/ThemeToggle.vue` | 快速主题切换组件 |
| `src/views/PersonalizedService.vue` | 完整主题和偏好设置页面 |
| `src/components/Layout.vue` | 普通用户布局中的主题入口 |
| `src/components/AdminLayout.vue` | 管理员布局中的主题入口 |

## 3. 推荐使用方式

业务组件中不要硬编码主题颜色，优先使用 CSS 变量：

```vue
<template>
  <section class="panel">
    <h2>标题</h2>
    <p>内容</p>
  </section>
</template>

<style scoped>
.panel {
  color: var(--theme-text);
  background: var(--theme-surface);
  border: 1px solid var(--theme-border);
  border-radius: var(--el-border-radius-base);
}

.panel h2 {
  color: var(--theme-primary);
}
</style>
```

如需在脚本中读取或应用主题：

```ts
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
themeStore.selectPresetTheme(themeStore.presetThemes[0])
```

## 4. 常用 CSS 变量

### 颜色

- `--theme-primary`
- `--theme-secondary`
- `--theme-accent`
- `--theme-success`
- `--theme-warning`
- `--theme-error`

### 文本和背景

- `--theme-background`
- `--theme-surface`
- `--theme-card-background`
- `--theme-text`
- `--theme-text-secondary`
- `--theme-border`

### Element Plus 相关

- `--el-color-primary`
- `--el-font-size-base`
- `--el-border-radius-base`
- `--el-transition-duration`

## 5. 持久化

主题配置保存到 localStorage。用户再次进入系统时，Store 会恢复上次配置，并同步到文档根节点 CSS 变量。

## 6. 扩展预设主题

在 `src/stores/theme.ts` 的预设主题数组中新增配置：

```ts
{
  id: 7,
  name: '新主题',
  description: '主题描述',
  config: {
    primaryColor: '#2f6fdd',
    backgroundMode: 'light',
    fontSize: 14,
    borderRadius: 8,
    animations: true,
    customColors: {
      secondary: '#2f9e6d',
      accent: '#d99025',
      success: '#2f9e6d',
      warning: '#d99025',
      error: '#d84d4d',
    },
  },
}
```

## 7. 页面接入建议

- 新页面外层使用 `var(--theme-background)` 或默认布局背景。
- 卡片、表格、工具栏使用 `var(--theme-card-background)`、`var(--theme-border)`。
- 主要操作使用 Element Plus `type="primary"`，由主题变量自动接管颜色。
- 不要在业务页面中写死大面积单一色值。
- 深色模式下检查文本对比度。

## 8. 当前已接入页面

- 普通用户布局。
- 管理员布局。
- 个性化服务。
- 统计分析。
- GIS 分析。
- 数据预测。
- 管理员用户管理。
- 管理员存储管理。

文档更新时间：2026-05-16。
