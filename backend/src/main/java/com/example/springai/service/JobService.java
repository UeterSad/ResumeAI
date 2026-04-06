package com.example.springai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.springai.model.JobCategory;
import com.example.springai.model.JobPosition;

import java.util.List;

public interface JobService {
    /**
     * 查询全部职位分类。
     */
    List<JobCategory> listAllCategories();

    /**
     * 新增职位分类。
     */
    JobCategory createCategory(JobCategory categoryPayload);

    /**
     * 删除职位分类（若分类下存在职位则抛错）。
     */
    boolean removeCategory(Integer categoryId);

    /**
     * 分页查询职位列表。
     */
    IPage<JobPosition> queryJobPage(int pageNumber, int pageSize, String keyword, Integer categoryId);

    /**
     * 根据职位 ID 查询详情。
     */
    JobPosition findJobById(Long jobId);

    /**
     * 新增职位。
     */
    boolean createJob(JobPosition jobPayload);

    /**
     * 更新职位。
     */
    boolean updateJob(JobPosition jobPayload);

    /**
     * 删除职位。
     */
    boolean removeJob(Long jobId);
}
