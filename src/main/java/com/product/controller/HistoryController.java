package com.product.controller;

import com.product.common.PageResult;
import com.product.param.HistoryQueryParam;
import com.product.service.HistoryService;
import com.product.vo.HistoryRecordVO;
import com.product.vo.StatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HistoryController {

    private final HistoryService historyService;

    /**
     * 查询历史记录列表
     */
    @GetMapping("/list")
    public PageResult<HistoryRecordVO> list(HistoryQueryParam param) {
        return historyService.queryHistory(param);
    }

    /**
     * 获取单条详情
     */
    @GetMapping("/detail")
    public Object detail(@RequestParam String type, @RequestParam Long id) {
        return historyService.getDetail(type, id);
    }

    /**
     * 删除单条记录
     */
    @DeleteMapping("/{type}/{id}")
    public boolean delete(@PathVariable String type, @PathVariable Long id) {
        return historyService.deleteRecord(type, id);
    }

    /**
     * 批量删除
     */
    @PostMapping("/batch-delete")
    public int batchDelete(@RequestBody Map<String, List<String>> body) {
        List<String> ids = body.get("ids");
        return historyService.batchDelete(ids);
    }

    /**
     * 获取统计数据
     */
    @GetMapping("/statistics")
    public StatisticsVO statistics() {
        return historyService.getStatistics();
    }

    /**
     * 搜索建议（自动补全）
     */
    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam String keyword) {
        // 返回最近的搜索历史或热门提示词
        return List.of("写一篇关于AI的文章", "润色这段文字", "续写小说开头");
    }
}