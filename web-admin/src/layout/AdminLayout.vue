<template>
  <el-container class="admin-layout">
    <!-- 侧边栏：菜单由 RBAC 返回的 menu:admin:* 资源驱动（文档 8.2） -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <div class="logo-mark">
          <el-icon :size="20"><FirstAidKit /></el-icon>
        </div>
        <div v-if="!isCollapse" class="logo-text">
          <div class="logo-name">养老社区管理后台</div>
        </div>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          class="menu"
        >
          <template v-for="item in adminMenus" :key="item.resourceCode">
            <el-sub-menu v-if="item.children && item.children.length" :index="item.resourceCode">
              <template #title>
                <el-icon><component :is="iconOf(item.resourceCode)" /></el-icon>
                <span>{{ item.resourceName }}</span>
              </template>
              <el-menu-item
                v-for="child in item.children"
                :key="child.resourceCode"
                :index="routeOf(child.resourceCode)"
              >
                <el-icon><component :is="iconOf(child.resourceCode)" /></el-icon>
                <span>{{ child.resourceName }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="routeOf(item.resourceCode)">
              <el-icon><component :is="iconOf(item.resourceCode)" /></el-icon>
              <span>{{ item.resourceName }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="collapse-btn" @click="isCollapse = !isCollapse">
            <el-icon :size="18">
              <component :is="isCollapse ? 'Expand' : 'Fold'" />
            </el-icon>
          </div>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tooltip content="消息中心" placement="bottom">
            <div class="header-action" @click="router.push('/message')">
              <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
                <el-icon :size="18"><Bell /></el-icon>
              </el-badge>
            </div>
          </el-tooltip>

          <el-dropdown @command="handleCommand">
            <span class="user">
              <el-avatar :size="32" class="user-avatar">{{ avatarText }}</el-avatar>
              <span class="user-name">{{ userInfo?.realName || userInfo?.phone }}</span>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  {{ userInfo?.realName || userInfo?.phone }}
                </el-dropdown-item>
                <el-dropdown-item command="password" divided>修改密码</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 修改密码（接口文档 1.7，改密后强制下线需重新登录） -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="420px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="submitPassword">确认</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { FirstAidKit, Bell, ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'
import { changePassword } from '../api/auth'

// menu:admin:* 资源编码 → 前端路由路径映射
const MENU_ROUTE_MAP = {
  'menu:admin:dashboard': '/dashboard',
  'menu:admin:health': '/health',
  'menu:admin:assessment': '/assessment',
  'menu:admin:appointment': '/appointment',
  'menu:admin:activity': '/activity',
  'menu:admin:member': '/member',
  'menu:admin:message': '/message',
  'menu:admin:config': '/config',
  'menu:admin:rbac:role': '/rbac/role',
  'menu:admin:rbac:permission': '/rbac/permission',
  'menu:admin:rbac:resource': '/rbac/resource'
}

// menu:admin:* 资源编码 → 菜单图标映射（图标已全局注册）
const MENU_ICON_MAP = {
  'menu:admin:dashboard': 'Odometer',
  'menu:admin:health': 'FirstAidKit',
  'menu:admin:assessment': 'Document',
  'menu:admin:appointment': 'Calendar',
  'menu:admin:activity': 'Flag',
  'menu:admin:member': 'User',
  'menu:admin:message': 'ChatDotRound',
  'menu:admin:config': 'Setting',
  'menu:admin:rbac': 'Lock',
  'menu:admin:rbac:role': 'UserFilled',
  'menu:admin:rbac:permission': 'Key',
  'menu:admin:rbac:resource': 'Files'
}

const routeOf = (code) => MENU_ROUTE_MAP[code] || '/'
const iconOf = (code) => MENU_ICON_MAP[code] || 'Document'

/** 过滤出管理端菜单（资源编码以 menu:admin: 开头），保留树形 */
function filterAdminMenus(nodes) {
  return (nodes || [])
    .filter((n) => n.resourceCode && n.resourceCode.startsWith('menu:admin:'))
    .map((n) => ({ ...n, children: filterAdminMenus(n.children) }))
}

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const userInfo = computed(() => auth.userInfo)
const adminMenus = computed(() => filterAdminMenus(auth.menus))
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '管理端')
const avatarText = computed(() => {
  const name = userInfo.value?.realName || userInfo.value?.phone || '管'
  return name.charAt(0)
})

// ===== 侧栏折叠 =====
const isCollapse = ref(false)

// ===== 消息角标（TODO: 待后端未读消息接口提供后替换）=====
const unreadCount = ref(3)

// ===== 修改密码 =====
const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({ oldPassword: '', newPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '新密码至少 8 位', trigger: 'blur' }
  ]
}

async function submitPassword() {
  await pwdFormRef.value.validate()
  pwdLoading.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('修改成功，请重新登录')
    auth.clear()
    router.replace('/login')
  } catch (e) {
    // 错误由拦截器提示
  } finally {
    pwdLoading.value = false
    pwdVisible.value = false
  }
}

async function handleCommand(command) {
  if (command === 'password') {
    pwdVisible.value = true
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
  } else if (command === 'logout') {
    await auth.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100%;
}

/* ===== 侧边栏 ===== */
.aside {
  display: flex;
  flex-direction: column;
  background: var(--sidebar-bg);
  transition: width 0.25s ease;
  overflow: hidden;
}
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 60px;
  padding: 0 16px;
  flex: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.logo-mark {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--ec-radius-md);
  color: #fff;
  background: var(--ec-primary);
}
.logo-name {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  line-height: 1.2;
  white-space: nowrap;
}
.menu-scroll {
  flex: 1;
}
/* 菜单主题样式（背景/文字/hover/active）统一维护在 styles/sidebar.css，
   此处仅保留布局相关 */
.menu {
  border-right: none;
  padding: 8px;
}
.menu:not(.el-menu--collapse) {
  width: 204px;
}

/* ===== Header ===== */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid var(--ec-border);
}
.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--ec-text-regular);
  transition: background 0.2s, color 0.2s;
}
.collapse-btn:hover {
  background: var(--ec-bg-page);
  color: var(--ec-primary);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 6px;
}
.header-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--ec-text-regular);
  transition: background 0.2s, color 0.2s;
}
.header-action:hover {
  background: var(--ec-bg-page);
  color: var(--ec-primary);
}
.user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-left: 8px;
  padding: 4px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}
.user:hover {
  background: var(--ec-bg-page);
}
.user-avatar {
  color: #fff;
  font-weight: 600;
  background: var(--ec-primary);
}
.user-name {
  font-size: 14px;
  color: var(--ec-text-primary);
}

/* ===== 内容区 ===== */
.main {
  background: var(--ec-bg-page);
  padding: 20px;
}
</style>
