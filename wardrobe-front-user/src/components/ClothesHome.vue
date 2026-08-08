<template>
  <div class="clothes-home">
    <div class="topContent">
      <el-row>
        <el-col :span="24">
          <h2 class="title">本季新品</h2>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="22">
          <el-input
              class="search-input"
              v-model="clothesName"
              placeholder="请输入服装名称">
            <template #prefix>
              <el-icon class="el-input__icon"><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="2" class="text-right">
          <el-button @click="search" type="primary" plain>搜索</el-button>
        </el-col>
      </el-row>
    </div>
    <div class="clothing-list">
      <el-row :gutter="40" class="row-spacing">
        <el-col :span="4" v-for="(clothing, index) in clothings" :key="index">
            <div class="image-container">
                <img :src="url + clothing.image" alt="Clothing Image" />
            </div>
          <div class="clothing-details">
            <h3>{{ clothing.clothName }}</h3>
            <i>{{ clothing.style }}</i>
            <p><strong>￥{{ clothing.price }}</strong></p>
            <el-button type="primary" @click="showDetails(clothing)">查看详情</el-button>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted, watch} from 'vue';
import axios from "../axios";
import {useRoute, useRouter} from "vue-router";
import {Search} from "@element-plus/icons-vue";


const clothings = ref([]) // 用于存储服装信息的响应式数组
const allClothings = ref([]) // 存储所有服装数据，用于筛选
const clothesName = ref('')

const router = useRouter()
const route = useRoute()
const url = "/api/images/"
//获取所有服装数据
const fetchClothings = () => {
  axios.get('/clothes').then(res => {
    allClothings.value = res.data
    filterClothings()
    console.log(clothings.value)
  }).catch(error => {
    alert(error.message)
  })
}
// 根据路由参数筛选服装
const filterClothings = () => {
  let filtered = [...allClothings.value]
  
  // 根据类别筛选
  if (route.query.typeId) {
    const typeId = parseInt(route.query.typeId)
    filtered = filtered.filter(item => item.typeId === typeId)
  }
  
  // 根据风格筛选
  if (route.query.style) {
    filtered = filtered.filter(item => item.style === route.query.style)
  }
  
  clothings.value = filtered
}
onMounted(() => {
  fetchClothings()
})
// 使用watch监听$route.query的变化
watch(
    () => route.query, // 监听路由的query对象
    (newVal, oldVal) => {
      // 当路由查询参数变化时，重新筛选
      if (newVal.typeId !== oldVal.typeId || newVal.style !== oldVal.style) {
        filterClothings();
      }
    },
    { immediate: false, deep: true } // immediate: false 表示不在初始化时调用，deep: true 表示深度监听对象变化
);
// 查看详情的方法
const showDetails = (clothing) => {
  router.push({
    name: 'ClothesDetail',
    params: { id: clothing.id },
  })
}
//根据名称查询服装
const search = () => {
  axios.get('/clothes/search',{
    params:{
      keyword : clothesName.value
    }
  }).then(res => {
    if(res.data != null ){
      clothings.value = res.data
    }else {
      alert("没有对应服装！")
    }
  })
}
</script>

<style scoped>

.topContent {
  display: flex;
  justify-content: space-between; /* 使内容靠右 */
  align-items: center; /* 垂直居中，如果需要的话 */
  padding: 30px; /* 添加一些内边距，以便内容不会紧贴着边缘 */
}
.title{
  margin-right: auto;

}
.search-input{
  width: 300px; /* 设置输入框的宽度 */
}

.clothing-list{
  margin-top: 20px;
  max-width: 1600px; /* 设置你希望的最大宽度 */
  margin-left: auto;
  margin-right: auto;
}

.image-container {
  width: 100%;
  height: 200px;
  overflow: hidden;
  margin-bottom: 10px;
}
.image-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.clothing-details {
  text-align: center;
}

.clothing-details button {
  margin-top: 10px;
}
button {
  background: #007bff;
  color: white;
  border: none;
  cursor :pointer;
}
p{
  color:red;
}
.image-container {
    width: 200px; /* 设定您想要的宽度 */
    height: 250px; /* 设定您想要的高度 */
    overflow: hidden; /* 隐藏超出容器的部分 */
    position: relative; /* 使得内部的img可以相对于这个容器定位 */
}

.image-container img {
    width: 100%;
    height: 100%;
    object-fit: cover; /* 保持纵横比，裁剪或填充至容器大小 */
    position: absolute; /* 绝对定位以覆盖整个容器 */
    top: 50%; /* 将图片顶部移动到容器的中心点 */
    left: 50%; /* 将图片左侧移动到容器的中心点 */
    transform: translate(-40%, -50%); /* 通过偏移来居中图片 */
}
</style>