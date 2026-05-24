import './assets/main.css'
import './assets/flairs.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

// Apply theme before mount so there's no flash of unstyled content
const storedTheme = localStorage.getItem('fhq-theme') ?? 'pokeball'
document.documentElement.setAttribute('data-theme', storedTheme)

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
