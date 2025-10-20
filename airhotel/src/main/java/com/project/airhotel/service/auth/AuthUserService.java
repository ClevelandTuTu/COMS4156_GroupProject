package com.project.airhotel.service.auth;

import com.project.airhotel.model.Users;
import com.project.airhotel.repository.UsersRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthUserService {

  private final UsersRepository usersRepository;

  public AuthUserService(UsersRepository usersRepository) {
    this.usersRepository = usersRepository;
  }

  /**
   * 按 email 查找用户，不存在就创建一条（仅设置 email / name），返回我方 userId。
   */
  @Transactional
  public Long findOrCreateByEmail(String email, @Nullable String name) {
    if (email == null || email.isBlank()) {
      throw new IllegalStateException("OAuth2 principal has no email.");
    }
    Users user = usersRepository.findByEmail(email)
        .map(u -> {
          if (name != null && !name.equals(u.getName())) {
            u.setName(name);
          }
          return u;
        })
        .orElseGet(() -> {
          Users u = new Users();
          u.setEmail(email);
          u.setName(name);
          return usersRepository.save(u);
        });
    return user.getId();
  }
}
