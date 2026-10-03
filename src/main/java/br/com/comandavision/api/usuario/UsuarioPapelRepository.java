package br.com.comandavision.api.usuario;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioPapelRepository extends JpaRepository<UsuarioPapel, UUID> {
    long countByPapel(PapelUsuario papel);
}
