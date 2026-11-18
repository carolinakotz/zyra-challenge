package br.com.zyra.resource;

import br.com.zyra.bo.ContatoBO;
import br.com.zyra.dto.ContatoEntradaDTO;
import br.com.zyra.dto.ContatoSaidaDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

// endpoint REST do formulário de contato
// de propósito, só existe o POST: as mensagens têm dados pessoais (nome, e-mail, telefone)
// e não podem ser lidas por uma rota pública. Um GET em /contatos responde 405.
@RestController
@RequestMapping("/contatos")
public class ContatoResource {

    private final ContatoBO contatoBO;

    public ContatoResource(ContatoBO contatoBO) {
        this.contatoBO = contatoBO;
    }

    // POST /contatos -> 201 com header Location e o contato gravado no corpo
    @PostMapping
    public ResponseEntity<ContatoSaidaDTO> cadastrar(@RequestBody @Valid ContatoEntradaDTO dto) {
        ContatoSaidaDTO criado = contatoBO.cadastrar(dto);

        // ex.: http://localhost:8080/contatos/42 (esse endereço só vai abrir para o admin)
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(criado.idContato())
                .toUri();

        return ResponseEntity.created(location).body(criado);
    }
}
