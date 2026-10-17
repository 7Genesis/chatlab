package com.chatlab.core.auth.service;

import com.chatlab.core.user.domain.User;
import com.chatlab.core.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Hash de uma senha qualquer, usado quando o e-mail não existe: assim a resposta demora
    // quase o mesmo tempo nos dois casos e não revela se o e-mail está cadastrado.
    private final String hashFalso;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.hashFalso = passwordEncoder.encode("senha-que-ninguem-usa");
    }

    @Transactional(readOnly = true)
    public JwtService.Token login(String email, String senha) {
        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);

        String hash = user != null ? user.getPasswordHash() : hashFalso;
        boolean senhaConfere = passwordEncoder.matches(senha, hash);

        if (user == null || !senhaConfere || !Boolean.TRUE.equals(user.getActive())) {
            throw new InvalidCredentialsException();
        }
        return jwtService.gerar(user);
    }
}
