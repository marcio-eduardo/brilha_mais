package br.com.positivo.brilhamais.config;

import br.com.positivo.brilhamais.models.Tecnico;
import br.com.positivo.brilhamais.repositories.TecnicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import br.com.positivo.brilhamais.models.ApuracaoMensal;
import br.com.positivo.brilhamais.repositories.ApuracaoMensalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TecnicoRepository tecnicoRepository;
    private final ApuracaoMensalRepository apuracaoMensalRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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

        // ---------------------------------------------------------
        // BASE DE TESTES (Exibição RJ)
        // ---------------------------------------------------------
        try {
            jdbcTemplate.execute("INSERT INTO tb_base_atp (ct_codigo, nome_atp, uf) VALUES ('RJ-TESTE', 'Base RJ Teste', 'RJ') ON CONFLICT DO NOTHING");
            System.out.println("✅ Base RJ-TESTE configurada para cadastro.");
        } catch (Exception e) {
            System.out.println("Base RJ-TESTE já existe ou não pôde ser criada.");
        }

        // Criar o técnico sem matrícula para ele poder se cadastrar
        Tecnico tecTeste = tecnicoRepository.findByNomeAndEstadoNative("Técnico Positivo", "RJ").orElse(null);
        if (tecTeste == null) {
            tecTeste = Tecnico.builder()
                    .matricula(null)
                    .cpf(null)
                    .nomeCompleto("Técnico Positivo")
                    .ativo(true)
                    .build();
            tecTeste.setCtBase("RJ-TESTE");
            tecTeste.setCargo("Técnico On-site");
            tecTeste.setIsPrimeiroAcesso(true);
            tecnicoRepository.save(tecTeste);
            System.out.println("✅ Técnico Positivo criado sem matrícula para teste de cadastro!");
        }

        // Se o técnico 00000 for criado pelo fluxo de cadastro, inserimos a apuração fictícia
        Tecnico tecCadastrado = tecnicoRepository.findByMatricula("00000").orElse(null);
        if (tecCadastrado != null) {
            LocalDate mesAtual = LocalDate.now().withDayOfMonth(1);
            ApuracaoMensal apuracaoTeste = apuracaoMensalRepository.findFirstByTecnicoIdTecnicoAndMesAno(tecCadastrado.getIdTecnico(), mesAtual).orElse(null);
            
            if (apuracaoTeste == null) {
                apuracaoTeste = ApuracaoMensal.builder()
                        .tecnico(tecCadastrado)
                        .mesAno(mesAtual)
                        .build();

                apuracaoTeste.setAtingimentoSla(new BigDecimal("1.0000"));
                apuracaoTeste.setPontosSla(20.0);
                apuracaoTeste.setAtingimentoReincidencia(new BigDecimal("0.0500"));
                apuracaoTeste.setPontosReincidencia(20.0);
                apuracaoTeste.setAtingimentoPecas(new BigDecimal("0.3000"));
                apuracaoTeste.setPontosPecas(25.0);
                apuracaoTeste.setAtingimentoNps(new BigDecimal("0.9000"));
                apuracaoTeste.setPontosNps(20.0);
                apuracaoTeste.setAtingimentoPerdidos(new BigDecimal("0.0000"));
                apuracaoTeste.setPontosPerdidos(10.0);

                apuracaoTeste.setPontuacaoTotal(new BigDecimal("95.00"));
                apuracaoTeste.setStatusElegibilidade(true);
                apuracaoTeste.setMotivoInelegibilidade(null);
                apuracaoTeste.setTotalChamados(42);
                apuracaoTeste.setDataCalculo(LocalDateTime.now());

                apuracaoMensalRepository.save(apuracaoTeste);
                System.out.println("✅ Apuração fictícia configurada para o técnico 00000 com sucesso!");
            }
        }
    }
}
