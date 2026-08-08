<template>
    <div>
        <el-card class="box-card" style="max-width: 550px; margin: 0 auto; ">
            <template #header>
                <div class="card-header">
                    <h2>个人信息</h2>
                </div>
            </template>
            <el-form :model="personInfo"
                     style="max-width: 500px;"
                     label-width="auto"
                     :rules="rules"
                     ref="ruleFormRef"
                     status-icon>
                <el-form-item label="姓名:" prop="userName">
                    <el-input v-model="personInfo.userName" />
                </el-form-item>
              <el-form-item label="原始密码:" prop="password">
                <el-input v-model="personInfo.password" type="password" show-password placeholder="请输入原始密码"/>
              </el-form-item>
                <el-form-item label="新密码:" prop="newpsw">
                    <el-input v-model="personInfo.newpsw" type="password" show-password placeholder="请输入新密码"/>
                </el-form-item>
                <el-form-item label="手机号:" prop="phone">
                    <el-input v-model="personInfo.phone" />
                </el-form-item>
                <el-form-item label="地址:" prop="address">
                    <el-input v-model="personInfo.address" />
                </el-form-item>
<!--                <el-form-item label="角色:">
                {{ roleDisplayName }}
                </el-form-item>-->
                <div style="display:flex; justify-content: right">
                    <el-form-item>
                        <el-button type="primary" @click="onSubmit" >更新</el-button>
                    </el-form-item>
                </div>
            </el-form>

        </el-card>
    </div>
</template>

<script setup>
import {onMounted, reactive, ref} from 'vue';
/*import {computed, onMounted, reactive, ref} from 'vue';*/
import {useRoute} from "vue-router";
import axios from "../axios";
const route = useRoute()

/*const roleDisplayName = computed(() => {
    switch (personInfo.value.role) {
        case 1:
            return '管理员';
        case 2:
            return '普通用户';
        default:
            return '未知角色';
    }
})*/

const personInfo = ref({
    userName: '',
    password: '',
    newpsw: '',
    phone: '',
    address: '',
    role: '',
})
const ruleFormRef = ref(null);
const rules = reactive({
    userName: [
        { required: true, message: '请输入用户姓名', trigger: 'blur' },
        { min: 2, max: 10, message: '用户姓名长度在 2 到 10 个字符', trigger: 'blur' }
    ],
  password: [
    { required: true, message: '请输入原始密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码长度在 6 到 16 个字符', trigger: 'blur' }
  ],
  newpsw: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码长度在 6 到 16 个字符', trigger: 'blur' }
  ],
    phone: [
        { required: true, message: '请输入电话号码', trigger: 'blur' },
        { pattern: /^1[34578]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
    ],
    address: [
        { required: true, message: '请输入地址', trigger: 'blur' }
    ]
})
onMounted(() => {
    getPersonInfo()
})
const getPersonInfo = () =>{
    axios.get('/user/' + route.query.userId).then(res => {
        personInfo.value = res.data
    }).catch(error =>{
        console.log(error)
    })
}
const onSubmit = () => {
    const data = {
        id: personInfo.value.id,
        userName: personInfo.value.userName,
        password: personInfo.value.password,
        newpsw: personInfo.value.newpsw,
        phone: personInfo.value.phone,
        address: personInfo.value.address,
    }
    // 表单数据校验
    if (!ruleFormRef.value) return
    ruleFormRef.value.validate((valid, fields) => {
        if (valid) {
            axios.put('/user',data).then(res => {
                alert(res.data)
            })
        } else {
            console.log('表单校验未通过!', fields)
        }
    })
}
</script>

<style scoped>
.person {
  height: 500px;
  background-color: #eaeaea;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  line-height: 178px;
  text-align: center;
}

.avatar {
  width: 178px;
  height: 178px;
  display: block;
}

</style>
