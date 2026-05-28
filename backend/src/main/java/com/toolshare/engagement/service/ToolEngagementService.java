package com.toolshare.engagement.service;

import com.toolshare.engagement.model.ToolCommentRecord;
import com.toolshare.engagement.repository.ToolCommentRepository;
import com.toolshare.engagement.repository.ToolFavoriteRepository;
import com.toolshare.engagement.repository.ToolStarRepository;
import com.toolshare.engagement.web.CommentCreateRequest;
import com.toolshare.engagement.web.ToolCommentResponse;
import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.ForbiddenException;
import com.toolshare.exception.NotFoundException;
import com.toolshare.model.AuditLogEntry;
import com.toolshare.security.CurrentUser;
import com.toolshare.tool.repository.ToolRepository;
import com.toolshare.user.repository.AuditLogRepository;
import com.toolshare.user.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ToolEngagementService {

    private final ToolRepository toolRepository;
    private final ToolStarRepository toolStarRepository;
    private final ToolFavoriteRepository toolFavoriteRepository;
    private final ToolCommentRepository toolCommentRepository;
    private final CommentSanitizer commentSanitizer;
    private final AuditLogRepository auditLogRepository;

    public ToolEngagementService(ToolRepository toolRepository,
                                 ToolStarRepository toolStarRepository,
                                 ToolFavoriteRepository toolFavoriteRepository,
                                 ToolCommentRepository toolCommentRepository,
                                 CommentSanitizer commentSanitizer,
                                 AuditLogRepository auditLogRepository) {
        this.toolRepository = toolRepository;
        this.toolStarRepository = toolStarRepository;
        this.toolFavoriteRepository = toolFavoriteRepository;
        this.toolCommentRepository = toolCommentRepository;
        this.commentSanitizer = commentSanitizer;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void starTool(Long toolId, CurrentUser currentUser) {
        assertToolExists(toolId);
        if (toolStarRepository.exists(toolId, currentUser.id())) {
            throw new BadRequestException("当前工具已点赞，请勿重复操作");
        }
        toolStarRepository.insert(toolId, currentUser.id());
        toolRepository.incrementStarCount(toolId);
    }

    @Transactional
    public void unstarTool(Long toolId, CurrentUser currentUser) {
        assertToolExists(toolId);
        if (!toolStarRepository.exists(toolId, currentUser.id())) {
            throw new BadRequestException("当前工具尚未点赞");
        }
        toolStarRepository.delete(toolId, currentUser.id());
        toolRepository.decrementStarCount(toolId);
    }

    @Transactional
    public void favoriteTool(Long toolId, CurrentUser currentUser) {
        assertToolExists(toolId);
        if (toolFavoriteRepository.exists(toolId, currentUser.id())) {
            throw new BadRequestException("当前工具已收藏，请勿重复操作");
        }
        toolFavoriteRepository.insert(toolId, currentUser.id());
        toolRepository.incrementFavoriteCount(toolId);
    }

    @Transactional
    public void unfavoriteTool(Long toolId, CurrentUser currentUser) {
        assertToolExists(toolId);
        if (!toolFavoriteRepository.exists(toolId, currentUser.id())) {
            throw new BadRequestException("当前工具尚未收藏");
        }
        toolFavoriteRepository.delete(toolId, currentUser.id());
        toolRepository.decrementFavoriteCount(toolId);
    }

    public boolean hasStarred(Long toolId, Long userId) {
        return toolStarRepository.exists(toolId, userId);
    }

    public boolean hasFavorited(Long toolId, Long userId) {
        return toolFavoriteRepository.exists(toolId, userId);
    }

    public List<Long> listFavoriteToolIds(Long userId) {
        return toolFavoriteRepository.findFavoriteToolIdsByUserId(userId);
    }

    public List<ToolCommentResponse> listComments(Long toolId, CurrentUser currentUser) {
        assertToolExists(toolId);
        return toolCommentRepository.findVisibleByToolId(toolId).stream()
                .map(comment -> ToolCommentResponse.from(comment, canDelete(comment, currentUser)))
                .toList();
    }

    @Transactional
    public ToolCommentResponse createComment(Long toolId, CommentCreateRequest request, CurrentUser currentUser) {
        assertToolExists(toolId);
        String sanitized = commentSanitizer.sanitize(request.content());
        long commentId = toolCommentRepository.insert(toolId, currentUser.id(), sanitized, currentUser.id());
        toolRepository.incrementCommentCount(toolId);
        ToolCommentRecord comment = toolCommentRepository.findActiveById(commentId)
                .orElseThrow(() -> new NotFoundException("评论不存在"));
        return ToolCommentResponse.from(comment, true);
    }

    @Transactional
    public void deleteComment(Long toolId, Long commentId, CurrentUser currentUser) {
        assertToolExists(toolId);
        ToolCommentRecord comment = toolCommentRepository.findActiveById(commentId)
                .orElseThrow(() -> new NotFoundException("评论不存在"));
        if (!comment.toolId().equals(toolId)) {
            throw new NotFoundException("评论不存在");
        }
        if (!canDelete(comment, currentUser)) {
            throw new ForbiddenException("当前账号无权删除该评论");
        }
        toolCommentRepository.logicalDelete(commentId, currentUser.id());
        toolRepository.decrementCommentCount(toolId);
        if (currentUser.roleCodes().contains(RoleRepository.ADMIN) && !comment.userId().equals(currentUser.id())) {
            auditLogRepository.insert(new AuditLogEntry(
                    "COMMENT_DELETED_BY_ADMIN",
                    "COMMENT",
                    String.valueOf(commentId),
                    currentUser.id(),
                    currentUser.username(),
                    "deleted comment on tool " + toolId
            ));
        }
    }

    private boolean canDelete(ToolCommentRecord comment, CurrentUser currentUser) {
        return comment.userId().equals(currentUser.id()) || currentUser.roleCodes().contains(RoleRepository.ADMIN);
    }

    private void assertToolExists(Long toolId) {
        toolRepository.findActiveById(toolId)
                .orElseThrow(() -> new NotFoundException("工具不存在"));
    }
}
