<template>
  <div class="user-root">
    <NavBar />
    <main class="main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <Footer />
  </div>
</template>

<script setup lang="ts">
import NavBar from './NavBar.vue'
import Footer from './Footer.vue'
</script>

<style scoped>
.user-root {
  min-height: 100vh;
  display: flex; flex-direction: column;
  background: var(--app-bg);
}
.main { flex: 1; width: 100%; max-width: 1280px; margin: 0 auto; padding: 20px 24px 40px; }

.fade-enter-active, .fade-leave-active { transition: opacity .2s ease, transform .2s ease; }
.fade-enter-from { opacity: 0; transform: translateY(6px); }
.fade-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
