package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.vo.MemberVO;
import com.zfc.eldercare.core.vo.PageVO;

/**
 * 会员管理服务（详细设计文档 5.10，管理端）。
 */
public interface MemberService {

    /** 会员分页（按关键字/状态/等级筛选，role=MEMBER） */
    PageVO<MemberVO> memberPage(String keyword, String status, String memberLevel, int page, int size);

    /** 会员详情（基本信息 + 积分） */
    MemberVO memberDetail(Long id);

    /** 启用会员 */
    void enable(Long id);

    /** 禁用会员 */
    void disable(Long id);

    /** 调整会员等级 */
    void updateLevel(Long id, String memberLevel);

    /** 手动调整积分（正=调增，负=调减），返回调整后余额 */
    int adjustPoints(Long id, int delta);

    /**
     * 重置密码：可指定新密码或由系统生成，BCrypt 加密更新后强制该会员所有设备下线；
     * 返回最终密码（系统生成的需告知管理员）。
     */
    String resetPassword(Long id, String password);
}
