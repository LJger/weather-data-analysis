package com.example.backend.mapper;

import com.example.backend.entity.CollectionTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CollectionTaskMapper {

    CollectionTask findById(@Param("taskId") Long taskId);

    List<CollectionTask> findByUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId);

    void insert(CollectionTask task);

    void update(CollectionTask task);

    void deleteById(@Param("taskId") Long taskId);

    boolean existsById(@Param("taskId") Long taskId);
}
