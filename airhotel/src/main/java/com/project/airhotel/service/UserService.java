package com.project.airhotel.service;

import com.project.airhotel.repository.UsersRepository;
import com.project.airhotel.model.Users;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Ziyang Su
 * @version 1.0.0
 */
@Service
public class UserService {
    private final UsersRepository userRepository;

    public UserService(UsersRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<Users> getById(Long id) {
        return userRepository.findById(id);
    }
}
