package com.product.service;

import com.product.common.PageResult;
import com.product.entity.*;
import com.product.mapper.*;
import com.product.param.HistoryQueryParam;
import com.product.vo.HistoryRecordVO;
import com.product.vo.StatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final GenerationMapper generationMapper;
    private final PolishMapper polishMapper;
    private final ContinuationMapper continuationMapper;
    private final MarkdownCopyMapper markdownCopyMapper;

    /**
     * 查询统一历史记录
     */
    public PageResult<HistoryRecordVO> queryHistory(HistoryQueryParam param) {
        List<HistoryRecordVO> allRecords = new ArrayList<>();

        // 根据类型筛选查询
        if ("all".equals(param.getType()) || "generate".equals(param.getType())) {
            allRecords.addAll(queryGenerationHistory(param));
        }
        if ("all".equals(param.getType()) || "polish".equals(param.getType())) {
            allRecords.addAll(queryPolishHistory(param));
        }
        if ("all".equals(param.getType()) || "continue".equals(param.getType())) {
            allRecords.addAll(queryContinueHistory(param));
        }
        if ("all".equals(param.getType()) || "markdown".equals(param.getType())) {
            allRecords.addAll(queryMarkdownHistory(param));
        }

        // 状态筛选
        if (!"all".equals(param.getStatus())) {
            allRecords = allRecords.stream()
                    .filter(r -> r.getStatus().equals(param.getStatus()))
                    .collect(Collectors.toList());
        }

        // 平台筛选：单平台记录看 platform，文案生成看 platforms 是否包含该平台（含今日头条）
        if (param.getPlatform() != null && !param.getPlatform().isBlank()) {
            String pf = param.getPlatform().trim();
            allRecords = allRecords.stream()
                    .filter(r -> pf.equals(r.getPlatform())
                            || (r.getPlatforms() != null && r.getPlatforms().contains(pf)))
                    .collect(Collectors.toList());
        }

        // 关键词搜索
        if (param.getKeyword() != null && !param.getKeyword().isEmpty()) {
            String kw = param.getKeyword().toLowerCase();
            allRecords = allRecords.stream()
                    .filter(r -> r.getTitle().toLowerCase().contains(kw)
                            || (r.getPreview() != null && r.getPreview().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }

        // 排序
        Comparator<HistoryRecordVO> comparator;
        if ("wordCount".equals(param.getSortBy())) {
            comparator = Comparator.comparing(HistoryRecordVO::getWordCount,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        } else {
            comparator = Comparator.comparing(HistoryRecordVO::getCreatedAt);
        }

        if ("asc".equals(param.getSortOrder())) {
            allRecords.sort(comparator);
        } else {
            allRecords.sort(comparator.reversed());
        }

        // 分页
        int total = allRecords.size();
        int start = (param.getPage() - 1) * param.getSize();
        int end = Math.min(start + param.getSize(), total);

        List<HistoryRecordVO> pageList = start < total ?
                allRecords.subList(start, end) : new ArrayList<>();

        // 组装分页结果
        PageResult<HistoryRecordVO> result = new PageResult<>();
        result.setList(pageList);
        result.setTotal((long) total);
        result.setPage(param.getPage());
        result.setSize(param.getSize());
        result.setTotalPages((int) Math.ceil((double) total / param.getSize()));
        result.setHasNext(end < total);
        result.setHasPrev(param.getPage() > 1);

        return result;
    }

    /**
     * 获取单条记录详情
     */
    public Object getDetail(String type, Long id) {
        return switch (type) {
            case "generate" -> generationMapper.selectById(id);
            case "polish" -> polishMapper.selectById(id);
            case "continue" -> continuationMapper.selectById(id);
            case "markdown" -> markdownCopyMapper.selectById(id);
            default -> null;
        };
    }

    /**
     * 删除单条记录
     */
    public boolean deleteRecord(String type, Long id) {
        int result = switch (type) {
            case "generate" -> generationMapper.deleteById(id);
            case "polish" -> polishMapper.deleteById(id);
            case "continue" -> continuationMapper.deleteById(id);
            case "markdown" -> markdownCopyMapper.deleteById(id);
            default -> 0;
        };
        return result > 0;
    }

    /**
     * 批量删除
     */
    public int batchDelete(List<String> ids) {
        int count = 0;
        for (String id : ids) {
            String[] parts = id.split(":");
            if (parts.length == 2 && deleteRecord(parts[0], Long.parseLong(parts[1]))) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取统计数据
     */
    public StatisticsVO getStatistics() {
        StatisticsVO vo = new StatisticsVO();

        // 各类型数量
        vo.setGenerateCount(generationMapper.count());
        vo.setPolishCount(polishMapper.count());
        vo.setContinueCount(continuationMapper.count());
        long markdownCount = safeLong(markdownCopyMapper.count());
        vo.setTotalCount(vo.getGenerateCount() + vo.getPolishCount() + vo.getContinueCount() + markdownCount);

        // 今日新增
        vo.setTodayCount(generationMapper.countToday() +
                polishMapper.countToday() +
                continuationMapper.countToday() +
                safeLong(markdownCopyMapper.countToday()));

        // 总字数
        vo.setTotalWords(generationMapper.sumWords() +
                polishMapper.sumWords() +
                continuationMapper.sumWords() +
                safeLong(markdownCopyMapper.sumWords()));

        return vo;
    }

    // ========== 私有方法 ==========

    private List<HistoryRecordVO> queryGenerationHistory(HistoryQueryParam param) {
        List<GenerationTask> list = generationMapper.selectAll();
        return list.stream().map(this::convertGeneration).collect(Collectors.toList());
    }

    private List<HistoryRecordVO> queryPolishHistory(HistoryQueryParam param) {
        List<PolishTask> list = polishMapper.selectAll();
        return list.stream().map(this::convertPolish).collect(Collectors.toList());
    }

    private List<HistoryRecordVO> queryContinueHistory(HistoryQueryParam param) {
        List<ContinuationTask> list = continuationMapper.selectAll();
        return list.stream().map(this::convertContinue).collect(Collectors.toList());
    }

    private List<HistoryRecordVO> queryMarkdownHistory(HistoryQueryParam param) {
        List<MarkdownCopy> list = markdownCopyMapper.selectAll();
        return list.stream().map(this::convertMarkdown).collect(Collectors.toList());
    }

    private HistoryRecordVO convertGeneration(GenerationTask task) {
        HistoryRecordVO vo = new HistoryRecordVO();
        vo.setId(task.getId());
        vo.setType("generate");
        vo.setTypeName("文案生成");
        vo.setTitle(truncate(task.getPrompt(), 50));
        vo.setPreview(truncate(task.getContent(), 100));
        vo.setStatus(convertStatus(task.getStatus()));
        vo.setStatusName(getStatusName(task.getStatus()));
        vo.setCreatedAt(task.getCreatedAt());
        vo.setTimeAgo(calculateTimeAgo(task.getCreatedAt()));
        vo.setWordCount(task.getWordCount());
        // 有内容的平台列表，用于按平台筛选与展示（含抖音/小红书/视频号/今日头条）
        List<String> platforms = new ArrayList<>();
        if (task.getDouyinCopy() != null && !task.getDouyinCopy().isBlank()) platforms.add("douyin");
        if (task.getXiaohongshuCopy() != null && !task.getXiaohongshuCopy().isBlank()) platforms.add("xiaohongshu");
        if (task.getVideoAccountCopy() != null && !task.getVideoAccountCopy().isBlank()) platforms.add("video_account");
        if (task.getToutiaoCopy() != null && !task.getToutiaoCopy().isBlank()) platforms.add("toutiao");
        vo.setPlatforms(platforms.isEmpty() ? null : platforms);
        return vo;
    }

    private HistoryRecordVO convertPolish(PolishTask task) {
        HistoryRecordVO vo = new HistoryRecordVO();
        vo.setId(task.getId());
        vo.setType("polish");
        vo.setTypeName("文章美化");
        vo.setTitle(truncate(task.getOriginalContent(), 50));
        vo.setPreview(truncate(task.getResultContent(), 100));
        vo.setStatus(convertStatus(task.getStatus()));
        vo.setStatusName(getStatusName(task.getStatus()));
        vo.setPlatform(task.getPlatform());
        vo.setPlatformName(platformKeyToName(task.getPlatform()));
        vo.setCreatedAt(task.getCreatedAt());
        vo.setTimeAgo(calculateTimeAgo(task.getCreatedAt()));
        vo.setWordCount(task.getResultContent() != null ? task.getResultContent().length() : 0);
        return vo;
    }

    private HistoryRecordVO convertContinue(ContinuationTask task) {
        HistoryRecordVO vo = new HistoryRecordVO();
        vo.setId(task.getId());
        vo.setType("continue");
        vo.setTypeName("文章续写");
        vo.setTitle(truncate(task.getOriginalContent(), 50));
        vo.setPreview(truncate(task.getContinuation(), 100));
        vo.setStatus(convertStatus(task.getStatus()));
        vo.setStatusName(getStatusName(task.getStatus()));
        vo.setPlatform(task.getPlatform());
        vo.setPlatformName(platformKeyToName(task.getPlatform()));
        vo.setCreatedAt(task.getCreatedAt());
        vo.setTimeAgo(calculateTimeAgo(task.getCreatedAt()));
        vo.setWordCount(task.getTotalLength());
        return vo;
    }

    private HistoryRecordVO convertMarkdown(MarkdownCopy row) {
        HistoryRecordVO vo = new HistoryRecordVO();
        vo.setId(row.getId());
        vo.setType("markdown");
        vo.setTypeName("保存文案");
        vo.setTitle(truncate(row.getTitle(), 50));
        vo.setPreview(truncate(row.getContent(), 100));
        vo.setStatus("completed");
        vo.setStatusName("已保存");
        LocalDateTime time = row.getUpdatedAt() != null ? row.getUpdatedAt() : row.getCreatedAt();
        vo.setCreatedAt(time);
        vo.setTimeAgo(calculateTimeAgo(time));
        vo.setWordCount(row.getContent() != null ? row.getContent().length() : 0);
        return vo;
    }

    private Long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private String convertStatus(Integer status) {
        return switch (status) {
            case 0 -> "processing";
            case 1 -> "completed";
            case 2 -> "failed";
            default -> "unknown";
        };
    }

    private String getStatusName(Integer status) {
        return switch (status) {
            case 0 -> "处理中";
            case 1 -> "已完成";
            case 2 -> "失败";
            default -> "未知";
        };
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }

    private static String platformKeyToName(String key) {
        if (key == null || key.isBlank()) return null;
        return switch (key) {
            case "douyin" -> "抖音";
            case "xiaohongshu" -> "小红书";
            case "video_account" -> "视频号";
            case "toutiao" -> "今日头条";
            default -> key;
        };
    }

    private String calculateTimeAgo(LocalDateTime time) {
        Duration duration = Duration.between(time, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1) return "刚刚";
        if (minutes < 60) return minutes + "分钟前";
        if (hours < 24) return hours + "小时前";
        if (days < 30) return days + "天前";
        return time.toLocalDate().toString();
    }
}