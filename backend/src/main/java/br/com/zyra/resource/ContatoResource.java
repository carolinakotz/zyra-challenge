package br.com.zyra.resource;

import br.com.zyra.bo.ContatoBO;
import br.com.zyra.dto.ContatoEntradaDTO;
import br.com.zyra.dto.ContatoSaidaDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

// endpoints REST das mensagens de contato
// o POST é público (formulário do site). Ler e excluir é só para o admin: as mensagens têm
// dados pessoais (nome, e-mail, telefone). Quem bloqueia os outros perfis é o SegurancaConfig.
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

    // GET /contatos?pagina=0&tamanho=50 -> 200 com a lista (da mais recente para a mais antiga)
    // os dois parâmetros são opcionais; padrões e tamanho máximo ficam no ContatoBO
    @GetMapping
    public ResponseEntity<List<ContatoSaidaDTO>> listar(@RequestParam(required = false) Integer pagina,
                                                        @RequestParam(required = false) Integer tamanho) {
        return ResponseEntity.ok(contatoBO.listar(pagina, tamanho));
    }

    // GET /contatos/{id} -> 200 com a mensagem, ou 404 se não existir
    @GetMapping("/{id}")
    public ResponseEntity<ContatoSaidaDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(contatoBO.buscarPorId(id));
    }

    // DELETE /contatos/{id} -> 204 sem corpo, ou 404 se não existir
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        contatoBO.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
