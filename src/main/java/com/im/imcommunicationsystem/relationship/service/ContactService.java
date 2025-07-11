package com.im.imcommunicationsystem.relationship.service;

import com.im.imcommunicationsystem.relationship.dto.response.ContactResponse;
import com.im.imcommunicationsystem.relationship.entity.Contact;

import java.util.List;
import java.util.Optional;

/**
 * 联系人服务接口
 * 处理联系人关系管理的核心业务逻辑
 */
public interface ContactService {

    /**
     * 获取联系人列表
     * @param userId 用户ID
     * @param includeBlocked 是否包含被屏蔽的联系人
     * @return 联系人响应列表
     */
    List<ContactResponse> getContactList(Long userId, boolean includeBlocked);

    /**
     * 获取联系人详情
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 联系人响应
     */
    Optional<ContactResponse> getContactDetail(Long userId, Long friendId);

    /**
     * 设置好友备注
     * @param userId 用户ID
     * @param friendId 好友ID
     * @param alias 备注名
     * @return 是否设置成功
     */
    boolean setContactAlias(Long userId, Long friendId, String alias);

    /**
     * 屏蔽联系人
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否屏蔽成功
     */
    boolean blockContact(Long userId, Long friendId);

    /**
     * 解除屏蔽
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否解除成功
     */
    boolean unblockContact(Long userId, Long friendId);

    /**
     * 删除好友关系
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否删除成功
     */
    boolean deleteContact(Long userId, Long friendId);

    /**
     * 添加好友关系
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否添加成功
     */
    boolean addContact(Long userId, Long friendId);

    /**
     * 检查联系人关系状态
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 联系人关系
     */
    Optional<Contact> checkContactRelationship(Long userId, Long friendId);

    /**
     * 检查是否为好友关系
     * @param userId1 用户1 ID
     * @param userId2 用户2 ID
     * @return 是否为好友
     */
    boolean isFriend(Long userId1, Long userId2);

    /**
     * 检查是否被屏蔽
     * @param userId 用户ID
     * @param friendId 好友ID
     * @return 是否被屏蔽
     */
    boolean isBlocked(Long userId, Long friendId);

    /**
     * 获取好友数量
     * @param userId 用户ID
     * @return 好友数量
     */
    long getFriendCount(Long userId);

    /**
     * 搜索联系人
     * @param userId 用户ID
     * @param keyword 搜索关键词（备注名）
     * @return 联系人响应列表
     */
    List<ContactResponse> searchContacts(Long userId, String keyword);

    /**
     * 获取被屏蔽的联系人列表
     * @param userId 用户ID
     * @return 被屏蔽的联系人列表
     */
    List<ContactResponse> getBlockedContacts(Long userId);

    /**
     * 批量删除联系人
     * @param userId 用户ID
     * @param friendIds 好友ID列表
     * @return 删除成功的数量
     */
    int batchDeleteContacts(Long userId, List<Long> friendIds);

    /**
     * 批量屏蔽联系人
     * @param userId 用户ID
     * @param friendIds 好友ID列表
     * @return 屏蔽成功的数量
     */
    int batchBlockContacts(Long userId, List<Long> friendIds);
}