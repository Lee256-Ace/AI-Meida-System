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

    return { tabs, activeTabId, saveEditorState }
}, {
    persist: true // 如果安装了 pinia-plugin-persistedstate，开启此项可自动同步到 LocalStorage
})