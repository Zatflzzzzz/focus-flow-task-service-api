package org.myProject.focus.flow.service.api.services;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.myProject.focus.flow.service.api.exceptions.CustomAppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Objects;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
@Transactional
public class ValidateRequestsService {

    public void verifyingUserAccessToProject(Long projectOwnerId, Long currentUserId) {

        if (!Objects.equals(projectOwnerId, currentUserId)) {
            throw new CustomAppException(
                    HttpStatus.FORBIDDEN,
                    "You does not have access to this project"
            );
        }
    }
}
