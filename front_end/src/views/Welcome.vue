<template>
  <div class="welcome-page">
    <header class="topbar">
      <div class="topbar-inner">
        <router-link to="/welcome" class="brand">
          <span class="brand-mark">
            <el-icon><Cloudy /></el-icon>
          </span>
          <span>气象数据管理与分析系统</span>
        </router-link>

        <nav class="nav-links">
          <a href="#modules">功能模块</a>
          <a href="#workflow">数据流程</a>
          <a href="#tech">技术说明</a>
        </nav>

        <div class="topbar-actions">
          <router-link to="/login" class="link-button">登录</router-link>
          <router-link to="/register" class="primary-button">注册</router-link>
        </div>
      </div>
    </header>

    <main>
      <section class="hero-section">
        <div class="hero-copy">
          <p class="eyebrow">Weather Data Workspace</p>
          <h1>面向个人使用的气象数据采集、分析与预测工作台</h1>
          <p class="hero-desc">
            系统围绕气象站点数据的采集任务、统计分析、GIS 可视化和趋势预测组织页面，
            登录后可按模块完成数据处理和结果查看。
          </p>
          <div class="hero-actions">
            <router-link to="/login" class="primary-button large">进入系统</router-link>
            <router-link to="/register" class="secondary-button large">创建账户</router-link>
          </div>
        </div>

        <div class="system-panel" aria-label="系统模块概览">
          <div class="panel-header">
            <span class="status-dot"></span>
            <span>模块状态</span>
          </div>
          <div class="panel-grid">
            <div v-for="item in panelItems" :key="item.label" class="panel-item">
              <el-icon><component :is="item.icon" /></el-icon>
              <div>
                <strong>{{ item.label }}</strong>
                <span>{{ item.desc }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id="modules" class="section-block">
        <div class="section-heading">
          <p class="eyebrow">Modules</p>
          <h2>核心功能模块</h2>
          <span>保留数据系统需要的操作入口，减少无关展示内容。</span>
        </div>

        <div class="module-grid">
          <article v-for="module in modules" :key="module.title" class="module-card">
            <div class="module-icon">
              <el-icon><component :is="module.icon" /></el-icon>
            </div>
            <h3>{{ module.title }}</h3>
            <p>{{ module.desc }}</p>
            <ul>
              <li v-for="point in module.points" :key="point">{{ point }}</li>
            </ul>
          </article>
        </div>
      </section>

      <section id="workflow" class="section-block muted">
        <div class="section-heading">
          <p class="eyebrow">Workflow</p>
          <h2>数据处理流程</h2>
          <span>页面按实际使用顺序组织，便于定位问题和查看结果。</span>
        </div>

        <div class="workflow">
          <div v-for="(step, index) in workflowSteps" :key="step.title" class="workflow-step">
            <span class="step-index">{{ String(index + 1).padStart(2, '0') }}</span>
            <h3>{{ step.title }}</h3>
            <p>{{ step.desc }}</p>
          </div>
        </div>
      </section>

      <section id="tech" class="section-block">
        <div class="tech-layout">
          <div class="section-heading align-left">
            <p class="eyebrow">Stack</p>
            <h2>技术与页面入口</h2>
            <span>前端使用 Vue 3、Element Plus 和 ECharts，后端接口保持现有调用方式。</span>
          </div>

          <div class="tech-list">
            <div v-for="item in techItems" :key="item.name" class="tech-item">
              <span>{{ item.name }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>

          <div class="entry-card">
            <h3>开始使用</h3>
            <p>已有账号可直接登录；首次使用可先注册普通用户账号。</p>
            <div class="entry-actions">
              <router-link to="/login" class="primary-button">登录</router-link>
              <router-link to="/register" class="secondary-button">注册</router-link>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import {
  Cloudy,
  DataAnalysis,
  DataLine,
  Location,
  Setting,
  TrendCharts,
  User,
} from '@element-plus/icons-vue'

const panelItems = [
  { label: '采集任务', desc: '站点与要素配置', icon: DataLine },
  { label: '统计分析', desc: '时间序列与相关性', icon: DataAnalysis },
  { label: 'GIS 分析', desc: '地图与空间分布', icon: Location },
  { label: '趋势预测', desc: '历史数据预测', icon: TrendCharts },
]

const modules = [
  {
    title: '用户与权限',
    desc: '完成账号登录、资料维护和普通用户/管理员分流。',
    icon: User,
    points: ['注册登录', '个人资料', '角色权限'],
  },
  {
    title: '数据采集',
    desc: '配置气象站点、时间范围和气象要素，查看任务执行状态。',
    icon: DataLine,
    points: ['采集任务创建', '任务列表', '执行状态统计'],
  },
  {
    title: '统计分析',
    desc: '对已采集数据进行时间序列、相关性和空间分布分析。',
    icon: DataAnalysis,
    points: ['时间序列图表', '相关性热力图', '数据查询表'],
  },
  {
    title: 'GIS 可视化',
    desc: '在地图中查看站点位置、要素分布和最近站点数据。',
    icon: Location,
    points: ['站点分布', '热力图', '点选查询'],
  },
  {
    title: '趋势预测',
    desc: '基于历史观测数据运行预测算法并展示预测结果。',
    icon: TrendCharts,
    points: ['算法配置', '评估指标', '预测曲线'],
  },
  {
    title: '系统设置',
    desc: '维护界面偏好和默认分析参数，减少重复配置。',
    icon: Setting,
    points: ['外观设置', '默认要素', '快捷入口'],
  },
]

const workflowSteps = [
  { title: '创建采集任务', desc: '选择站点、气象要素和时间范围。' },
  { title: '检查数据结果', desc: '在任务列表和数据表中核对采集结果。' },
  { title: '分析与可视化', desc: '使用图表和地图查看时间、空间特征。' },
  { title: '运行趋势预测', desc: '选择算法并查看预测曲线和误差指标。' },
]

const techItems = [
  { name: '前端框架', value: 'Vue 3 + TypeScript' },
  { name: '界面组件', value: 'Element Plus' },
  { name: '图表展示', value: 'ECharts' },
  { name: '地图可视化', value: '百度地图 GL / MapVGL' },
  { name: '系统接口', value: 'Spring Boot REST API' },
  { name: '数据存储', value: 'PostgreSQL / Redis / HDFS' },
]
</script>

<style scoped>
.welcome-page {
  min-height: 100vh;
  background: #f4f7fb;
  color: #1f2d3d;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #d9e2ec;
}

.topbar-inner {
  max-width: 1180px;
  height: 64px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #102a43;
  text-decoration: none;
  font-weight: 700;
  font-size: 17px;
}

.brand-mark,
.module-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #1d4f91;
  background: #e4eef8;
  border: 1px solid #c7d8ea;
}

.nav-links {
  display: flex;
  gap: 24px;
  font-size: 14px;
}

.nav-links a {
  color: #52616f;
  text-decoration: none;
}

.nav-links a:hover {
  color: #1d4f91;
}

.topbar-actions,
.hero-actions,
.entry-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.primary-button,
.secondary-button,
.link-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 16px;
  border-radius: 6px;
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
}

.primary-button {
  color: #fff;
  background: #1d4f91;
  border: 1px solid #1d4f91;
}

.secondary-button,
.link-button {
  color: #1d4f91;
  background: #fff;
  border: 1px solid #c7d8ea;
}

.large {
  min-height: 44px;
  padding: 0 22px;
}

.hero-section {
  max-width: 1180px;
  margin: 0 auto;
  padding: 72px 24px 56px;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(420px, 0.95fr);
  gap: 44px;
  align-items: center;
}

.eyebrow {
  margin: 0 0 12px;
  color: #2f80c2;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 0;
  color: #102a43;
  font-size: 40px;
  line-height: 1.2;
  font-weight: 750;
}

.hero-desc {
  max-width: 620px;
  margin: 20px 0 28px;
  color: #52616f;
  font-size: 16px;
  line-height: 1.8;
}

.system-panel {
  background: #fff;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  box-shadow: 0 12px 30px rgba(25, 55, 90, 0.08);
  overflow: hidden;
}

.panel-header {
  height: 52px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #e6edf5;
  color: #334e68;
  font-weight: 700;
}

.status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #2f855a;
}

.panel-grid {
  padding: 18px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.panel-item {
  min-height: 96px;
  padding: 14px;
  border: 1px solid #e6edf5;
  border-radius: 8px;
  background: #f8fbff;
  display: flex;
  gap: 12px;
}

.panel-item .el-icon {
  color: #1d4f91;
  font-size: 22px;
  margin-top: 2px;
}

.panel-item strong,
.panel-item span {
  display: block;
}

.panel-item strong {
  margin-bottom: 6px;
  color: #102a43;
}

.panel-item span {
  color: #627d98;
  font-size: 13px;
}

.section-block {
  padding: 64px 24px;
  background: #fff;
}

.section-block.muted {
  background: #eef4f9;
}

.section-heading {
  max-width: 720px;
  margin: 0 auto 34px;
  text-align: center;
}

.section-heading.align-left {
  margin: 0;
  text-align: left;
}

.section-heading h2 {
  margin: 0 0 10px;
  color: #102a43;
  font-size: 28px;
}

.section-heading span {
  color: #627d98;
  line-height: 1.7;
}

.module-grid,
.workflow,
.tech-layout {
  max-width: 1180px;
  margin: 0 auto;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.module-card,
.workflow-step,
.entry-card {
  background: #fff;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
}

.module-card {
  padding: 22px;
}

.module-card h3 {
  margin: 18px 0 8px;
  color: #102a43;
  font-size: 18px;
}

.module-card p,
.workflow-step p,
.entry-card p {
  margin: 0;
  color: #627d98;
  line-height: 1.7;
}

.module-card ul {
  margin: 18px 0 0;
  padding: 0;
  list-style: none;
  color: #334e68;
}

.module-card li {
  padding: 6px 0;
  border-top: 1px solid #edf2f7;
  font-size: 14px;
}

.workflow {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.workflow-step {
  padding: 22px;
}

.step-index {
  color: #1d4f91;
  font-weight: 800;
  font-size: 13px;
}

.workflow-step h3 {
  margin: 12px 0 8px;
  color: #102a43;
  font-size: 17px;
}

.tech-layout {
  display: grid;
  grid-template-columns: 1fr 1.1fr 0.85fr;
  gap: 24px;
  align-items: start;
}

.tech-list {
  display: grid;
  gap: 10px;
}

.tech-item {
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  background: #f8fbff;
}

.tech-item span {
  color: #627d98;
}

.tech-item strong {
  color: #102a43;
  text-align: right;
}

.entry-card {
  padding: 22px;
  background: #f8fbff;
}

.entry-card h3 {
  margin: 0 0 10px;
  color: #102a43;
}

.entry-actions {
  margin-top: 20px;
}

@media (max-width: 980px) {
  .hero-section,
  .tech-layout {
    grid-template-columns: 1fr;
  }

  .system-panel {
    max-width: 620px;
  }

  .module-grid,
  .workflow {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .topbar-inner {
    height: auto;
    padding: 14px 18px;
    flex-wrap: wrap;
  }

  .nav-links {
    order: 3;
    width: 100%;
    gap: 16px;
  }

  .hero-section {
    padding-top: 44px;
  }

  .hero-copy h1 {
    font-size: 30px;
  }

  .panel-grid,
  .module-grid,
  .workflow {
    grid-template-columns: 1fr;
  }
}
</style>
