import { defineStore } from 'pinia'
import { reactive } from 'vue'

/**
 * 文案生成 / 文章美化 / 文章续写 三个页面的缓存，切换窗口时保留表单与结果
 */
export const useAiWritingCacheStore = defineStore('aiWritingCache', () => {
  const writing = reactive({
    form: { prompt: '', platforms: ['douyin', 'xiaohongshu', 'video_account', 'toutiao'], targetAudience: '' },
    result: { show: false, taskId: '-', status: 'processing', reasoning: '', content: '', task: null }
  })
  const polish = reactive({
    form: { content: '', type: 'enhance', targetAudience: '大众', platform: '' },
    result: { show: false, taskId: '-', status: 'processing', content: '', platformName: '' }
  })
  const continue_ = reactive({
    form: { content: '', hint: '', platform: '', extendLength: '中等', extendStyle: '保持原风格' },
    result: { show: false, taskId: '-', status: 'processing', content: '', platformName: '' }
  })

  function saveWriting(form, result) {
    writing.form.prompt = form.prompt ?? ''
    writing.form.platforms = Array.isArray(form.platforms) ? [...form.platforms] : []
    writing.form.targetAudience = form.targetAudience ?? ''
    writing.result.show = !!result.show
    writing.result.taskId = result.taskId ?? '-'
    writing.result.status = result.status ?? 'processing'
    writing.result.reasoning = result.reasoning ?? ''
    writing.result.content = result.content ?? ''
    writing.result.task = result.task != null ? result.task : null
  }

  function savePolish(form, result) {
    polish.form.content = form.content ?? ''
    polish.form.type = form.type ?? 'enhance'
    polish.form.targetAudience = form.targetAudience ?? '大众'
    polish.form.platform = form.platform ?? ''
    polish.result.show = !!result.show
    polish.result.taskId = result.taskId ?? '-'
    polish.result.status = result.status ?? 'processing'
    polish.result.content = result.content ?? ''
    polish.result.platformName = result.platformName ?? ''
  }

  function saveContinue(form, result) {
    continue_.form.content = form.content ?? ''
    continue_.form.hint = form.hint ?? ''
    continue_.form.platform = form.platform ?? ''
    continue_.form.extendLength = form.extendLength ?? '中等'
    continue_.form.extendStyle = form.extendStyle ?? '保持原风格'
    continue_.result.show = !!result.show
    continue_.result.taskId = result.taskId ?? '-'
    continue_.result.status = result.status ?? 'processing'
    continue_.result.content = result.content ?? ''
    continue_.result.platformName = result.platformName ?? ''
  }

  return { writing, polish, continue: continue_, saveWriting, savePolish, saveContinue }
})
