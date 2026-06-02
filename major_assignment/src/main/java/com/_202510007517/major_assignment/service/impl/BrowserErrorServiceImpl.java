package com._202510007517.major_assignment.service.impl;

import com._202510007517.major_assignment.entity.BrowserError;
import com._202510007517.major_assignment.entity.dto.PageResult;
import com._202510007517.major_assignment.mapper.BrowserErrorMapper;
import com._202510007517.major_assignment.service.BrowserErrorService;
import com._202510007517.major_assignment.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 浏览器错误日志服务实现类
 */
@Service
public class BrowserErrorServiceImpl implements BrowserErrorService {

    @Autowired
    private BrowserErrorMapper browserErrorMapper;

    @Override
    public Long saveBrowserError(BrowserError browserError) {
        browserError.init();
        browserErrorMapper.insert(browserError);
        return browserError.getId();
    }

    @Override
    public int batchSaveBrowserErrors(List<BrowserError> browserErrors) {
        browserErrors.forEach(BrowserError::init);
        return browserErrorMapper.batchInsert(browserErrors);
    }

    @Override
    public BrowserError getBrowserErrorById(Long id) {
        return browserErrorMapper.selectById(id);
    }

    @Override
    public PageResult<BrowserError> getBrowserErrorList(Map<String, String> params, int page, int size) {
        // 转换为Map<String, Object>以便添加分页参数
        Map<String, Object> queryParams = new HashMap<>(params);
        int total = browserErrorMapper.countByParams(queryParams);
        PageUtils.PageWindow window = PageUtils.resolvePageWindow(page, size, total);
        queryParams.put("offset", window.offset());
        queryParams.put("limit", window.size());

        List<BrowserError> errors = browserErrorMapper.selectByParams(queryParams);
        return PageUtils.buildPageResult(errors, window.page(), window.size(), total);
    }

    @Override
    public int updateBrowserError(BrowserError browserError) {
        browserError.updateTime();
        return browserErrorMapper.update(browserError);
    }

    @Override
    public int updateErrorStatus(Long id, Integer status, Long processedBy, String processResult) {
        return browserErrorMapper.updateStatusById(id, status, processedBy, processResult);
    }

    @Override
    public int deleteBrowserErrorById(Long id) {
        return browserErrorMapper.deleteById(id);
    }

    @Override
    public List<Map<String, Object>> getErrorTypeStats(String startTime, String endTime) {
        return browserErrorMapper.countByErrorType(startTime, endTime);
    }

    @Override
    public List<Map<String, Object>> getPageErrorStats(String startTime, String endTime, int limit) {
        return browserErrorMapper.countByPageUrl(startTime, endTime, limit);
    }
}
