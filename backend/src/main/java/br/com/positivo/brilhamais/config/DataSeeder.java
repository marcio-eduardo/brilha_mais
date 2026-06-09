package br.com.positivo.brilhamais.config;

import br.com.positivo.brilhamais.models.Tecnico;
import br.com.positivo.brilhamais.repositories.TecnicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TecnicoRepository tecnicoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Garantir que todos os técnicos importados tenham uma senha padrão inicial (ex: 12345)
        var tecnicos = tecnicoRepository.findAll();
        for (Tecnico t : tecnicos) {
            if (t.getSenha() == null || t.getSenha().isEmpty()) {
                t.setSenha(passwordEncoder.encode("12345"));
                t.setIsPrimeiroAcesso(true);
                tecnicoRepository.save(t);
            }
        }

        // Criar ou Atualizar usuário master '72916'
        Tecnico admin = tecnicoRepository.findByMatricula("72916").orElse(null);
        if (admin == null) {
            admin = Tecnico.builder()
                    .matricula("72916")
                    .nomeCompleto("Administrador Master")
                    .ativo(true)
                    .build();
        }
        
        // Forçar as credenciais e cargo de admin para o 72916
        admin.setSenha(passwordEncoder.encode("admin"));
        admin.setCargo("Administrador");
        admin.setIsPrimeiroAcesso(false);
        tecnicoRepository.save(admin);
        System.out.println("✅ Usuário Master configurado com sucesso! [Login: 72916 | Senha: admin]");
    }
}
