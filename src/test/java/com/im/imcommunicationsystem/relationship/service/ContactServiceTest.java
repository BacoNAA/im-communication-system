package com.im.imcommunicationsystem.relationship.service;

import com.im.imcommunicationsystem.relationship.entity.ContactId;
import com.im.imcommunicationsystem.relationship.repository.ContactRepository;
import com.im.imcommunicationsystem.relationship.service.impl.ContactServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private ContactTagAssignmentService contactTagAssignmentService;

    @InjectMocks
    private ContactServiceImpl contactService;

    private final Long userId = 1L;
    private final Long friendId = 2L;

    @BeforeEach
    void setUp() {
        // 可以在这里进行一些通用的设置
    }

    @Test
    void deleteContact_ShouldClearTagsAndDeleteRelationship() {
        // 设置测试预期行为
        doNothing().when(contactTagAssignmentService).clearContactTags(userId, friendId);
        doNothing().when(contactRepository).deleteById(new ContactId(userId, friendId));
        doNothing().when(contactRepository).deleteById(new ContactId(friendId, userId));

        // 执行测试
        boolean result = contactService.deleteContact(userId, friendId);

        // 验证结果
        assertTrue(result);
        // 验证标签清理被调用
        verify(contactTagAssignmentService, times(1)).clearContactTags(userId, friendId);
        // 验证联系人删除被调用了两次（双向关系）
        verify(contactRepository, times(1)).deleteById(new ContactId(userId, friendId));
        verify(contactRepository, times(1)).deleteById(new ContactId(friendId, userId));
    }

    @Test
    void deleteContact_ShouldReturnFalseWhenExceptionOccurs() {
        // 设置测试预期行为 - 模拟异常
        doThrow(new EmptyResultDataAccessException(1)).when(contactTagAssignmentService).clearContactTags(userId, friendId);

        // 执行测试
        boolean result = contactService.deleteContact(userId, friendId);

        // 验证结果
        assertFalse(result);
        // 验证标签清理被调用
        verify(contactTagAssignmentService, times(1)).clearContactTags(userId, friendId);
        // 验证联系人删除没有被调用
        verify(contactRepository, never()).deleteById(any());
    }
} 