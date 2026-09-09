package com.minerva.infrastructure.rest.service;

import com.minerva.infrastructure.persistence.repository.JpaAppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private JpaAppUserRepository jpaAppUserRepository;

    public UserDetailsServiceImpl(JpaAppUserRepository jpaAppUserRepository) {
        this.jpaAppUserRepository = jpaAppUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return jpaAppUserRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario NO Encontrado!"));
    }
}
