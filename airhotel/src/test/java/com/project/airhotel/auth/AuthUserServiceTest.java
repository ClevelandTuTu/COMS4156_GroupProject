package com.project.airhotel.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.airhotel.user.domain.Users;
import com.project.airhotel.user.repository.UsersRepository;
import com.project.airhotel.user.service.AuthUserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthUserServiceTest {

  @Mock
  UsersRepository usersRepository;

  @InjectMocks
  AuthUserService authUserService;

  @Test
  void findOrCreateByEmail_nullEmail_throws() {
    // Expect IllegalStateException for null email
    assertThrows(IllegalStateException.class,
        () -> authUserService.findOrCreateByEmail(null, "Alice"));
  }

  @Test
  void findOrCreateByEmail_blankEmail_throws() {
    // Expect IllegalStateException for blank email
    assertThrows(IllegalStateException.class,
        () -> authUserService.findOrCreateByEmail("  ", "Alice"));
  }

  @Test
  void findOrCreateByEmail_existingUser_sameName_returnsId_withoutSaving() {
    final Users existing = new Users();
    existing.setId(42L);
    existing.setEmail("a@b.com");
    existing.setName("Alice");

    when(usersRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existing));

    final Long id = authUserService.findOrCreateByEmail("a@b.com",
        "Alice");

    assertEquals(42L, id);
    // Should not create/save when user exists with same name
    verify(usersRepository, never()).save(any());
    assertEquals("Alice", existing.getName());
  }

  @Test
  void findOrCreateByEmail_existingUser_differentName_updatesInMemory_returnsId() {
    final Users existing = new Users();
    existing.setId(7L);
    existing.setEmail("x@y.com");
    existing.setName("OldName");

    when(usersRepository.findByEmail("x@y.com"))
        .thenReturn(Optional.of(existing));

    final Long id = authUserService.findOrCreateByEmail("x@y.com",
        "NewName");

    assertEquals(7L, id);
    assertEquals("NewName", existing.getName());
    verify(usersRepository, never()).save(any());
  }

  @Test
  void findOrCreateByEmail_notFound_createsAndSaves_returnsId() {
    when(usersRepository.findByEmail("n@n.com")).thenReturn(Optional.empty());

    // Capture the entity passed to save
    final ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
    final Users saved = new Users();
    saved.setId(100L);
    saved.setEmail("n@n.com");
    saved.setName("Neo");

    when(usersRepository.save(any(Users.class))).thenReturn(saved);

    final Long id = authUserService.findOrCreateByEmail("n@n.com",
        "Neo");

    verify(usersRepository).save(captor.capture());
    final Users toSave = captor.getValue();
    assertEquals("n@n.com", toSave.getEmail());
    assertEquals("Neo", toSave.getName());
    assertEquals(100L, id);
  }

  @Test
  void findOrCreateByEmail_existingUser_nullStoredName_updatesToNewName_withoutSaving() {
    final Users existing = new Users();
    existing.setId(88L);
    existing.setEmail("edge@case.com");
    existing.setName(null);

    when(usersRepository.findByEmail("edge@case.com"))
        .thenReturn(Optional.of(existing));

    final Long id = authUserService.findOrCreateByEmail("edge@case.com",
        "Display");

    assertEquals(88L, id);
    assertEquals("Display", existing.getName());
    verify(usersRepository, never()).save(any());
  }

  @Test
  void findOrCreateByEmail_existingUser_blankProvidedName_overwritesName_withoutSaving() {
    final Users existing = new Users();
    existing.setId(9L);
    existing.setEmail("blank@name.com");
    existing.setName("WasName");

    when(usersRepository.findByEmail("blank@name.com"))
        .thenReturn(Optional.of(existing));

    final Long id = authUserService
        .findOrCreateByEmail("blank@name.com", "");

    assertEquals(9L, id);
    assertEquals("", existing.getName());
    verify(usersRepository, never()).save(any());
  }

  @Test
  void findOrCreateByEmail_notFound_createsWithNullName_returnsId() {
    when(usersRepository.findByEmail("new@none.com"))
        .thenReturn(Optional.empty());

    final ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
    final Users saved = new Users();
    saved.setId(501L);
    saved.setEmail("new@none.com");
    saved.setName(null);
    when(usersRepository.save(any(Users.class))).thenReturn(saved);

    final Long id = authUserService.findOrCreateByEmail("new@none.com",
        null);

    verify(usersRepository).save(captor.capture());
    final Users toSave = captor.getValue();
    assertEquals("new@none.com", toSave.getEmail());
    assertNull(toSave.getName());
    assertEquals(501L, id);
  }

  @Test
  void findOrCreateByEmail_emailWithSpaces_isNotTrimmed_andStillWorksWithExactMatch() {
    final String spaced = "  spaced@mail.com  ";
    final Users existing = new Users();
    existing.setId(321L);
    existing.setEmail(spaced);
    existing.setName("Spaced");

    when(usersRepository.findByEmail(spaced))
        .thenReturn(Optional.of(existing));

    final Long id = authUserService.findOrCreateByEmail(spaced, "Spaced");

    assertEquals(321L, id);
    verify(usersRepository, never()).save(any());
  }
}
