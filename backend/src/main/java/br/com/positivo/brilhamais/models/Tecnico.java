package br.com.positivo.brilhamais.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "tb_tecnico")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tecnico implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTecnico;

    @Column(name = "matricula", unique = true, length = 20)
    private String matricula;

    @Column(name = "cpf", unique = true, length = 20)
    private String cpf;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(name = "ct_base")
    private String ctBase;

    @Column(name = "cargo")
    private String cargo;

    @Column(name = "senha")
    private String senha;

    @Column(name = "ativo")
    private Boolean ativo = true;

    @Column(name = "is_primeiro_acesso")
    private Boolean isPrimeiroAcesso = true;

    // Métodos do UserDetails (Spring Security)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_TECNICO"));
    }

    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public String getUsername() {
        return this.matricula;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.ativo != null && this.ativo;
    }
}
