<template>
  <div class="theme-toggle">
    <el-dropdown trigger="click" @command="handleThemeChange">
      <el-button type="primary" :icon="Setting" circle />
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item
            v-for="theme in presetThemes"
            :key="theme.id"
            :command="theme"
            :class="{ active: currentTheme?.id === theme.id }"
          >
            <div class="theme-item">
              <div class="theme-preview" :style="getThemePreviewStyle(theme)"></div>
              <div class="theme-info">
                <div class="theme-name">{{ theme.name }}</div>
                <div class="theme-desc">{{ theme.description }}</div>
              </div>
            </div>
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Setting } from '@element-plus/icons-vue'
import { useThemeStore } from '@/stores/theme'
import type { PresetTheme } from '@/stores/theme'

const themeStore = useThemeStore()

const presetThemes = computed(() => themeStore.presetThemes)
const currentTheme = computed(() => themeStore.selectedPresetTheme)

const handleThemeChange = (theme: PresetTheme) => {
  themeStore.selectPresetTheme(theme)
}

const getThemePreviewStyle = (theme: PresetTheme) => {
  const isDark = theme.config.backgroundMode === 'dark'
  return {
    backgroundColor: theme.config.primaryColor,
    border: `2px solid ${isDark ? '#ffffff' : '#000000'}`,
    borderRadius: theme.config.borderRadius + 'px',
  }
}
</script>

<style scoped>
.theme-toggle {
  display: inline-block;
}

.theme-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 0;
}

.theme-preview {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  flex-shrink: 0;
}

.theme-info {
  flex: 1;
}

.theme-name {
  font-weight: 500;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.theme-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.el-dropdown-item.active {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.el-dropdown-item.active .theme-name {
  color: var(--el-color-primary);
}
</style>
