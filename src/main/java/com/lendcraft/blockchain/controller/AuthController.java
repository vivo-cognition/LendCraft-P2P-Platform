package com.lendcraft.blockchain.controller;

import com.lendcraft.blockchain.dto.UserLoginDto;
import com.lendcraft.blockchain.dto.UserRegisterDto;
import com.lendcraft.blockchain.dto.UpdateProfileDto;
import com.lendcraft.blockchain.dto.UserResponseDto;
import com.lendcraft.blockchain.entity.UserEntity;
import com.lendcraft.blockchain.repository.UserRepository;
import com.lendcraft.blockchain.model.CryptoUtils;
import com.lendcraft.blockchain.security.JwtUtils; // Наш обновленный бин
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.mindrot.jbcrypt.BCrypt; // Импорт шифратора паролей
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public AuthController(UserRepository userRepository, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegisterDto dto) {
        if (userRepository.existsById(dto.getPassportNumber())) {
            return ResponseEntity.badRequest().body("Пользователь с таким паспортом уже существует!");
        }
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Этот логин уже занят!");
        }

        String publicKeyStr;
        String privateKeyStr;
        try {
            KeyPair keyPair = com.lendcraft.blockchain.model.CryptoUtils.generateKeyPair();
            publicKeyStr = com.lendcraft.blockchain.model.CryptoUtils.keyToString(keyPair.getPublic());
            privateKeyStr = com.lendcraft.blockchain.model.CryptoUtils.keyToString(keyPair.getPrivate());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ошибка генерации крипто-кошелька: " + e.getMessage());
        }

        UserEntity newUser = new UserEntity();
        newUser.setPassportNumber(dto.getPassportNumber());
        newUser.setUsername(dto.getUsername());

        String hashedPw = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt());
        newUser.setPassword(hashedPw);

        newUser.setTrustRating(500);
        newUser.setFiatBalance(BigDecimal.ZERO);

        newUser.setPublicKey(publicKeyStr);

        userRepository.save(newUser);

        return ResponseEntity.ok(Map.of(
                "message", "Пользователь успешно зарегистрирован!",
                "publicKey", publicKeyStr,
                "privateKey", privateKeyStr
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody UserLoginDto dto) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(dto.getUsername());

        if (userOpt.isEmpty() || !BCrypt.checkpw(dto.getPassword(), userOpt.get().getPassword())) {
            return ResponseEntity.badRequest().body("Неверный логин или пароль!");
        }

        UserEntity user = userOpt.get();
        String realJwtToken = jwtUtils.generateToken(user.getUsername(), user.getPassportNumber());

        ResponseCookie cookie = ResponseCookie.from("accessToken", realJwtToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Lax")
                .build();

        UserResponseDto response = new UserResponseDto(
                user.getPassportNumber(), user.getUsername(), user.getTrustRating(),
                user.getFiatBalance(), user.getPublicKey(), user.getEmail(), user.getBio()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    try {
                        String token = cookie.getValue();
                        String username = jwtUtils.validateTokenAndGetUsername(token);

                        UserEntity user = userRepository.findByUsername(username).orElse(null);
                        if (user != null) {
                            UserResponseDto response = new UserResponseDto(
                                    user.getPassportNumber(), user.getUsername(), user.getTrustRating(),
                                    user.getFiatBalance(), user.getPublicKey(), user.getEmail(), user.getBio()
                            );
                            return ResponseEntity.ok(response);
                        }
                    } catch (TokenExpiredException e) {
                        return createLogoutResponse("Сессия истекла. Войдите заново.");
                    } catch (JWTVerificationException e) {
                        return createLogoutResponse("Неверный токен авторизации.");
                    }
                }
            }
        }
        return ResponseEntity.status(401).body("Пользователь не авторизован");
    }

    @PostMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileDto dto, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return ResponseEntity.status(401).body("Не авторизован");

        for (Cookie cookie : cookies) {
            if ("accessToken".equals(cookie.getName())) {
                try {
                    String token = cookie.getValue();
                    String username = jwtUtils.validateTokenAndGetUsername(token);

                    UserEntity user = userRepository.findByUsername(username).orElse(null);
                    if (user == null) return ResponseEntity.badRequest().body("Пользователь не найден");

                    user.setEmail(dto.getEmail());
                    user.setBio(dto.getBio());

                    if (dto.getNewPassword() != null && !dto.getNewPassword().trim().isEmpty()) {
                        user.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
                    }

                    userRepository.save(user);

                    UserResponseDto response = new UserResponseDto(
                            user.getPassportNumber(), user.getUsername(), user.getTrustRating(),
                            user.getFiatBalance(), user.getPublicKey(), user.getEmail(), user.getBio()
                    );
                    return ResponseEntity.ok(response);

                } catch (JWTVerificationException e) {
                    return createLogoutResponse("Ошибка безопасности. Сессия сброшена.");
                }
            }
        }
        return ResponseEntity.status(401).body("Не авторизован");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(false) // false для localhost
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Вы успешно вышли из системы.");
    }

    private ResponseEntity<?> createLogoutResponse(String message) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        return ResponseEntity.status(401)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(message);
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteCurrentAccount(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return ResponseEntity.status(401).body("Не авторизован");

        for (Cookie cookie : cookies) {
            if ("accessToken".equals(cookie.getName())) {
                try {
                    String token = cookie.getValue();
                    String username = jwtUtils.validateTokenAndGetUsername(token);

                    Optional<UserEntity> userOpt = userRepository.findByUsername(username);
                    if (userOpt.isEmpty()) {
                        return ResponseEntity.badRequest().body("Пользователь не найден");
                    }
                    userRepository.delete(userOpt.get());
                    return createLogoutResponse("Ваш аккаунт и крипто-кошелек успешно удалены из блокчейн-системы.");

                } catch (JWTVerificationException e) {
                    return createLogoutResponse("Ошибка авторизации.");
                }
            }
        }
        return ResponseEntity.status(401).body("Не авторизован");
    }

    @PostMapping("/fiat/deposit")
    public ResponseEntity<?> depositFiat(@RequestParam String passportNumber, @RequestParam BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Сумма пополнения должна быть больше нуля!");
        }

        Optional<UserEntity> userOpt = userRepository.findById(passportNumber);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Пользователь с таким паспортом не найден!");
        }

        UserEntity user = userOpt.get();
        user.setFiatBalance(user.getFiatBalance().add(amount));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "passportNumber", user.getPassportNumber(),
                "username", user.getUsername(),
                "trustRating", user.getTrustRating(),
                "fiatBalance", user.getFiatBalance(),
                "publicKey", user.getPublicKey(),
                "email", user.getEmail() != null ? user.getEmail() : "",
                "bio", user.getBio() != null ? user.getBio() : ""
        ));
    }
}
