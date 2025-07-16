package com.im.imcommunicationsystem.relationship.controller;

import com.im.imcommunicationsystem.common.dto.ApiResponse;
import com.im.imcommunicationsystem.relationship.dto.request.ContactTagAssignRequest;
import com.im.imcommunicationsystem.relationship.dto.response.ContactTagAssignmentResponse;
import com.im.imcommunicationsystem.relationship.dto.response.ContactTagResponse;
import com.im.imcommunicationsystem.relationship.dto.response.ContactResponse;
import com.im.imcommunicationsystem.relationship.service.ContactTagAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

/**
 * 联系人标签分配控制器
 * 负责处理联系人与标签的分配关系管理
 */
@RestController
@RequestMapping("/api/v1/contact-tag-assignments")
@RequiredArgsConstructor
@Validated
@Tag(name = "联系人标签分配管理", description = "联系人与标签的分配关系操作")
public class ContactTagAssignmentController {

    private final ContactTagAssignmentService assignmentService;

    @PostMapping
    @Operation(summary = "为联系人分配标签", description = "为指定联系人分配一个或多个标签")
    public ApiResponse<List<ContactTagAssignmentResponse>> assignTagsToContact(
            @Valid @RequestBody ContactTagAssignRequest request) {
        List<ContactTagAssignmentResponse> responses = assignmentService.assignTagsToContact(request);
        return ApiResponse.success(responses);
    }

    @DeleteMapping
    @Operation(summary = "移除联系人的标签", description = "移除指定联系人的一个或多个标签")
    public ApiResponse<Integer> removeTagsFromContact(
            @Parameter(description = "用户ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "好友ID") @RequestParam @NotNull @Positive Long friendId,
            @Parameter(description = "标签ID列表") @RequestBody @NotEmpty List<@Positive Long> tagIds) {
        int removedCount = assignmentService.removeTagsFromContact(userId, friendId, tagIds);
        return ApiResponse.success(removedCount);
    }

    @GetMapping("/contact-tags")
    @Operation(summary = "获取联系人的所有标签", description = "获取指定联系人的所有标签列表")
    public ApiResponse<List<ContactTagResponse>> getContactTags(
            @Parameter(description = "用户ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "好友ID") @RequestParam @NotNull @Positive Long friendId) {
        List<ContactTagResponse> tags = assignmentService.getContactTags(userId, friendId);
        return ApiResponse.success(tags);
    }

    @GetMapping("/tag-contacts")
    @Operation(summary = "获取标签下的所有联系人", description = "获取使用指定标签的所有联系人ID列表")
    public ApiResponse<List<Long>> getContactsByTag(
            @Parameter(description = "用户ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "标签ID") @RequestParam @NotNull @Positive Long tagId) {
        List<Long> contactIds = assignmentService.getContactsByTag(userId, tagId);
        return ApiResponse.success(contactIds);
    }

    @PostMapping("/batch-query")
    @Operation(summary = "批量获取联系人标签", description = "批量获取多个联系人的标签信息")
    public ApiResponse<Map<Long, List<ContactTagResponse>>> getBatchContactTags(
            @Parameter(description = "用户ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "好友ID列表") @RequestBody @NotEmpty List<@Positive Long> friendIds) {
        Map<Long, List<ContactTagResponse>> result = assignmentService.getBatchContactTags(userId, friendIds);
        return ApiResponse.success(result);
    }

    @PutMapping("/replace")
    @Operation(summary = "替换联系人的所有标签", description = "替换指定联系人的所有标签")
    public ApiResponse<List<ContactTagAssignmentResponse>> replaceContactTags(
            @Parameter(description = "用户ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "好友ID") @RequestParam @NotNull @Positive Long friendId,
            @Parameter(description = "新的标签ID列表") @RequestBody List<@Positive Long> tagIds) {
        List<ContactTagAssignmentResponse> responses = assignmentService.replaceContactTags(userId, friendId, tagIds);
        return ApiResponse.success(responses);
    }

    @GetMapping("/has-tag")
    @Operation(summary = "检查联系人是否有指定标签", description = "检查指定联系人是否已分配某个标签")
    public ApiResponse<Boolean> hasTag(
            @Parameter(description = "用户ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "好友ID") @RequestParam @NotNull @Positive Long friendId,
            @Parameter(description = "标签ID") @RequestParam @NotNull @Positive Long tagId) {
        boolean hasTag = assignmentService.hasTag(userId, friendId, tagId);
        return ApiResponse.success(hasTag);
    }

    @GetMapping("/contact-tag-count")
    @Operation(summary = "获取联系人的标签数量", description = "获取指定联系人的标签总数")
    public ApiResponse<Integer> getContactTagCount(
            @Parameter(description = "用户ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "好友ID") @RequestParam @NotNull @Positive Long friendId) {
        int count = assignmentService.getContactTagCount(userId, friendId);
        return ApiResponse.success(count);
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清除联系人的所有标签", description = "清除指定联系人的所有标签")
    public ApiResponse<Integer> clearContactTags(
            @Parameter(description = "用户ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "好友ID") @RequestParam @NotNull @Positive Long friendId) {
        int clearedCount = assignmentService.clearContactTags(userId, friendId);
        return ApiResponse.success(clearedCount);
    }

    @GetMapping("/tag-usage-count")
    @Operation(summary = "获取标签使用次数", description = "获取指定标签被多少个联系人使用")
    public ApiResponse<Long> getTagUsageCount(
            @Parameter(description = "用户ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "标签ID") @RequestParam @NotNull @Positive Long tagId) {
        long count = assignmentService.getTagUsageCount(userId, tagId);
        return ApiResponse.success(count);
    }

    @GetMapping("/tag-contact-details")
    @Operation(summary = "获取标签下的所有联系人详细信息", description = "获取使用指定标签的所有联系人的详细信息列表")
    public ApiResponse<List<ContactResponse>> getContactDetailsByTag(
            @Parameter(description = "用户ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "标签ID") @RequestParam @NotNull @Positive Long tagId) {
        List<ContactResponse> contacts = assignmentService.getContactDetailsByTag(userId, tagId);
        return ApiResponse.success(contacts);
    }
}