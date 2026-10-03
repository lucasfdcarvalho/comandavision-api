package br.com.comandavision.api.usuario;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.com.comandavision.api.usuario.dto.AlterarPapelRequest;
import br.com.comandavision.api.usuario.dto.CriarUsuarioRequest;
import br.com.comandavision.api.usuario.dto.UsuarioResponse;
import br.com.comandavision.api.usuario.supabase.SupabaseAdminClient;
import br.com.comandavision.api.usuario.supabase.UsuarioSupabase;

// Contas ficam no Supabase Auth; o papel fica em comandavision.usuarios_papeis.
// Sem @Transactional: as chamadas ao Supabase não participam da transação do banco.
@Service
public class UsuarioService {

    private final SupabaseAdminClient supabaseAdminClient;
    private final UsuarioPapelRepository usuarioPapelRepository;

    public UsuarioService(SupabaseAdminClient supabaseAdminClient, UsuarioPapelRepository usuarioPapelRepository) {
        this.supabaseAdminClient = supabaseAdminClient;
        this.usuarioPapelRepository = usuarioPapelRepository;
    }

    public List<UsuarioResponse> listar() {
        Map<UUID, PapelUsuario> papeis = usuarioPapelRepository.findAll().stream()
                .collect(Collectors.toMap(UsuarioPapel::getUsuarioId, UsuarioPapel::getPapel));

        return supabaseAdminClient.listarUsuarios().stream()
                .map(usuario -> UsuarioResponse.from(usuario, papeis.get(usuario.id())))
                .sorted(Comparator.comparing(
                        (UsuarioResponse usuario) -> usuario.nome() != null ? usuario.nome() : usuario.email(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public UsuarioResponse criar(CriarUsuarioRequest request) {
        UsuarioSupabase usuario = supabaseAdminClient.criarUsuario(
                request.nome().trim(),
                request.email().trim().toLowerCase(),
                request.senha());

        try {
            usuarioPapelRepository.saveAndFlush(new UsuarioPapel(usuario.id(), request.papel()));
        } catch (RuntimeException exception) {
            // Sem papel a conta não serve para nada: desfaz o cadastro no Supabase.
            supabaseAdminClient.excluirUsuario(usuario.id());
            throw exception;
        }

        return UsuarioResponse.from(usuario, request.papel());
    }

    public UsuarioResponse alterarPapel(UUID id, AlterarPapelRequest request, UUID idUsuarioLogado) {
        if (id.equals(idUsuarioLogado)) {
            throw new OperacaoUsuarioInvalidaException("Você não pode alterar o seu próprio papel");
        }

        UsuarioSupabase usuario = buscarExistente(id);

        UsuarioPapel papelUsuario = usuarioPapelRepository.findById(id)
                .orElseGet(() -> new UsuarioPapel(id, request.papel()));

        boolean removendoDono = papelUsuario.getPapel() == PapelUsuario.DONO && request.papel() != PapelUsuario.DONO;

        if (removendoDono && usuarioPapelRepository.countByPapel(PapelUsuario.DONO) <= 1) {
            throw new OperacaoUsuarioInvalidaException("É preciso manter pelo menos um usuário com o papel DONO");
        }

        papelUsuario.setPapel(request.papel());
        usuarioPapelRepository.save(papelUsuario);

        return UsuarioResponse.from(usuario, request.papel());
    }

    public UsuarioResponse desativar(UUID id, UUID idUsuarioLogado) {
        if (id.equals(idUsuarioLogado)) {
            throw new OperacaoUsuarioInvalidaException("Você não pode desativar a sua própria conta");
        }

        buscarExistente(id);

        return UsuarioResponse.from(supabaseAdminClient.definirBloqueio(id, true), buscarPapel(id));
    }

    public UsuarioResponse reativar(UUID id) {
        buscarExistente(id);

        return UsuarioResponse.from(supabaseAdminClient.definirBloqueio(id, false), buscarPapel(id));
    }

    private UsuarioSupabase buscarExistente(UUID id) {
        UsuarioSupabase usuario = supabaseAdminClient.buscarUsuario(id);

        if (usuario == null) {
            throw new UsuarioNaoEncontradoException(id);
        }

        return usuario;
    }

    private PapelUsuario buscarPapel(UUID id) {
        return usuarioPapelRepository.findById(id)
                .map(UsuarioPapel::getPapel)
                .orElse(null);
    }
}
