import { Link } from 'react-router-dom'

import xsignLogo from '../../../assets/brand/xsign-logo-login.png'

const sections = [
  {
    title: '1. Quem somos',
    content: [
      'O Cortex Sign, também identificado como Xsign, é uma plataforma para gestão, envio, validação e assinatura eletrônica de documentos.',
      'Esta Política de Privacidade explica como tratamos dados pessoais relacionados ao uso da plataforma, incluindo dados de usuários, organizações, signatários e evidências de assinatura.',
    ],
  },
  {
    title: '2. Dados que podemos coletar',
    content: [
      'Podemos coletar dados cadastrais, como nome, e-mail, CPF, telefone, organização vinculada e perfil de acesso.',
      'Também podemos tratar dados relacionados aos documentos, como título, arquivos enviados, signatários, status do fluxo, hashes, registros de auditoria, endereço IP, data e hora de eventos e evidências necessárias para comprovar a assinatura.',
    ],
  },
  {
    title: '3. Para que usamos os dados',
    content: [
      'Usamos os dados para autenticar usuários, gerenciar organizações, criar fluxos de assinatura, enviar códigos de confirmação, registrar evidências, validar documentos assinados e manter a segurança da plataforma.',
      'Também podemos usar informações técnicas para prevenir fraudes, investigar incidentes, cumprir obrigações legais e melhorar a estabilidade do serviço.',
    ],
  },
  {
    title: '4. Compartilhamento de dados',
    content: [
      'Podemos compartilhar dados apenas quando necessário para operar a plataforma, como serviços de armazenamento, envio de e-mail, envio de mensagens, infraestrutura, auditoria, suporte ou cumprimento de obrigação legal.',
      'Não vendemos dados pessoais.',
    ],
  },
  {
    title: '5. Segurança e evidências de assinatura',
    content: [
      'Aplicamos medidas técnicas e organizacionais para proteger os dados tratados na plataforma.',
      'As evidências de assinatura, como hashes, data e hora, IP, token de confirmação e registros de eventos, são mantidas para permitir validação, rastreabilidade e auditoria dos documentos assinados.',
    ],
  },
  {
    title: '6. Retenção dos dados',
    content: [
      'Mantemos os dados pelo tempo necessário para cumprir as finalidades da plataforma, obrigações legais, contratuais, regulatórias e necessidades de auditoria.',
      'Quando aplicável, dados podem ser eliminados, anonimizados ou bloqueados conforme solicitação válida do titular ou política interna de retenção.',
    ],
  },
  {
    title: '7. Direitos dos titulares',
    content: [
      'Nos termos da Lei Geral de Proteção de Dados Pessoais (LGPD), o titular pode solicitar confirmação de tratamento, acesso, correção, anonimização, bloqueio, eliminação, portabilidade, informações sobre compartilhamento e revisão de decisões automatizadas, quando aplicável.',
      'Solicitações serão analisadas conforme a legislação vigente, regras de segurança e necessidade de preservação de evidências legais de assinatura.',
    ],
  },
  {
    title: '8. Canais de contato',
    content: [
      'Para dúvidas, solicitações ou pedidos relacionados à privacidade, entre em contato pelo e-mail: cortex.admcloud@gmail.com.',
      'Ao usar a plataforma, você declara estar ciente desta Política de Privacidade.',
    ],
  },
]

export function PrivacyPolicyPage() {
  return (
    <main className="min-h-screen bg-surface-page px-4 py-8 text-ink sm:px-6 lg:px-8">
      <article className="mx-auto max-w-4xl overflow-hidden rounded-card border border-line bg-white shadow-card">
        <header className="border-b border-line px-5 py-6 text-center sm:px-8 sm:py-8">
          <img src={xsignLogo} alt="Xsign" className="mx-auto w-40 max-w-full sm:w-48" />
          <p className="mt-5 text-xs font-semibold uppercase tracking-[0.16em] text-brand-500">
            Privacidade e proteção de dados
          </p>
          <h1 className="mt-2 text-2xl font-semibold tracking-tight text-ink sm:text-3xl">
            Política de Privacidade
          </h1>
          <p className="mx-auto mt-3 max-w-2xl text-sm leading-6 text-muted">
            Esta política descreve como o Cortex Sign trata dados pessoais para permitir autenticação,
            gestão documental, assinatura eletrônica, validação e auditoria.
          </p>
          <p className="mt-3 text-xs text-muted">Última atualização: 30/07/2026</p>
        </header>

        <div className="grid gap-5 px-5 py-6 sm:px-8 sm:py-8">
          {sections.map((section) => (
            <section className="rounded-card border border-line bg-surface-page/55 p-4" key={section.title}>
              <h2 className="text-base font-semibold text-ink">{section.title}</h2>
              <div className="mt-3 grid gap-2 text-sm leading-6 text-muted">
                {section.content.map((paragraph) => (
                  <p key={paragraph}>{paragraph}</p>
                ))}
              </div>
            </section>
          ))}
        </div>

        <footer className="flex flex-col gap-3 border-t border-line px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-8">
          <p className="text-xs text-muted">Cortex Sign / Xsign</p>
          <Link className="text-sm font-medium text-brand-500 transition-colors hover:text-brand-600" to="/login">
            Voltar para o login
          </Link>
        </footer>
      </article>
    </main>
  )
}
