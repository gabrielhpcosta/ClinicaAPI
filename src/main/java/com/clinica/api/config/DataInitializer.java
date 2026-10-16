package com.clinica.api.config;

import com.clinica.api.entities.Usuario;
import com.clinica.api.enums.Role;
import com.clinica.api.repositories.UsuarioRepository;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository usuarioRepository,
                                   PasswordEncoder passwordEncoder,
                                   Dotenv dotenv) {
        return args -> {
            // Só cria o administrador se o banco ainda não tiver nenhum usuário
            if (usuarioRepository.count() > 0) {
                return;
            }

            String email = dotenv.get("ADMIN_EMAIL");
            String senha = dotenv.get("ADMIN_PASSWORD");

            if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
                log.warn("Banco sem usuários, mas ADMIN_EMAIL e ADMIN_PASSWORD não estão no .env. O administrador inicial não foi criado.");
                return;
            }

            Usuario admin = new Usuario();
            admin.setNome("Administrador");
            admin.setEmail(email);
            admin.setSenha(passwordEncoder.encode(senha));
            admin.setRole(Role.ADMIN);
            admin.setAtivo(true);

            usuarioRepository.save(admin);

            log.info("Administrador inicial criado: {}", email);
        };
    }
}