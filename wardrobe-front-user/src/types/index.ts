// =============================================================================
// 全局类型定义：与后端实体严格对齐
// =============================================================================

export interface IdEntity {
  id: number
}

// --- 用户 ---------------------------------------------------------------
export interface User extends IdEntity {
  userName: string
  password?: string
  phone?: string | null
  address?: string | null
  role?: number           // 0=普通用户 1=管理员
  token?: string | null   // 后端临时字段
}

export interface UserVo extends Partial<User> {
  newpsw?: string
}

export interface LoginRole {
  role: 'user' | 'admin'
}

// --- 分类/品牌/尺码 ----------------------------------------------------
export interface Brand extends IdEntity {
  brandName: string
  logo?: string | null
  intro?: string | null
}

// 后端实际字段叫 Type (非 Category)
export interface Type extends IdEntity {
  typeName: string
  intro?: string | null
}

export interface Size extends IdEntity {
  typeId?: number
  sizeName?: string
  sizeValue?: string
  intro?: string | null
}

// --- 商品 ---------------------------------------------------------------
export interface Clothes extends IdEntity {
  typeId?: number | null
  brandId?: number | null
  clothName: string
  images?: string | null          // 分号分隔的多张图
  image?: string | null           // 封面图
  price?: number
  stock?: number
  description?: string | null
  style?: string | null
  sales?: number
  // 关联：后端 findById 会返回
  sizeList?: Size[]
}

// --- 购物车 -------------------------------------------------------------
export interface Cart extends IdEntity {
  userId: number
  clothId: number
  clothSize?: string
  amount: number
  clothes?: Clothes | null    // 后端 findByUserId 会回查填进来
}

// --- 订单 ---------------------------------------------------------------
export type OrderStatus = 0 | 1 | 2 | 3
// 0 待支付 1 待发货 2 待收货 3 已完成

export interface Order extends IdEntity {
  userId: number
  clothesDetails?: string | null     // JSON 字符串：商品明细数组
  price?: number
  status: OrderStatus
  address?: string | null
  time?: string | null
  userName?: string | null           // 后端可选关联字段
  phone?: string | null              // 后端可选关联字段
}

// --- 评论 ---------------------------------------------------------------
export interface Review extends IdEntity {
  userId?: number
  clothId?: number
  content: string
  rating?: number
  reviewTime?: string | null
  images?: string | null
  userName?: string
  clothName?: string
}

// --- 向量检索响应 -------------------------------------------------------
export interface VectorSearchResult {
  clothId: number
  clothName: string
  typeId?: number | null
  brandId?: number | null
  price?: number
  style?: string | null
  stock?: number
  sales?: number
  images?: string | null
  image?: string | null
  description?: string | null
}

export interface VectorStatus {
  embeddingModelAvailable: boolean
  vectorStoreAvailable: boolean
}

// --- 通用响应 -----------------------------------------------------------
export interface ResultEnvelope<T = unknown> {
  code?: number | string
  message?: string
  data?: T
  msg?: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum?: number
  pageSize?: number
}
