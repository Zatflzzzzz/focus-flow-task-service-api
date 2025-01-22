package org.myProject.focus.flow.service.api.controllers.helpers;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Objects;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
@Transactional
public class ValidateRequestsHelper {

    public void verifyingUserAccessToProject(Long projectOwnerId, Long currentUserId) {

        if (!Objects.equals(projectOwnerId, currentUserId)) {
            throw new CustomAppException(
                    HttpStatus.FORBIDDEN,
                    "You does not have access to this project"
            );
        }
    }
}
