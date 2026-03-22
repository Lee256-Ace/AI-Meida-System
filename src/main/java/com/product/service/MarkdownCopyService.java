package com.product.service;

import com.product.dto.MarkdownCopySaveRequest;
import com.product.entity.MarkdownCopy;
import com.product.mapper.MarkdownCopyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MarkdownCopyService {

    private final MarkdownCopyMapper markdownCopyMapper;

    /**
     * 保存当前标签文案：无 id 或 id 不存在则插入并返回新 id；否则更新。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long save(MarkdownCopySaveRequest req) {
        String title = StringUtils.hasText(req.getTitle()) ? req.getTitle().trim() : "未命名";
        String content = req.getContent() != null ? req.getContent() : "";

        LocalDateTime now = LocalDateTime.now();
        Long id = req.getId();
        if (id != null) {
            MarkdownCopy existing = markdownCopyMapper.selectById(id);
            if (existing != null) {
                existing.setTitle(title);
                existing.setContent(content);
                existing.setUpdatedAt(now);
                markdownCopyMapper.update(existing);
                return id;
            }
        }

        MarkdownCopy row = new MarkdownCopy();
        row.setTitle(title);
        row.setContent(content);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        markdownCopyMapper.insert(row);
        return row.getId();
    }
}
