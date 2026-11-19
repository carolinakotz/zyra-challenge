package br.com.zyra.resource;

import br.com.zyra.bo.AutenticacaoBO;
import br.com.zyra.dto.LoginEntradaDTO;
import br.com.zyra.dto.LoginSaidaDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// endpoint de login; público no SegurancaConfig e limitado a 5 tentativas a cada 15 minutos
// por IP no LimiteRequisicoesFilter
@RestController
@RequestMapping("/autenticacao")
public class AutenticacaoResource {

    private final AutenticacaoBO autenticacaoBO;

    public AutenticacaoResource(AutenticacaoBO autenticacaoBO) {
        this.autenticacaoBO = autenticacaoBO;
    }

    // POST /autenticacao/login -> 200 com token, perfil e expiraEm
    // e-mail ou senha errados -> 401 "E-mail ou senha inválidos." (TratadorGlobalExceptions)
    @PostMapping("/login")
    public ResponseEntity<LoginSaidaDTO> fazerLogin(@RequestBody @Valid LoginEntradaDTO dto) {
        return ResponseEntity.ok(autenticacaoBO.fazerLogin(dto));
    }
}
