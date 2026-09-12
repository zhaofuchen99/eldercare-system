<template>
  <div class="profile-page">
    <van-nav-bar title="个人信息" left-arrow @click-left="$router.back()" fixed placeholder safe-area-inset-top />

    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-cell title="手机号" :value="form.phone" label="手机号不可修改" />
        <van-field v-model="form.realName" label="姓名" placeholder="请输入真实姓名" maxlength="50" />
        <van-field label="性别">
          <template #input>
            <van-radio-group v-model="form.gender" direction="horizontal">
              <van-radio name="男">男</van-radio>
              <van-radio name="女">女</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field label="出生日期" is-link readonly :model-value="form.birthDate || ''" placeholder="请选择" @click="showDate = true" />
        <van-field
          v-model="form.height"
          type="number"
          label="身高"
          placeholder="50-250 cm"
          :rules="[{ validator: heightRule, message: '身高需在 50-250 cm' }]"
        />
        <van-field v-model="form.emergencyContact" label="紧急联系人" placeholder="选填" maxlength="20" />
        <van-field v-model="form.avatar" label="头像URL" placeholder="https://..." maxlength="500" />
      </van-cell-group>

      <div class="save-wrap">
        <van-button round block type="primary" native-type="submit" :loading="submitting">保存</van-button>
      </div>
    </van-form>

    <van-popup v-model:show="showDate" position="bottom" round>
      <van-date-picker
        v-model="birthPicker"
        :min-date="minDate"
        :max-date="maxDate"
        @confirm="onConfirmDate"
        @cancel="showDate = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { showSuccessToast } from 'vant'
import { getProfile, updateProfile } from '../../api/auth'
import { useAuthStore } from '../../store/auth'

const auth = useAuthStore()
const submitting = ref(false)
const showDate = ref(false)
const form = reactive({
  phone: '',
  realName: '',
  gender: '',
  birthDate: '',
  height: '',
  emergencyContact: '',
  avatar: ''
})
const birthPicker = ref([])
const minDate = new Date(1930, 0, 1)
const maxDate = new Date()

function heightRule(val) {
  if (!val) return true
  const n = Number(val)
  return !Number.isNaN(n) && n >= 50 && n <= 250
}

function onConfirmDate({ selectedValues }) {
  form.birthDate = selectedValues.join('-')
  showDate.value = false
}

async function onSubmit() {
  submitting.value = true
  try {
    const payload = {}
    if (form.realName) payload.realName = form.realName
    if (form.gender) payload.gender = form.gender
    if (form.birthDate) payload.birthDate = form.birthDate
    if (form.height !== '') payload.height = Number(form.height)
    if (form.emergencyContact) payload.emergencyContact = form.emergencyContact
    if (form.avatar) payload.avatar = form.avatar
    await updateProfile(payload)
    showSuccessToast('保存成功')
    // 同步本地 store 的姓名/头像
    const profile = await getProfile()
    auth.setUserInfo(profile)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const p = await getProfile()
    Object.assign(form, {
      phone: p.phone || '',
      realName: p.realName || '',
      gender: p.gender || '',
      birthDate: p.birthDate || '',
      height: p.height ?? '',
      emergencyContact: p.emergencyContact || '',
      avatar: p.avatar || ''
    })
    if (p.birthDate) {
      birthPicker.value = p.birthDate.split('-').map((x) => Number(x))
    }
  } catch {
    /* 拦截器已提示 */
  }
})
</script>

<style scoped>
.profile-page {
  background: var(--page-bg);
  min-height: 100vh;
}
.profile-page :deep(.van-cell-group--inset) {
  margin-top: 14px;
  box-shadow: var(--card-shadow);
}
.save-wrap {
  margin: 24px 20px;
}
.save-wrap :deep(.van-button) {
  height: 52px;
  font-size: 18px;
  font-weight: 600;
}
</style>
