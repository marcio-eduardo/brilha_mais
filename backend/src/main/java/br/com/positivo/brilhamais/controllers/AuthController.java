package br.com.positivo.brilhamais.controllers;

import br.com.positivo.brilhamais.dto.*;
import br.com.positivo.brilhamais.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Não autenticado");
        }
        
        // O username será a matrícula (pois mapeamos no UserDetails)
        String matricula = authentication.getName();
        authService.changePassword(matricula, request.getNovaSenha());
        
        return ResponseEntity.ok("Senha alterada com sucesso");
    }

    @PostMapping("/verificar-tecnico")
    public ResponseEntity<VerificarTecnicoResponse> verificarTecnico(@Valid @RequestBody VerificarTecnicoRequest request) {
        return ResponseEntity.ok(authService.verificarTecnico(request));
    }

    @PostMapping("/vincular-matricula")
    public ResponseEntity<AuthResponse> vincularMatricula(@Valid @RequestBody VincularMatriculaRequest request) {
        return ResponseEntity.ok(authService.vincularMatricula(request));
    }
}
