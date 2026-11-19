import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter, RouterProvider } from 'react-router'
import './index.css'
import App from './App.tsx'
import PaginaInicio from './pages/PaginaInicio'
import PaginaSobre from './pages/PaginaSobre'
import PaginaMissoes from './pages/PaginaMissoes'
import PaginaIntegrantes from './pages/PaginaIntegrantes'
import PaginaFaq from './pages/PaginaFaq'
import PaginaContato from './pages/PaginaContato'
import PaginaCadastroMissao from './pages/PaginaCadastroMissao'
import PaginaDetalheMissao from './pages/PaginaDetalheMissao'
import PaginaEdicaoMissao from './pages/PaginaEdicaoMissao'
import PaginaErro from './pages/PaginaErro'
import PaginaLogin from './pages/PaginaLogin'
import PaginaContatosAdmin from './pages/PaginaContatosAdmin'
import RotaAdmin from './components/RotaAdmin'

// App é o layout (cabeçalho + rodapé); as rotas filhas aparecem no <Outlet /> dele
const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    // mostrado no lugar de App quando a rota não existe ou uma página lança erro
    errorElement: <PaginaErro />,
    children: [
      // cada página controla a própria largura e os próprios espaçamentos
      { index: true, element: <PaginaInicio /> },
      { path: 'sobre', element: <PaginaSobre /> },
      { path: 'missoes', element: <PaginaMissoes /> },
      { path: 'integrantes', element: <PaginaIntegrantes /> },
      { path: 'faq', element: <PaginaFaq /> },
      { path: 'contato', element: <PaginaContato /> },
      // sem link no menu: a equipe acessa digitando /login
      { path: 'login', element: <PaginaLogin /> },
      { path: 'missoes/:id', element: <PaginaDetalheMissao /> },
      // só para admin (cadastro e edição de missão, mensagens de contato): sem sessão, o RotaAdmin manda para o /login
      // ("nova" é texto fixo, então continua com prioridade sobre ":id")
      {
        element: <RotaAdmin />,
        children: [
          { path: 'missoes/nova', element: <PaginaCadastroMissao /> },
          { path: 'missoes/:id/editar', element: <PaginaEdicaoMissao /> },
          { path: 'admin/contatos', element: <PaginaContatosAdmin /> },
        ],
      },
    ],
  },
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
)
