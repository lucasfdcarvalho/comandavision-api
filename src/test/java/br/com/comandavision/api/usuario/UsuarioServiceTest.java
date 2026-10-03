package br.com.comandavision.api.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.comandavision.api.usuario.dto.AlterarPapelRequest;
import br.com.comandavision.api.usuario.dto.CriarUsuarioRequest;
import br.com.comandavision.api.usuario.dto.UsuarioResponse;
import br.com.comandavision.api.usuario.supabase.SupabaseAdminClient;
import br.com.comandavision.api.usuario.supabase.UsuarioSupabase;

@ExtendWith(MockitoExtension.class)
public class UsuarioServiceTest {
    private static final UUID ID_DONO = UUID.randomUUID();
    private static final UUID ID_FUNCIONARIO = UUID.randomUUID();

    @Mock
    private SupabaseAdminClient supabaseAdminClient;

    @Mock
    private UsuarioPapelRepository usuarioPapelRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private static UsuarioSupabase usuario(UUID id, String nome, String email, OffsetDateTime banidoAte) {
        return new UsuarioSupabase(id, email, Map.of("nome", nome), banidoAte, null, OffsetDateTime.now());
    }

    @Test
    public void deveListarUsuariosComPapelOrdenadosPorNome() {
        when(supabaseAdminClient.listarUsuarios()).thenReturn(List.of(
                usuario(ID_FUNCIONARIO, "Maria", "maria@teste.com", OffsetDateTime.now().plusYears(10)),
                usuario(ID_DONO, "Ana", "ana@teste.com", null)));

        when(usuarioPapelRepository.findAll()).thenReturn(List.of(
                new UsuarioPapel(ID_DONO, PapelUsuario.DONO),
                new UsuarioPapel(ID_FUNCIONARIO, PapelUsuario.FUNCIONARIO)));

        List<UsuarioResponse> usuarios = usuarioService.listar();

        assertEquals("Ana", usuarios.get(0).nome());
        assertEquals(PapelUsuario.DONO, usuarios.get(0).papel());
        assertEquals("Maria", usuarios.get(1).nome());
        assertFalse(usuarios.get(1).ativo());
    }

    @Test
    public void deveCriarUsuarioComPapel() {
        CriarUsuarioRequest request = new CriarUsuarioRequest(" Maria ", " Maria@Teste.com ", "senha123", PapelUsuario.FUNCIONARIO);

        when(supabaseAdminClient.criarUsuario("Maria", "maria@teste.com", "senha123"))
                .thenReturn(usuario(ID_FUNCIONARIO, "Maria", "maria@teste.com", null));

        UsuarioResponse resposta = usuarioService.criar(request);

        assertEquals(ID_FUNCIONARIO, resposta.id());
        assertEquals(PapelUsuario.FUNCIONARIO, resposta.papel());
        verify(usuarioPapelRepository).saveAndFlush(any(UsuarioPapel.class));
    }

    @Test
    public void deveExcluirContaQuandoFalharAoSalvarPapel() {
        CriarUsuarioRequest request = new CriarUsuarioRequest("Maria", "maria@teste.com", "senha123", PapelUsuario.FUNCIONARIO);

        when(supabaseAdminClient.criarUsuario("Maria", "maria@teste.com", "senha123"))
                .thenReturn(usuario(ID_FUNCIONARIO, "Maria", "maria@teste.com", null));

        when(usuarioPapelRepository.saveAndFlush(any(UsuarioPapel.class)))
                .thenThrow(new DataIntegrityViolationException("falha"));

        assertThrows(DataIntegrityViolationException.class, () -> usuarioService.criar(request));

        verify(supabaseAdminClient).excluirUsuario(ID_FUNCIONARIO);
    }

    @Test
    public void naoDevePermitirAlterarOProprioPapel() {
        assertThrows(
                OperacaoUsuarioInvalidaException.class,
                () -> usuarioService.alterarPapel(ID_DONO, new AlterarPapelRequest(PapelUsuario.FUNCIONARIO), ID_DONO));

        verifyNoInteractions(supabaseAdminClient, usuarioPapelRepository);
    }

    @Test
    public void naoDeveRemoverOUltimoDono() {
        UUID outroDono = UUID.randomUUID();

        when(supabaseAdminClient.buscarUsuario(outroDono))
                .thenReturn(usuario(outroDono, "Carlos", "carlos@teste.com", null));

        when(usuarioPapelRepository.findById(outroDono))
                .thenReturn(Optional.of(new UsuarioPapel(outroDono, PapelUsuario.DONO)));

        when(usuarioPapelRepository.countByPapel(PapelUsuario.DONO)).thenReturn(1L);

        assertThrows(
                OperacaoUsuarioInvalidaException.class,
                () -> usuarioService.alterarPapel(outroDono, new AlterarPapelRequest(PapelUsuario.FUNCIONARIO), ID_DONO));

        verify(usuarioPapelRepository, never()).save(any(UsuarioPapel.class));
    }

    @Test
    public void deveAlterarPapelDeFuncionarioParaDono() {
        when(supabaseAdminClient.buscarUsuario(ID_FUNCIONARIO))
                .thenReturn(usuario(ID_FUNCIONARIO, "Maria", "maria@teste.com", null));

        UsuarioPapel papel = new UsuarioPapel(ID_FUNCIONARIO, PapelUsuario.FUNCIONARIO);
        when(usuarioPapelRepository.findById(ID_FUNCIONARIO)).thenReturn(Optional.of(papel));

        UsuarioResponse resposta = usuarioService.alterarPapel(
                ID_FUNCIONARIO, new AlterarPapelRequest(PapelUsuario.DONO), ID_DONO);

        assertEquals(PapelUsuario.DONO, resposta.papel());
        assertEquals(PapelUsuario.DONO, papel.getPapel());
        verify(usuarioPapelRepository).save(papel);
    }

    @Test
    public void naoDevePermitirDesativarAPropriaConta() {
        assertThrows(
                OperacaoUsuarioInvalidaException.class,
                () -> usuarioService.desativar(ID_DONO, ID_DONO));

        verifyNoInteractions(supabaseAdminClient);
    }

    @Test
    public void deveDesativarUsuario() {
        when(supabaseAdminClient.buscarUsuario(ID_FUNCIONARIO))
                .thenReturn(usuario(ID_FUNCIONARIO, "Maria", "maria@teste.com", null));

        when(supabaseAdminClient.definirBloqueio(ID_FUNCIONARIO, true))
                .thenReturn(usuario(ID_FUNCIONARIO, "Maria", "maria@teste.com", OffsetDateTime.now().plusYears(100)));

        when(usuarioPapelRepository.findById(ID_FUNCIONARIO))
                .thenReturn(Optional.of(new UsuarioPapel(ID_FUNCIONARIO, PapelUsuario.FUNCIONARIO)));

        UsuarioResponse resposta = usuarioService.desativar(ID_FUNCIONARIO, ID_DONO);

        assertFalse(resposta.ativo());
    }

    @Test
    public void deveLancarExcecaoAoDesativarUsuarioInexistente() {
        when(supabaseAdminClient.buscarUsuario(ID_FUNCIONARIO)).thenReturn(null);

        assertThrows(
                UsuarioNaoEncontradoException.class,
                () -> usuarioService.desativar(ID_FUNCIONARIO, ID_DONO));
    }
}
