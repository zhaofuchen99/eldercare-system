package com.zfc.eldercare.core.vo;

import java.util.List;

/**
 * 评测答案快照（answers JSON 结构，详细设计文档 6.3.7）。
 */
public record SnapshotVO(
        int version,
        List<SnapshotItemVO> items
) {
    /** 单题答案快照 */
    public record SnapshotItemVO(
            Long qid,
            /** SINGLE/MULTIPLE/TEXT */
            String type,
            /** 单选为下标数字，多选为下标数组，文本为字符串 */
            Object value,
            /** 该题得分（仅计分题有值） */
            Integer score,
            Integer maxScore
    ) {
    }
}
