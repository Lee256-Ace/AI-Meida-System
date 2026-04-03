package com.product.service;

import com.product.common.PageResult;
import com.product.entity.PublishHistory;
import com.product.mapper.PublishHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PublishHistoryService {

    @Autowired
    private PublishHistoryMapper publishHistoryMapper;

    public PublishHistory getById(Long id) {
        return publishHistoryMapper.selectById(id);
    }

    public PageResult<PublishHistory> getPage(Integer page, Integer size, String platformName, String title) {
        int p = page != null && page > 0 ? page : 1;
        int s = size != null && size > 0 ? size : 10;
        Integer offset = (p - 1) * s;
        boolean hasSearch = (platformName != null && !platformName.trim().isEmpty())
                || (title != null && !title.trim().isEmpty());
        List<PublishHistory> list;
        Integer total;
        if (hasSearch) {
            list = publishHistoryMapper.selectByPageWithSearch(offset, s, platformName, title);
            total = publishHistoryMapper.countTotalWithSearch(platformName, title);
        } else {
            list = publishHistoryMapper.selectByPage(offset, s);
            total = publishHistoryMapper.countTotal();
        }
        return new PageResult<>(list, total.longValue(), p, s);
    }

    public void add(PublishHistory row) {
        LocalDateTime now = LocalDateTime.now();
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        if (row.getStatus() == null || row.getStatus().isBlank()) {
            row.setStatus("recorded");
        }
        if (row.getTitle() == null) {
            row.setTitle("");
        }
        publishHistoryMapper.insert(row);
    }

    public void update(PublishHistory row) {
        if (row.getId() == null) {
            throw new RuntimeException("id 不能为空");
        }
        row.setUpdatedAt(LocalDateTime.now());
        publishHistoryMapper.update(row);
    }

    public void delete(Long id) {
        publishHistoryMapper.deleteById(id);
    }
}
