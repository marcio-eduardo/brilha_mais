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
        // Criar ou Atualizar usuário master '72916'
        Tecnico admin = tecnicoRepository.findByMatricula("72916").orElse(null);
        if (admin == null) {
            admin = Tecnico.builder()
                    .matricula("72916")
                    .nomeCompleto("Administrador Master")
                    .ativo(true)
                    .build();
        }

        // LIMPEZA TEMPORÁRIA DE TESTE (Para limpar a matrícula do Marcelo Ladi)
        var marcelo = tecnicoRepository.findByMatricula("71066").orElse(null);
        if (marcelo != null) {
            marcelo.setMatricula(null);
            marcelo.setSenha(null);
            marcelo.setIsPrimeiroAcesso(true);
            tecnicoRepository.save(marcelo);
            System.out.println("🔄 Matrícula do Marcelo Ladi (71066) limpa para testes.");
        }
        
        // Forçar as credenciais e cargo de admin para o 72916
        admin.setSenha(passwordEncoder.encode("admin"));
        admin.setCargo("Administrador");
        admin.setIsPrimeiroAcesso(false);
        tecnicoRepository.save(admin);
        System.out.println("✅ Usuário Master configurado com sucesso! [Login: 72916 | Senha: admin]");
    }
}
