package com.zfc.eldercare.core.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zfc.eldercare.core.dto.MessageBatchPushDTO;
import com.zfc.eldercare.core.dto.MessagePushDTO;
import com.zfc.eldercare.core.entity.Message;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.MessageMapper;
import com.zfc.eldercare.core.mapper.UserMapper;
import com.zfc.eldercare.core.service.MessageService;
import com.zfc.eldercare.core.vo.MessageUnreadVO;
import com.zfc.eldercare.core.vo.MessageVO;
import com.zfc.eldercare.core.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 站内消息服务实现（详细设计文档 5.7）。
 * 会员端：消息列表/详情/标记已读均校验消息归属（非本人 403）；管理端：推送校验目标用户存在，批量推送单事务。
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    // ========== 会员端 ==========

    @Override
    public PageVO<MessageVO> userMessages(Long userId, int page, int size) {
        PageHelper.startPage(page, size);
        return toPageVO(messageMapper.selectPageByUserId(userId));
    }

    @Override
    public MessageVO messageDetail(Long userId, Long id) {
        return MessageVO.from(requireOwnedMessage(userId, id));
    }

    @Override
    public void markRead(Long userId, Long id) {
        requireOwnedMessage(userId, id);
        // 幂等：已是已读态 markRead 返回 0，无需处理
        messageMapper.markRead(id, userId);
    }

    @Override
    public MessageUnreadVO unreadStats(Long userId) {
        return new MessageUnreadVO(messageMapper.countByUserId(userId),
                messageMapper.countUnreadByUserId(userId));
    }

    // ========== 管理端 ==========

    @Override
    public PageVO<MessageVO> messagePage(Long userId, String type, int page, int size) {
        PageHelper.startPage(page, size);
        return toPageVO(messageMapper.selectPage(userId, type));
    }

    @Override
    public MessageVO adminMessageDetail(Long id) {
        return MessageVO.from(requireMessage(id));
    }

    @Override
    @Transactional
    public Long pushMessage(MessagePushDTO dto) {
        requireUser(dto.userId());
        Message message = new Message();
        message.setUserId(dto.userId());
        message.setTitle(dto.title());
        message.setContent(dto.content());
        message.setType(dto.type());
        messageMapper.insert(message);
        return message.getId();
    }

    @Override
    @Transactional
    public int pushBatch(MessageBatchPushDTO dto) {
        List<Long> userIds = dto.userIds().stream().distinct().toList();
        requireUsers(userIds);
        for (Long userId : userIds) {
            Message message = new Message();
            message.setUserId(userId);
            message.setTitle(dto.title());
            message.setContent(dto.content());
            message.setType(dto.type());
            messageMapper.insert(message);
        }
        return userIds.size();
    }

    @Override
    public void deleteMessage(Long id) {
        requireMessage(id);
        messageMapper.delete(id);
    }

    // ========== 私有辅助 ==========

    /** 分页结果组装（PageVO 直接取 PageInfo getter 构建） */
    private PageVO<MessageVO> toPageVO(List<Message> list) {
        PageInfo<Message> pageInfo = new PageInfo<>(list);
        List<MessageVO> voList = list.stream().map(MessageVO::from).toList();
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    private Message requireMessage(Long id) {
        Message message = messageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException(404, "消息不存在");
        }
        return message;
    }

    /** 校验消息归属：非本人 403（文档 7.3：资源非本人） */
    private Message requireOwnedMessage(Long userId, Long id) {
        Message message = requireMessage(id);
        if (!message.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该消息");
        }
        return message;
    }

    private void requireUser(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(404, "目标用户不存在");
        }
    }

    private void requireUsers(List<Long> userIds) {
        int existCount = userMapper.selectByIds(userIds).size();
        if (existCount != userIds.size()) {
            throw new BusinessException(404, "存在不存在的目标用户");
        }
    }
}
