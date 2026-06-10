package br.com.positivo.brilhamais.services;

import br.com.positivo.brilhamais.dto.*;
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

    public VerificarTecnicoResponse verificarTecnico(VerificarTecnicoRequest request) {
        String estadoFormatado = request.getEstado().trim().toUpperCase();
        // Permite "RIO DE JANEIRO" virar "%RIO DE JANEIRO%" ou usar UF. O backend espera o UF no banco na maioria dos casos.
        // O ILIKE concat(...) lá cuida de nomes parciais. Se ele digitar "RJ", "RJ" funciona perfeitamente.
        
        var tecnico = repository.findByNomeAndEstadoNative(
                request.getNome().trim(), estadoFormatado
        ).orElseThrow(() -> new RuntimeException("O Nome ou Estado divergente. Procure seu gestor."));

        if (tecnico.getMatricula() != null && !tecnico.getMatricula().trim().isEmpty()) {
            throw new RuntimeException("O Nome ja possui senha cadastrada. Procure seu gestor em caso de duvidas");
        }

        return VerificarTecnicoResponse.builder()
                .id(tecnico.getIdTecnico())
                .nomeCompleto(tecnico.getNomeCompleto())
                .ctBase(tecnico.getCtBase())
                .build();
    }

    public AuthResponse vincularMatricula(VincularMatriculaRequest request) {
        var tecnico = repository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Técnico não encontrado"));

        // Verificar se a matrícula já existe (embora não deva ocorrer, é bom garantir o erro correto do BD caso ocorra)
        if (repository.findByMatricula(request.getMatricula()).isPresent()) {
            throw new RuntimeException("A matrícula informada já está em uso.");
        }

        tecnico.setMatricula(request.getMatricula());
        tecnico.setIsPrimeiroAcesso(true); // Manter true para forçar troca de senha no próximo passo
        repository.save(tecnico);

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
}
