// espelham os records do backend (br.com.zyra.dto)
// os nomes dos campos são o contrato JSON: não renomear

// o que o frontend envia no POST /contatos
export interface ContatoEntradaDTO {
  nome: string
  sobrenome: string
  email: string
  // opcional: null quando a pessoa não informa
  telefone: string | null
  mensagem: string
  // campo isca: escondido de pessoas, só robôs preenchem (o backend recusa se vier com texto)
  site: string
}

// o que a API devolve depois de gravar a mensagem
export interface ContatoSaidaDTO {
  idContato: number
  nome: string
  sobrenome: string
  email: string
  telefone: string | null
  mensagem: string
  // data e hora no formato ISO, ex.: "2026-11-18T14:30:00"
  dataEnvio: string
}

// aviso mostrado depois do último envio; null quando não há aviso na tela
export interface RetornoEnvio {
  tipo: 'sucesso' | 'erro'
  texto: string
}
