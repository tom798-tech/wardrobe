// =============================================================================
// 与后端实体严格对齐
// =============================================================================

export interface IdEntity { id: number }

export interface AdminUser extends IdEntity {
  userName: string
  password?: string
  phone?: string | null
  address?: string | null
  role?: number       // 0普通 / 1管理员
  token?: string | null
}

export interface Brand extends IdEntity {
  brandName: string
  brandLogo?: string | null
  description?: string | null
  createTime?: string | null
}

export interface Type extends IdEntity {
  typeName: string
}

export interface Size extends IdEntity {
  typeId?: number
  sizeName?: string
}

export interface Clothes extends IdEntity {
  clothName: string
  image?: string | null
  typeId?: number | null
  style?: string | null
  price?: number
  sizeList?: Size[]
}

export interface Review extends IdEntity {
  userId?: number
  clothId?: number
  content: string
  rating?: number
  createTime?: string | null
  userName?: string | null
  clothName?: string | null
}

export interface Cart extends IdEntity {
  clothId: number
  clothSize?: string
  amount: number
  userId: number
  date?: string
  clothes?: Clothes
}

export type OrderStatus = 0 | 1 | 2 | 3
// 0 未支付 1 未发货 2 已发货 3 已收货

export interface Order extends IdEntity {
  userId: number
  clothesDetails?: string | null
  price?: number
  status: OrderStatus
  address?: string | null
  time?: string | null
  userName?: string | null
  phone?: string | null
}

// 管理端用户管理用：与后端 User 实体对齐（与 AdminUser 字段一致，只是语义不同）
export type User = AdminUser

export interface ClothesImageUploadRes extends String {}
