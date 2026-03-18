package com.product.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ErrorLog {
    private Long id;
    /** 模块：generate / polish / continue / coze / auth 等 */
    private String module;
    /** 阶段/步骤描述 */
    private String phase;
    /** 错误摘要（面向用户或日志列表） */
    private String errorMessage;
    /** 堆栈或详细内容 */
    private String errorDetail;
    /** 请求上下文，如 taskId、简要参数 */
    private String requestInfo;
    private LocalDateTime createdAt;
}
