<template>
  <div class="clothing-container">
    <el-row>
      <el-col :span="12" class="image-col">
        <div class="image-wrapper">
          <img
              :src="url + clothing.image"
              alt="Clothing Image"
              class="clothing-image"
          />
        </div>
      </el-col>
      <el-col :span="12" class="detail-col">
        <div class="detail-wrapper">
          <h1 class="clothing-name">{{ clothing.clothName }}</h1>
          <p class="clothing-style">风格：<strong>{{ clothing.style }}</strong></p>
          <p class="clothing-category">类别：<strong>{{ clothing.typeName }}</strong></p>
          <el-form-item label="尺码:" prop="size" class="clothing-size">
            <el-radio-group v-model="selectedSize">
              <el-radio-button
                  v-for="size in clothing.sizeList"
                  :label="size"
                  :key="size.id"
              >
                {{ size.sizeName }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <h2 class="clothing-price">￥{{ clothing.price }}</h2>
          <el-button type="primary" @click="addToCart">加入购物车</el-button>
        </div>
      </el-col>
    </el-row>

    <!-- 评论区域 -->
    <el-divider />
    <div class="review-section">
      <h3>商品评价（{{ reviewList.length }}条）</h3>
      <p v-if="avgRating > 0">平均评分：<el-rate v-model="avgRating" disabled show-score text-color="#ff9900" style="display: inline-flex"/></p>

      <!-- 添加评论 -->
      <div class="add-review" v-if="userId">
        <el-divider />
        <h4>发表评价</h4>
        <el-form :model="reviewForm" label-width="80px">
          <el-form-item label="评分">
            <el-rate v-model="reviewForm.rating" show-text/>
          </el-form-item>
          <el-form-item label="评价内容">
            <el-input v-model="reviewForm.content" type="textarea" :rows="3" placeholder="请输入您的评价"/>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitReview">提交评价</el-button>
          </el-form-item>
        </el-form>
      </div>
      <div class="add-review" v-else>
        <p style="color: #999">请先<a href="#/login">登录</a>后发表评价</p>
      </div>

      <!-- 评论列表 -->
      <el-divider />
      <div class="review-list">
        <div v-for="review in reviewList" :key="review.id" class="review-item">
          <div class="review-header">
            <span class="review-user">{{ review.userName }}</span>
            <el-rate v-model="review.rating" disabled style="display: inline-flex; margin-left: 10px"/>
            <span class="review-time">{{ review.createTime }}</span>
          </div>
          <div class="review-content">{{ review.content }}</div>
          <el-divider />
        </div>
        <p v-if="reviewList.length === 0" style="color: #999; text-align: center">暂无评价</p>
      </div>
    </div>
  </div>
</template>



<script setup>
import {onMounted, reactive, ref} from 'vue';
import {useRoute} from "vue-router";
import axios from "../axios";
import {ElMessage} from "element-plus";

const route = useRoute()
//图片路径
const url = "/api/images/"
//添加购物车时需要选择的尺码参数
let clothing = ref({})
const selectedSize = ref('');
onMounted(() => {
  getClothesInfo()
  getReviews()
})
const getClothesInfo = () =>{
  axios.get('/clothes/' + route.query.clothId).then(res => {
    clothing.value = res.data
  }).catch(error =>{
    console.log(error)
  })
}

// ----- 评论相关 -----
const reviewList = ref([])
const avgRating = ref(0)
const userId = ref(JSON.parse(localStorage.getItem('user'))?.id)

const getReviews = () => {
  axios.get('/review/cloth/' + route.query.clothId).then(res => {
    reviewList.value = res.data
    if(res.data.length > 0){
      let sum = 0
      res.data.forEach(r => sum += r.rating)
      avgRating.value = (sum / res.data.length).toFixed(1)
    }
  }).catch(error => { console.log(error) })
}

const reviewForm = reactive({ rating: 5, content: '' })
const submitReview = () => {
  if(!reviewForm.content.trim()){
    ElMessage({ type: 'warning', message: '请输入评价内容' })
    return
  }
  const now = new Date()
  const timeStr = now.getFullYear() + '-' +
    String(now.getMonth()+1).padStart(2,'0') + '-' +
    String(now.getDate()).padStart(2,'0') + ' ' +
    String(now.getHours()).padStart(2,'0') + ':' +
    String(now.getMinutes()).padStart(2,'0') + ':' +
    String(now.getSeconds()).padStart(2,'0')
  axios.post('/review', {
    userId: userId.value,
    clothId: parseInt(route.query.clothId),
    content: reviewForm.content,
    rating: reviewForm.rating,
    createTime: timeStr
  }).then(res => {
    ElMessage({ type: 'success', message: res.data })
    reviewForm.content = ''
    reviewForm.rating = 5
    getReviews()
  }).catch(error => { console.log(error) })
}

//添加购物车
const addToCart = () => {
  if (!selectedSize.value) {
    alert('请选择尺码！');
    return;
  }
  if(userId.value == null){
    alert("请先登录！")
  }else{
    axios.post('/cart',{
      clothId : clothing.value.id,
      clothSize : selectedSize.value.sizeName,
      userId : userId.value
    }).then(res => {
      alert(res.data)
    }).catch(error => {
      console.log(error)
    })
  }
}
</script>


<style scoped>
.clothing-container {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  padding: 30px 50px;
}

.image-col, .detail-col {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.image-wrapper {
  text-align: center;
}

.clothing-image {
  max-width: 80%;
  height: auto;
  display: block;
  object-fit: cover;
}
h2{ color: red; }
h1,p{ color: dimgray; }

/* 评论区域 */
.review-section {
  margin-top: 20px;
}
.add-review {
  margin: 15px 0;
}
.review-item {
  margin-bottom: 10px;
}
.review-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
.review-user {
  font-weight: bold;
  color: #333;
}
.review-time {
  margin-left: auto;
  color: #999;
  font-size: 13px;
}
.review-content {
  color: #555;
  line-height: 1.6;
  padding-left: 5px;
}
</style>