import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

// 创建axios实例
const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
// service.interceptors.response.use(
//   response => {
//     const res = response.data
//
//     // 如果返回的状态码不是200，说明接口有问题，应该提示错误
//     if (res.code !== 200) {
//       ElMessage({
//         message: res.message || '请求失败',
//         type: 'error',
//         duration: 5 * 1000
//       })
//
//       // 401: 未登录或token过期
//       if (res.code === 401) {
//         const userStore = useUserStore()
//         userStore.logoutAction()
//         window.location.href = '/login'
//       }
//
//       return Promise.reject(new Error(res.message || '请求失败'))
//     } else {
//       return res
//     }
//   },
//   error => {
//     console.error('响应错误:', error)
//     ElMessage({
//       message: error.message || '网络错误',
//       type: 'error',
//       duration: 5 * 1000
//     })
//     return Promise.reject(error)
//   }
// )

service.interceptors.response.use(
    response => {
        const res = response.data

        // 有 code 字段（可能是标准格式或错误响应）
        if (res && typeof res.code === 'number') {
            // 成功标准格式：code=200 且有 data
            if (res.code === 200 && 'data' in res) {
                return res.data
            }

            // 错误响应：code !== 200
            if (res.code !== 200) {
                ElMessage.error(res.message || '请求失败')
                if (res.code === 401) {
                    const userStore = useUserStore()
                    userStore.logoutAction()
                    window.location.href = '/login'
                }
                return Promise.reject(new Error(res.message || '请求失败'))
            }
        }

        // 无 code 或 code 不是数字：直接返回数据（AI 接口）
        return res
    },
    error => {
        console.error('响应错误:', error)
        ElMessage.error(error.message || '网络错误')
        return Promise.reject(error)
    }
)

export default service