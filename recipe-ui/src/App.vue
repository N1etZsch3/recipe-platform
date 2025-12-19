<script setup>
import { RouterView } from 'vue-router'
import Navbar from './components/Navbar.vue'
import Toast from './components/Toast.vue'
import Modal from './components/Modal.vue'
import { ref, provide } from 'vue'

const modalRef = ref(null)

const confirm = (message) => {
  if (modalRef.value) {
    return modalRef.value.show(message)
  }
  return Promise.resolve(false)
}

provide('confirm', confirm)
</script>

<template>
  <div class="min-h-screen bg-[#FFFBF5] text-gray-800 font-sans flex flex-col">
    <Navbar />
    <div class="flex-grow">
      <RouterView />
    </div>
    <Toast />
    <Modal ref="modalRef" />
  </div>
</template>
