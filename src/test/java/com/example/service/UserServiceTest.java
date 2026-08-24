package com.example.service;

import com.example.dataTransferObjects.LoginRequestDTO;
import com.example.dataTransferObjects.PasswordChangeDTO;
import com.example.dataTransferObjects.UserProfileDTO;
import com.example.dataTransferObjects.UserRegistrationDTO;
import com.example.dataTransferObjects.UserUpdateDTO;
import com.example.models.Role;
import com.example.models.User;
import com.example.repository.CategoryRepository;
import com.example.repository.ExpenseRepository;
import com.example.repository.IncomeRepository;
import com.example.repository.PasswordResetTokenRepository;
import com.example.repository.RefreshTokenRepository;
import com.example.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private IncomeRepository incomeRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("john", "john@test.com", "hashedPassword", new Date());
        user.setUser_id(1);
    }

    @Test
    void saveUser_success() {
        UserRegistrationDTO dto = new UserRegistrationDTO("john", "Password1", "john@test.com");

        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfileDTO result = userService.saveUser(dto);

        assertThat(result.getUsername()).isEqualTo("john");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void saveUser_emailAlreadyExists_throwsIllegalArgumentException() {
        UserRegistrationDTO dto = new UserRegistrationDTO("john", "Password1", "john@test.com");
        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.saveUser(dto))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void saveUser_usernameAlreadyExists_throwsIllegalArgumentException() {
        UserRegistrationDTO dto = new UserRegistrationDTO("john", "Password1", "john@test.com");
        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> userService.saveUser(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getRoleByUsername_returnsRole() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        Role role = userService.getRoleByUsername("john");

        assertThat(role).isEqualTo(Role.USER);
    }

    @Test
    void getRoleByUsername_notFound_throwsEntityNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getRoleByUsername("ghost"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getUserById_success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UserProfileDTO result = userService.getUserById(1);

        assertThat(result.getUsername()).isEqualTo("john");
    }

    @Test
    void getUserById_notFound_throwsEntityNotFoundException() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteUser_success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        userService.deleteUser(1);

        verify(refreshTokenRepository).deleteByUser(user);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_notFound_throwsRuntimeException() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(999))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void updateUserProfile_success() {
        UserUpdateDTO dto = new UserUpdateDTO("newUsername", "new@test.com");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("newUsername")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfileDTO result = userService.updateUserProfile(1, dto);

        assertThat(result).isNotNull();
    }

    @Test
    void updateUserProfile_usernameAlreadyTaken_throwsIllegalArgumentException() {
        UserUpdateDTO dto = new UserUpdateDTO("takenUsername", "john@test.com");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("takenUsername")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUserProfile(1, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateUserProfile_emailAlreadyTaken_throwsIllegalArgumentException() {
        UserUpdateDTO dto = new UserUpdateDTO("newUsername", "taken@test.com");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("newUsername")).thenReturn(false);
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUserProfile(1, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateUserProfile_invalidEmailFormat_throwsIllegalArgumentException() {
        UserUpdateDTO dto = new UserUpdateDTO("newUsername", "not-an-email");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("newUsername")).thenReturn(false);
        when(userRepository.existsByEmail("not-an-email")).thenReturn(false);

        assertThatThrownBy(() -> userService.updateUserProfile(1, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatePassword_success() {
        PasswordChangeDTO dto = new PasswordChangeDTO("oldPassword", "newPassword123");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newHashed");

        userService.updatePassword(1, dto);

        verify(userRepository).save(user);
        assertThat(user.getPassword_hash()).isEqualTo("newHashed");
    }

    @Test
    void updatePassword_wrongCurrentPassword_throwsSecurityException() {
        PasswordChangeDTO dto = new PasswordChangeDTO("wrongOld", "newPassword123");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongOld", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.updatePassword(1, dto))
                .isInstanceOf(SecurityException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success() {
        LoginRequestDTO dto = new LoginRequestDTO("john", "plainPassword");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plainPassword", "hashedPassword")).thenReturn(true);

        UserProfileDTO result = userService.login(dto);

        assertThat(result.getUsername()).isEqualTo("john");
    }

    @Test
    void login_userNotFound_throwsSecurityException_withGenericMessage() {
        LoginRequestDTO dto = new LoginRequestDTO("unknown", "pw");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(dto))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Usuario o contraseña incorrectos");
    }

    @Test
    void login_wrongPassword_throwsSecurityException_withSameGenericMessage() {
        // WHY: same message as "user not found" so the endpoint doesn't leak which usernames exist.
        LoginRequestDTO dto = new LoginRequestDTO("john", "wrongPassword");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(dto))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Usuario o contraseña incorrectos");
    }

    @Test
    void deleteAccount_success_cascadesAllUserData() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPassword", "hashedPassword")).thenReturn(true);

        userService.deleteAccount("john", "correctPassword");

        verify(expenseRepository).hardDeleteAllByUserId(1);
        verify(incomeRepository).hardDeleteAllByUserId(1);
        verify(categoryRepository).deleteByUserUserId(1);
        verify(refreshTokenRepository).deleteByUser(user);
        verify(passwordResetTokenRepository).deleteByUser(user);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteAccount_wrongPassword_throwsSecurityException_andDeletesNothing() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteAccount("john", "wrongPassword"))
                .isInstanceOf(SecurityException.class);

        verify(userRepository, never()).delete(any());
        verify(expenseRepository, never()).hardDeleteAllByUserId(anyInt());
    }

    @Test
    void deleteAccount_userNotFound_throwsEntityNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteAccount("ghost", "any"))
                .isInstanceOf(EntityNotFoundException.class);
    }
}