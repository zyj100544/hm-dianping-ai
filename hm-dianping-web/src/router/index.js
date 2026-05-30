import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import TypeShopsView from '../views/TypeShopsView.vue'
import ShopDetailView from '../views/ShopDetailView.vue'
import BlogListView from '../views/BlogListView.vue'
import BlogDetailView from '../views/BlogDetailView.vue'
import BlogPublishView from '../views/BlogPublishView.vue'
import LoginView from '../views/LoginView.vue'
import MeView from '../views/MeView.vue'
import RecommendView from '../views/RecommendView.vue'
import SearchView from '../views/SearchView.vue'
import NotFoundView from '../views/NotFoundView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/type/:id', component: TypeShopsView },
    { path: '/shop/:id', component: ShopDetailView },
    { path: '/blog', component: BlogListView },
    { path: '/blog/publish', component: BlogPublishView },
    { path: '/blog/:id', component: BlogDetailView },
    { path: '/login', component: LoginView },
    { path: '/me', component: MeView },
    { path: '/search', component: SearchView },
    { path: '/recommend', component: RecommendView },
    { path: '/:pathMatch(.*)*', component: NotFoundView },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

export default router
