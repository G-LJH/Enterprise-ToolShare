package com.toolshare.workflow.web;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record WorkflowSubmissionRequest(
        @NotBlank(message = "工作流名称不能为空")
        String name,
        @NotBlank(message = "适用场景不能为空")
        String scenario,
        @NotBlank(message = "工作流描述不能为空")
        String description,
        @NotBlank(message = "步骤说明不能为空")
        String steps,
        List<Long> toolIds
) {
}
