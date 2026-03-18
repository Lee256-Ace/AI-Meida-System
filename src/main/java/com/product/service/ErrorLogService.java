package com.product.service;

import com.product.common.PageResult;
import com.product.entity.ErrorLog;
import com.product.mapper.ErrorLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 错误日志落库，便于在 MySQL basesys.ai_error_log 中排查问题。
 * 模块/阶段清晰，便于定位出错的程序与步骤。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorLogService {

    private final ErrorLogMapper errorLogMapper;

    /**
     * 记录错误到 ai_error_log，并返回可展示的提示（含模块、阶段、摘要）。
     * 若落库失败仅打日志，不抛异常。
     *
     * @param module  模块：generate / polish / continue / coze / auth
     * @param phase   阶段：如 coze_request / parse_result / save_db
     * @param message 错误摘要（会返回给前端或日志列表）
     * @param e       异常，可为 null；非空时会取 stackTrace 写入 error_detail
     * @param requestInfo 请求上下文，如 "taskId=123"、"prompt前50字"
     * @return 用于返回给前端的提示，格式：[模块] 阶段「phase」失败：message。错误已记录至系统日志。
     */
    public String logAndMessage(String module, String phase, String message, Throwable e, String requestInfo) {
        String detail = null;
        if (e != null) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            detail = sw.toString();
            if (detail.length() > 65535) detail = detail.substring(0, 65535);
        }
        String req = requestInfo;
        if (req != null && req.length() > 500) req = req.substring(0, 500);
        saveAsync(module, phase, message != null ? message : (e != null ? e.getMessage() : "未知错误"), detail, req);
        String phaseLabel = (phase != null && !phase.isBlank()) ? ("阶段「" + phase + "」") : "执行";
        return String.format("[%s] %s失败：%s。错误已记录至系统日志。",
                moduleLabel(module), phaseLabel, message != null && !message.isBlank() ? message : (e != null ? e.getMessage() : "未知"));
    }

    /** 仅落库，不返回文案。用于在 catch 里记录后另行返回提示的场景。 */
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void saveAsync(String module, String phase, String errorMessage, String errorDetail, String requestInfo) {
        try {
            ErrorLog entity = new ErrorLog();
            entity.setModule(module != null ? module : "unknown");
            entity.setPhase(phase);
            entity.setErrorMessage(errorMessage != null ? errorMessage : "");
            entity.setErrorDetail(errorDetail);
            entity.setRequestInfo(requestInfo);
            entity.setCreatedAt(LocalDateTime.now());
            errorLogMapper.insert(entity);
            log.debug("错误已入库 ai_error_log id={} module={} phase={}", entity.getId(), entity.getModule(), entity.getPhase());
        } catch (Exception ex) {
            log.warn("写入 ai_error_log 失败", ex);
        }
    }

    public PageResult<ErrorLog> list(String module, int page, int size) {
        if (size <= 0) size = 20;
        if (page < 1) page = 1;
        int offset = (page - 1) * size;
        List<ErrorLog> list = (module != null && !module.isBlank())
                ? errorLogMapper.selectByModule(module.trim(), size, offset)
                : errorLogMapper.selectList(size, offset);
        Long total = (module != null && !module.isBlank())
                ? errorLogMapper.countByModule(module.trim())
                : errorLogMapper.count();
        return new PageResult<>(list, total, page, size);
    }

    public ErrorLog getById(Long id) {
        return errorLogMapper.selectById(id);
    }

    public int deleteById(Long id) {
        return errorLogMapper.deleteById(id);
    }

    private static String moduleLabel(String module) {
        if (module == null) return "系统";
        switch (module) {
            case "generate": return "文案生成";
            case "polish": return "文章美化";
            case "continue": return "文章续写";
            case "coze": return "Coze工作流";
            case "auth": return "登录鉴权";
            default: return module;
        }
    }
}
