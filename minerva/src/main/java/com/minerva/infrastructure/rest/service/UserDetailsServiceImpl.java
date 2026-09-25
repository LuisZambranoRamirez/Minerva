package com.minerva.infrastructure.rest.service;

import com.minerva.domain.entities.user.AccountApprovalStatus;
import com.minerva.infrastructure.persistence.entity.AppUserEntity;
import com.minerva.infrastructure.persistence.repository.JpaAppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final JpaAppUserRepository jpaAppUserRepository;

    public UserDetailsServiceImpl(JpaAppUserRepository jpaAppUserRepository) {
        this.jpaAppUserRepository = jpaAppUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return jpaAppUserRepository.findById(username)
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario NO Encontrado!"));
    }

    private UserDetails toUserDetails(AppUserEntity entity) {
        AccountApprovalStatus status = entity.getApprovalStatus() == null
                ? AccountApprovalStatus.APPROVED
                : entity.getApprovalStatus();
        return User.withUsername(entity.getUserName())
                .password(entity.getPassword())
                .authorities("ROLE_" + entity.getPersonal().getRole().name())
                .disabled(!Boolean.TRUE.equals(entity.getActive())
                        || !Boolean.TRUE.equals(entity.getPersonal().getActive())
                        || status != AccountApprovalStatus.APPROVED)
                .build();
    }
}