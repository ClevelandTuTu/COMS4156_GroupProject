package com.project.airhotel.service.auth;

import com.project.airhotel.model.Users;
import com.project.airhotel.repository.UsersRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthUserServiceTest {

  @Mock
  UsersRepository usersRepository;

  @InjectMocks
  AuthUserService authUserService;

  @Test
  void findOrCreateByEmail_nullEmail_throws() {
    // Expect IllegalStateException for null email
    assertThrows(IllegalStateException.class, () -> authUserService.findOrCreateByEmail(null, "Alice"));
  }

  @Test
  void findOrCreateByEmail_blankEmail_throws() {
    // Expect IllegalStateException for blank email
    assertThrows(IllegalStateException.class, () -> authUserService.findOrCreateByEmail("  ", "Alice"));
  }

  @Test
  void findOrCreateByEmail_existingUser_sameName_returnsId_withoutSaving() {
    Users existing = new Users();
    existing.setId(42L);
    existing.setEmail("a@b.com");
    existing.setName("Alice");

    when(usersRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));

    Long id = authUserService.findOrCreateByEmail("a@b.com", "Alice");

    assertEquals(42L, id);
    // Should not create/save when user exists with same name
    verify(usersRepository, never()).save(any());
    assertEquals("Alice", existing.getName());
  }

  @Test
  void findOrCreateByEmail_existingUser_differentName_updatesInMemory_returnsId() {
    Users existing = new Users();
    existing.setId(7L);
    existing.setEmail("x@y.com");
    existing.setName("OldName");

    when(usersRepository.findByEmail("x@y.com")).thenReturn(Optional.of(existing));

    Long id = authUserService.findOrCreateByEmail("x@y.com", "NewName");

    assertEquals(7L, id);
    // Name is updated in-memory (method does not force a save on rename)
    assertEquals("NewName", existing.getName());
    verify(usersRepository, never()).save(any());
  }

  @Test
  void findOrCreateByEmail_notFound_createsAndSaves_returnsId() {
    when(usersRepository.findByEmail("n@n.com")).thenReturn(Optional.empty());

    // Capture the entity passed to save
    ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
    Users saved = new Users();
    saved.setId(100L);
    saved.setEmail("n@n.com");
    saved.setName("Neo");

    when(usersRepository.save(any(Users.class))).thenReturn(saved);

    Long id = authUserService.findOrCreateByEmail("n@n.com", "Neo");

    verify(usersRepository).save(captor.capture());
    Users toSave = captor.getValue();
    assertEquals("n@n.com", toSave.getEmail());
    assertEquals("Neo", toSave.getName());
    assertEquals(100L, id);
  }
}
