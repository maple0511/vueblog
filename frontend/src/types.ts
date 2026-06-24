export interface ApiResponse<T> { code: number; message: string; data: T }
export interface User {
  id: number
  username: string
  email: string
  role: 'USER' | 'ADMIN'
  status: 'ACTIVE' | 'DISABLED'
  profileCompleted: boolean
}
export interface Tag { id: number; name: string; source: string }
export interface Post {
  id: number
  authorId: number
  authorName: string
  title: string
  summary: string
  content: string
  aiSummary?: string
  aiMetadataStatus: 'NONE' | 'PENDING' | 'READY' | 'FAILED'
  aiSummaryEdited: boolean
  aiGeneratedAt?: string
  reviewStatus: 'APPROVED' | 'REJECTED' | 'HIDDEN'
  reviewReason?: string
  tags: Tag[]
  createdAt: string
  updatedAt: string
}
export interface PageResult<T> {
  items: T[]
  page: number
  size: number
  total: number
  totalPages: number
}
export interface Comment {
  id: number
  authorId: number
  authorName: string
  content: string
  createdAt: string
}
export interface PreferenceOptions {
  options: string[]
  selected: string[]
}
export interface AdminUser {
  id: number
  username: string
  email: string
  role: 'USER' | 'ADMIN'
  status: 'ACTIVE' | 'DISABLED'
  profileCompleted: boolean
  createdAt: string
}
