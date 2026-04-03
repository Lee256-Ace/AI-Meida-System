import { defineStore } from 'pinia'
import {reactive, ref} from 'vue'

/**
 * Markdown编辑器 页面缓存，切换窗口保留输入
 * */
export const useMarkdownCacheStore = defineStore('markdownCache', () => {
    // 存放所有的标签页数据
    const tabs = ref([])
    const activeTabId = ref(null)

    // 保存数据的方法
    const saveEditorState = (newTabs, newActiveId) => {
        tabs.value = JSON.parse(JSON.stringify(newTabs)) // 深拷贝防止引用污染
        activeTabId.value = newActiveId
    }

    /**
     * 从其它页面（生成/美化/续写）打开 Markdown 编辑器的新标签页，并激活该标签。
     * 约定字段与 MarkdownEditor.vue 保持一致（含 savedCopyId）。
     */
    const openNewTab = ({ title = '未命名', content = '' } = {}) => {
        const id = Date.now()
        const tab = {
            id,
            title,
            content,
            isEditing: false,
            savedCopyId: null
        }
        if (!Array.isArray(tabs.value)) tabs.value = []
        tabs.value.push(tab)
        activeTabId.value = id
        return id
    }

    return { tabs, activeTabId, saveEditorState, openNewTab }
}, {
    persist: true // 如果安装了 pinia-plugin-persistedstate，开启此项可自动同步到 LocalStorage
})