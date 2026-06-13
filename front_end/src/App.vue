<script setup lang="ts"></script>

<template>
  <div id="app">
    <RouterView />
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background-color: var(--el-bg-color-page);
  color: var(--el-text-color-primary);
  transition:
    background-color var(--el-transition-duration),
    color var(--el-transition-duration);
}

#app {
  min-height: 100vh;
}
</style>
