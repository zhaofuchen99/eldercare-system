<template>
  <div class="config-page">
    <el-card shadow="never">
      <el-alert
        type="info"
        :closable="false"
        title="系统全局参数，修改后立即生效（积分赠送类配置在下次获得积分时读取）"
        class="config-alert"
      />
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="configKey" label="配置键" min-width="200" />
        <el-table-column prop="configValue" label="配置值" min-width="120" />
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="更新时间" width="165">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="100">
          <template #default="{ row }">
            <el-button v-perm="'admin:config:manage'" link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑配置值 -->
    <el-dialog v-model="editVisible" title="编辑配置" width="480px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="配置键">
          <el-input :model-value="editForm.configKey" disabled />
        </el-form-item>
        <el-form-item label="说明">
          <el-input :model-value="editForm.description" disabled />
        </el-form-item>
        <el-form-item label="配置值" required>
          <el-input v-model="editForm.configValue" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigList, updateConfig } from '../../api/config'
import { formatDateTime } from '../../utils/format'

const loading = ref(false)
const list = ref([])

const editVisible = ref(false)
const submitting = ref(false)
const editForm = reactive({ id: null, configKey: '', configValue: '', description: '' })

async function load() {
  loading.value = true
  try {
    list.value = await getConfigList()
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  Object.assign(editForm, {
    id: row.id,
    configKey: row.configKey,
    configValue: row.configValue,
    description: row.description
  })
  editVisible.value = true
}

async function submitEdit() {
  if (!editForm.configValue.trim()) return ElMessage.warning('请输入配置值')
  submitting.value = true
  try {
    await updateConfig(editForm.configKey, editForm.configValue)
    ElMessage.success('配置已更新，立即生效')
    editVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.config-alert {
  margin-bottom: 14px;
}
</style>
