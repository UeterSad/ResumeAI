package com.example.springai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springai.mapper.JobCategoryMapper;
import com.example.springai.mapper.JobPositionMapper;
import com.example.springai.model.JobCategory;
import com.example.springai.model.JobPosition;
import com.example.springai.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class JobServiceImpl implements JobService {

    @Autowired
    private JobCategoryMapper categoryMapper;

    @Autowired
    private JobPositionMapper positionMapper;

    @Override
    public List<JobCategory> listAllCategories() {
        return categoryMapper.selectList(null);
    }

    @Override
    @Transactional
    public JobCategory createCategory(JobCategory categoryPayload) {
        categoryPayload.setCreateTime(new Date());
        categoryMapper.insert(categoryPayload);
        return categoryPayload;
    }

    @Override
    @Transactional
    public boolean removeCategory(Integer categoryId) {
        // 分类删除前先检查是否仍被职位引用。
        LambdaQueryWrapper<JobPosition> categoryUsedQuery = new LambdaQueryWrapper<>();
        categoryUsedQuery.eq(JobPosition::getCategoryId, categoryId);
        long linkedJobCount = positionMapper.selectCount(categoryUsedQuery);
        if (linkedJobCount > 0) {
            throw new RuntimeException("该分类下还有职位，无法删除");
        }

        return categoryMapper.deleteById(categoryId) > 0;
    }

    @Override
    public IPage<JobPosition> queryJobPage(int pageNumber, int pageSize, String keyword, Integer categoryId) {
        Page<JobPosition> pageRequest = new Page<>(pageNumber, pageSize);
        return positionMapper.findJobs(pageRequest, keyword, categoryId);
    }

    @Override
    public JobPosition findJobById(Long jobId) {
        JobPosition jobPosition = positionMapper.selectById(jobId);
        if (jobPosition != null) {
            JobCategory category = categoryMapper.selectById(jobPosition.getCategoryId());
            if (category != null) {
                jobPosition.setCategoryName(category.getName());
            }
        }
        return jobPosition;
    }

    @Override
    @Transactional
    public boolean createJob(JobPosition jobPayload) {
        Date now = new Date();
        jobPayload.setCreateTime(now);
        jobPayload.setUpdateTime(now);
        jobPayload.setStatus(jobPayload.getStatus() == null ? 1 : jobPayload.getStatus());
        return positionMapper.insert(jobPayload) > 0;
    }

    @Override
    @Transactional
    public boolean updateJob(JobPosition jobPayload) {
        jobPayload.setUpdateTime(new Date());
        return positionMapper.updateById(jobPayload) > 0;
    }

    @Override
    @Transactional
    public boolean removeJob(Long jobId) {
        return positionMapper.deleteById(jobId) > 0;
    }
}
