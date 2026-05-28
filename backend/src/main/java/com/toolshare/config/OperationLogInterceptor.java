package com.toolshare.config;

import com.toolshare.log.repository.OperationLogRepository;
import com.toolshare.log.service.OperationLogPolicy;
import com.toolshare.model.OperationLogEntry;
import com.toolshare.security.CurrentUser;
import com.toolshare.security.CurrentUserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OperationLogInterceptor implements HandlerInterceptor {

    private final OperationLogRepository operationLogRepository;
    private final OperationLogPolicy operationLogPolicy;

    public OperationLogInterceptor(OperationLogRepository operationLogRepository,
                                   OperationLogPolicy operationLogPolicy) {
        this.operationLogRepository = operationLogRepository;
        this.operationLogPolicy = operationLogPolicy;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!operationLogPolicy.shouldLog(request)) {
            return;
        }
        CurrentUser currentUser = CurrentUserHolder.get();
        operationLogRepository.insert(new OperationLogEntry(
                operationLogPolicy.resolveOperation(request),
                operationLogPolicy.resolveModule(request),
                currentUser == null ? null : currentUser.id(),
                ex == null && response.getStatus() < 400,
                operationLogPolicy.resolveMessage(request, response.getStatus(), ex)
        ));
    }
}
