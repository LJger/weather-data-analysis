import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'

export interface ThemeConfig {
  primaryColor: string
  backgroundMode: 'light' | 'dark' | 'auto'
  fontSize: number
  borderRadius: number
  animations: boolean
  customColors?: {
    secondaryColor?: string
    accentColor?: string
    successColor?: string
    warningColor?: string
    errorColor?: string
  }
}

export interface PresetTheme {
  id: number
  name: string
  description: string
  config: ThemeConfig
}

export const useThemeStore = defineStore('theme', () => {
  // 默认主题配置
  const defaultTheme: ThemeConfig = {
    primaryColor: '#1d4f91',
    backgroundMode: 'light',
    fontSize: 14,
    borderRadius: 8,
    animations: true,
    customColors: {
      secondaryColor: '#909399',
      accentColor: '#2f855a',
      successColor: '#2f855a',
      warningColor: '#b7791f',
      errorColor: '#d64545',
    },
  }

  // 当前主题配置
  const currentTheme = ref<ThemeConfig>({ ...defaultTheme })

  // 当前选中的预设主题
  const selectedPresetTheme = ref<PresetTheme | null>(null)

  // 预设主题列表
  const presetThemes = ref<PresetTheme[]>([
    {
      id: 1,
      name: '默认浅色',
      description: '适合日常数据处理的浅色主题',
      config: { ...defaultTheme },
    },
    {
      id: 2,
      name: '深色模式',
      description: '低亮度环境下使用的深色主题',
      config: {
        ...defaultTheme,
        backgroundMode: 'dark',
      },
    },
  ])

  // 计算当前主题的 CSS 变量
  const themeVariables = computed(() => {
    const isDark =
      currentTheme.value.backgroundMode === 'dark' ||
      (currentTheme.value.backgroundMode === 'auto' &&
        window.matchMedia('(prefers-color-scheme: dark)').matches)

    return {
      '--el-color-primary': currentTheme.value.primaryColor,
      '--el-color-primary-light-3': lightenColor(currentTheme.value.primaryColor, 0.3),
      '--el-color-primary-light-5': lightenColor(currentTheme.value.primaryColor, 0.5),
      '--el-color-primary-light-7': lightenColor(currentTheme.value.primaryColor, 0.7),
      '--el-color-primary-light-8': lightenColor(currentTheme.value.primaryColor, 0.8),
      '--el-color-primary-light-9': lightenColor(currentTheme.value.primaryColor, 0.9),
      '--el-color-primary-dark-2': darkenColor(currentTheme.value.primaryColor, 0.2),

      '--el-color-success': currentTheme.value.customColors?.successColor || '#67c23a',
      '--el-color-warning': currentTheme.value.customColors?.warningColor || '#e6a23c',
      '--el-color-danger': currentTheme.value.customColors?.errorColor || '#f56c6c',
      '--el-color-info': currentTheme.value.customColors?.secondaryColor || '#909399',

      '--el-bg-color': isDark ? '#1a1a1a' : '#ffffff',
      '--el-bg-color-page': isDark ? '#0f172a' : '#f4f7fb',
      '--el-bg-color-overlay': isDark ? '#1d1e1f' : '#ffffff',

      '--el-text-color-primary': isDark ? '#e5eaf3' : '#303133',
      '--el-text-color-regular': isDark ? '#cfd3dc' : '#606266',
      '--el-text-color-secondary': isDark ? '#a3a6ad' : '#909399',
      '--el-text-color-placeholder': isDark ? '#8d9095' : '#a8abb2',
      '--el-text-color-disabled': isDark ? '#6c6e72' : '#c0c4cc',

      '--el-border-color': isDark ? '#4c4d4f' : '#dcdfe6',
      '--el-border-color-light': isDark ? '#414243' : '#e4e7ed',
      '--el-border-color-lighter': isDark ? '#363637' : '#ebeef5',
      '--el-border-color-extra-light': isDark ? '#2b2b2c' : '#f2f6fc',
      '--el-border-color-dark': isDark ? '#58585b' : '#d4d7de',
      '--el-border-color-darker': isDark ? '#636466' : '#cdd0d6',

      '--el-fill-color': isDark ? '#303133' : '#f0f2f5',
      '--el-fill-color-light': isDark ? '#262727' : '#f5f7fa',
      '--el-fill-color-lighter': isDark ? '#1d1d1d' : '#fafafa',
      '--el-fill-color-extra-light': isDark ? '#0a0a0a' : '#fafcff',
      '--el-fill-color-dark': isDark ? '#39393a' : '#ebedf0',
      '--el-fill-color-darker': isDark ? '#424243' : '#e6e8eb',
      '--el-fill-color-blank': isDark ? '#1a1a1a' : '#ffffff',

      '--el-font-size': currentTheme.value.fontSize + 'px',
      '--el-border-radius-base': currentTheme.value.borderRadius + 'px',
      '--el-border-radius-small': Math.max(2, currentTheme.value.borderRadius - 2) + 'px',
      '--el-border-radius-round': Math.max(20, currentTheme.value.borderRadius * 2) + 'px',
      '--el-border-radius-circle': '50%',

      // 自定义变量
      '--theme-primary': currentTheme.value.primaryColor,
      '--theme-secondary': currentTheme.value.customColors?.secondaryColor || '#909399',
      '--theme-accent': currentTheme.value.customColors?.accentColor || '#67c23a',
      '--theme-success': currentTheme.value.customColors?.successColor || '#67c23a',
      '--theme-warning': currentTheme.value.customColors?.warningColor || '#e6a23c',
      '--theme-error': currentTheme.value.customColors?.errorColor || '#f56c6c',
      '--theme-background': isDark ? '#1a1a1a' : '#ffffff',
      '--theme-surface': isDark ? '#1f2937' : '#f8fbff',
      '--theme-text': isDark ? '#ffffff' : '#303133',
      '--theme-text-secondary': isDark ? '#a3a6ad' : '#606266',
      '--theme-border': isDark ? '#4c4d4f' : '#dcdfe6',
      '--theme-shadow': isDark ? 'rgba(0, 0, 0, 0.24)' : 'rgba(25, 55, 90, 0.1)',

      // 动画相关
      '--el-transition-duration': currentTheme.value.animations ? '0.3s' : '0s',
      '--el-transition-duration-fast': currentTheme.value.animations ? '0.2s' : '0s',
    }
  })

  // 颜色工具函数
  const lightenColor = (color: string, amount: number): string => {
    const num = parseInt(color.replace('#', ''), 16)
    const amt = Math.round(2.55 * amount * 100)
    const R = (num >> 16) + amt
    const G = ((num >> 8) & 0x00ff) + amt
    const B = (num & 0x0000ff) + amt
    return (
      '#' +
      (
        0x1000000 +
        (R < 255 ? (R < 1 ? 0 : R) : 255) * 0x10000 +
        (G < 255 ? (G < 1 ? 0 : G) : 255) * 0x100 +
        (B < 255 ? (B < 1 ? 0 : B) : 255)
      )
        .toString(16)
        .slice(1)
    )
  }

  const darkenColor = (color: string, amount: number): string => {
    const num = parseInt(color.replace('#', ''), 16)
    const amt = Math.round(2.55 * amount * 100)
    const R = (num >> 16) - amt
    const G = ((num >> 8) & 0x00ff) - amt
    const B = (num & 0x0000ff) - amt
    return (
      '#' +
      (
        0x1000000 +
        (R > 255 ? 255 : R < 0 ? 0 : R) * 0x10000 +
        (G > 255 ? 255 : G < 0 ? 0 : G) * 0x100 +
        (B > 255 ? 255 : B < 0 ? 0 : B)
      )
        .toString(16)
        .slice(1)
    )
  }

  // 应用主题
  const applyTheme = (themeConfig: ThemeConfig) => {
    currentTheme.value = { ...themeConfig }
    updateDocumentVariables()
    saveThemeToStorage()
  }

  // 选择预设主题
  const selectPresetTheme = (theme: PresetTheme) => {
    selectedPresetTheme.value = theme
    applyTheme(theme.config)
  }

  // 更新文档 CSS 变量
  const updateDocumentVariables = () => {
    const root = document.documentElement
    Object.entries(themeVariables.value).forEach(([property, value]) => {
      root.style.setProperty(property, value)
    })
  }

  // 重置为默认主题
  const resetTheme = () => {
    applyTheme(defaultTheme)
    selectedPresetTheme.value = presetThemes.value[0] || null // 默认主题
  }

  // 保存主题到本地存储
  const saveThemeToStorage = () => {
    try {
      localStorage.setItem('theme-config', JSON.stringify(currentTheme.value))
      localStorage.setItem('selected-preset-theme', JSON.stringify(selectedPresetTheme.value))
    } catch (error) {
      console.warn('Failed to save theme to localStorage:', error)
    }
  }

  // 从本地存储加载主题
  const loadThemeFromStorage = () => {
    try {
      const savedTheme = localStorage.getItem('theme-config')
      const savedPreset = localStorage.getItem('selected-preset-theme')

      if (savedTheme) {
        const themeConfig = JSON.parse(savedTheme)
        currentTheme.value = { ...defaultTheme, ...themeConfig }
      }

      if (savedPreset) {
        try {
          const preset = JSON.parse(savedPreset) as PresetTheme
          selectedPresetTheme.value = preset
        } catch {
          selectedPresetTheme.value = null
        }
      }

      updateDocumentVariables()
    } catch (error) {
      console.warn('Failed to load theme from localStorage:', error)
      resetTheme()
    }
  }

  // 监听系统主题变化
  const watchSystemTheme = () => {
    if (currentTheme.value.backgroundMode === 'auto') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      const handleChange = () => {
        updateDocumentVariables()
      }
      mediaQuery.addEventListener('change', handleChange)
      return () => mediaQuery.removeEventListener('change', handleChange)
    }
  }

  // 初始化主题
  const initTheme = () => {
    loadThemeFromStorage()
    watchSystemTheme()
  }

  // 监听主题变化，自动更新文档变量
  watch(
    currentTheme,
    () => {
      updateDocumentVariables()
    },
    { deep: true },
  )

  return {
    currentTheme,
    selectedPresetTheme,
    presetThemes,
    themeVariables,
    applyTheme,
    selectPresetTheme,
    resetTheme,
    initTheme,
    lightenColor,
    darkenColor,
  }
})
