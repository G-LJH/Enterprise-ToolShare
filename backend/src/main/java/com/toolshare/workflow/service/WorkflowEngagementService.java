package com.toolshare.workflow.service;

import com.toolshare.exception.BadRequestException;
import com.toolshare.exception.NotFoundException;
import com.toolshare.security.CurrentUser;
import com.toolshare.workflow.repository.WorkflowFavoriteRepository;
import com.toolshare.workflow.repository.WorkflowRepository;
import com.toolshare.workflow.repository.WorkflowStarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkflowEngagementService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStarRepository workflowStarRepository;
    private final WorkflowFavoriteRepository workflowFavoriteRepository;

    public WorkflowEngagementService(WorkflowRepository workflowRepository,
                                     WorkflowStarRepository workflowStarRepository,
                                     WorkflowFavoriteRepository workflowFavoriteRepository) {
        this.workflowRepository = workflowRepository;
        this.workflowStarRepository = workflowStarRepository;
        this.workflowFavoriteRepository = workflowFavoriteRepository;
    }

    @Transactional
    public void starWorkflow(Long workflowId, CurrentUser currentUser) {
        assertWorkflowExists(workflowId);
        if (workflowStarRepository.exists(workflowId, currentUser.id())) {
            throw new BadRequestException("当前工作流已点赞，请勿重复操作");
        }
        workflowStarRepository.insert(workflowId, currentUser.id());
        workflowRepository.incrementStarCount(workflowId);
    }

    @Transactional
    public void unstarWorkflow(Long workflowId, CurrentUser currentUser) {
        assertWorkflowExists(workflowId);
        if (!workflowStarRepository.exists(workflowId, currentUser.id())) {
            throw new BadRequestException("当前工作流尚未点赞");
        }
        workflowStarRepository.delete(workflowId, currentUser.id());
        workflowRepository.decrementStarCount(workflowId);
    }

    @Transactional
    public void favoriteWorkflow(Long workflowId, CurrentUser currentUser) {
        assertWorkflowExists(workflowId);
        if (workflowFavoriteRepository.exists(workflowId, currentUser.id())) {
            throw new BadRequestException("当前工作流已收藏，请勿重复操作");
        }
        workflowFavoriteRepository.insert(workflowId, currentUser.id());
        workflowRepository.incrementFavoriteCount(workflowId);
    }

    @Transactional
    public void unfavoriteWorkflow(Long workflowId, CurrentUser currentUser) {
        assertWorkflowExists(workflowId);
        if (!workflowFavoriteRepository.exists(workflowId, currentUser.id())) {
            throw new BadRequestException("当前工作流尚未收藏");
        }
        workflowFavoriteRepository.delete(workflowId, currentUser.id());
        workflowRepository.decrementFavoriteCount(workflowId);
    }

    public boolean hasStarred(Long workflowId, Long userId) {
        return workflowStarRepository.exists(workflowId, userId);
    }

    public boolean hasFavorited(Long workflowId, Long userId) {
        return workflowFavoriteRepository.exists(workflowId, userId);
    }

    public List<Long> listFavoriteWorkflowIds(Long userId) {
        return workflowFavoriteRepository.findFavoriteWorkflowIdsByUserId(userId);
    }

    private void assertWorkflowExists(Long workflowId) {
        workflowRepository.findActiveById(workflowId)
                .orElseThrow(() -> new NotFoundException("工作流不存在"));
    }
}
