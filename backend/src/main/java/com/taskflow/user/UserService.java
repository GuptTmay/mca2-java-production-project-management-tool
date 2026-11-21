package com.taskflow.user;

import com.taskflow.common.dto.PageResponse;
import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.user.dto.UpdateProfileRequest;
import com.taskflow.user.dto.UpdateRoleRequest;
import com.taskflow.user.dto.UpdateUserEnabledRequest;
import com.taskflow.user.dto.UserResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public UserResponse getById(UUID id, User currentUser) {
        requireSelfOrProducer(id, currentUser);
        User target = findByIdOrThrow(id);
        return userMapper.toResponse(target);
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateProfileRequest request, User currentUser) {
        requireSelfOrProducer(id, currentUser);
        User target = findByIdOrThrow(id);
        target.setFirstName(request.firstName().trim());
        target.setLastName(request.lastName().trim());
        return userMapper.toResponse(userRepository.save(target));
    }

    public PageResponse<UserResponse> listUsers(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(userMapper::toResponse));
    }

    @Transactional
    public UserResponse updateRole(UUID id, UpdateRoleRequest request, User currentUser) {
        requireProducer(currentUser);
        if (currentUser.getId().equals(id) && request.role() != Role.PRODUCER) {
            throw new ForbiddenOperationException("You cannot remove your own Producer access");
        }
        User target = findByIdOrThrow(id);
        target.setRole(request.role());
        return userMapper.toResponse(userRepository.save(target));
    }

    @Transactional
    public UserResponse updateEnabled(UUID id, UpdateUserEnabledRequest request, User currentUser) {
        requireProducer(currentUser);
        if (currentUser.getId().equals(id) && !request.enabled()) {
            throw new ForbiddenOperationException("You cannot deactivate your own account");
        }
        User target = findByIdOrThrow(id);
        target.setEnabled(request.enabled());
        return userMapper.toResponse(userRepository.save(target));
    }

    private User findByIdOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private void requireSelfOrProducer(UUID targetId, User currentUser) {
        boolean isSelf = currentUser.getId().equals(targetId);
        boolean isProducer = currentUser.getRole() == Role.PRODUCER;
        if (!isSelf && !isProducer) {
            throw new ForbiddenOperationException("You can only access or modify your own profile");
        }
    }

    private void requireProducer(User currentUser) {
        if (currentUser.getRole() != Role.PRODUCER) {
            throw new ForbiddenOperationException("Only Producers can manage users");
        }
    }
}
