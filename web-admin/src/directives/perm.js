import { useAuthStore } from '../store/auth'

/**
 * 按钮权限指令（文档 8.2：菜单/按钮资源按当前用户权限渲染）。
 * 用法：<el-button v-perm="'admin:assessment:manage'">…</el-button>
 * 当前用户 permissions 不含该权限码时移除元素。
 */
export default {
  mounted(el, binding) {
    const auth = useAuthStore()
    const required = binding.value
    if (required && !auth.permissions.includes(required)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}
