package br.com.positivo.brilhamais.services;

import br.com.positivo.brilhamais.dto.AuthRequest;
import br.com.positivo.brilhamais.dto.AuthResponse;
import br.com.positivo.brilhamais.models.Tecnico;
import br.com.positivo.brilhamais.repositories.TecnicoRepository;
import br.com.positivo.brilhamais.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TecnicoRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(AuthRequest request) {
        // Authenticate the user credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getMatricula(),
                        request.getSenha()
                )
        );

        var tecnico = repository.findByMatricula(request.getMatricula())
                .orElseThrow();

        var accessToken = jwtService.generateToken(tecnico);
        var refreshToken = jwtService.generateRefreshToken(tecnico);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isPrimeiroAcesso(tecnico.getIsPrimeiroAcesso())
                .nome(tecnico.getNomeCompleto())
                .cargo(tecnico.getCargo())
                .build();
    }

    public void changePassword(String matricula, String novaSenha) {
        var tecnico = repository.findByMatricula(matricula)
                .orElseThrow(() -> new RuntimeException("Técnico não encontrado"));

        tecnico.setSenha(passwordEncoder.encode(novaSenha));
        tecnico.setIsPrimeiroAcesso(false);
        repository.save(tecnico);
    }
}
