package br.com.cortex.sign.modules.usuario.mapper;

import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.usuario.dto.request.AtualizarUsuarioRequest;
import br.com.cortex.sign.modules.usuario.dto.request.CriarUsuarioRequest;
import br.com.cortex.sign.modules.usuario.dto.response.UsuarioResponse;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public Usuario toEntity(CriarUsuarioRequest request, Organizacao organizacao, String senhaHash) {
        Usuario usuario = new Usuario();
        usuario.setOrganizacao(organizacao);
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenhaHash(senhaHash);
        usuario.setPerfil(request.perfil());
        usuario.setAtivo(true);
        return usuario;
    }

    public void updateEntity(Usuario usuario, AtualizarUsuarioRequest request, Organizacao organizacao) {
        usuario.setOrganizacao(organizacao);
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setPerfil(request.perfil());
        usuario.setAtivo(request.ativo());
    }

    public UsuarioResponse toResponse(Usuario usuario) {
        Organizacao organizacao = usuario.getOrganizacao();

        return new UsuarioResponse(
                usuario.getId(),
                organizacao != null ? organizacao.getId() : null,
                organizacao != null ? organizacao.getNome() : null,
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getAtivo(),
                usuario.getCriadoEm(),
                usuario.getAtualizadoEm()
        );
    }
}
