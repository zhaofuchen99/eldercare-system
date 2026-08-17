package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.PointTransaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分流水表 Mapper（文档 6.3.18）。
 */
@Mapper
public interface PointTransactionMapper {

    int insert(PointTransaction transaction);
}
