import { Link } from 'react-router-dom'

import xsignLogo from '../../../assets/brand/xsign-logo-login.png'
import loginFormBackground from '../../../assets/login/login-fundo-form.png'
import loginPanelImage from '../../../assets/login/xsign-login.png'
import { LoginForm } from '../components/login-form'

export function LoginPage() {
  return (
    <main className="grid min-h-screen bg-surface-page text-ink [min-height:100dvh] lg:grid-cols-[1.1fr_0.9fr]">
      <section
        aria-label="Painel visual do Xsign"
        className="relative hidden min-h-screen overflow-hidden bg-brand-900 [min-height:100dvh] lg:block"
      >
        <img
          src={loginPanelImage}
          alt=""
          className="absolute inset-0 h-full w-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-brand-900/35 via-brand-900/8 to-brand-900/16" />

        <div className="absolute left-8 top-[18%] max-w-lg 2xl:left-16 2xl:top-[22%] 2xl:max-w-xl">
          <p className="mb-4 text-xs font-semibold uppercase tracking-[0.24em] text-blue-100/68 2xl:mb-5">
            Gestão de assinaturas digitais
          </p>

          <h2 className="text-3xl font-semibold leading-tight tracking-tight text-white 2xl:text-5xl">
            Assinaturas digitais com rastreabilidade completa.
          </h2>

          <p className="mt-5 max-w-md text-sm leading-6 text-blue-50/72 2xl:mt-6">
            Controle documentos, signatários e evidências em um fluxo seguro,
            organizado e pronto para auditoria.
          </p>
        </div>

        <div className="absolute bottom-8 left-8 right-8 flex max-w-xl flex-wrap gap-3 2xl:bottom-12 2xl:left-16 2xl:right-auto">
          <span className="inline-flex items-center gap-2 rounded-full border border-white/14 bg-white/[0.09] px-4 py-2.5 text-xs font-medium text-blue-50/88 shadow-[0_18px_45px_rgba(0,0,0,0.22)] ring-1 ring-white/5 backdrop-blur-xl">
            <span className="size-1.5 rounded-full bg-blue-200 shadow-[0_0_14px_rgba(191,219,254,0.9)]" />
            Documentos centralizados
          </span>
          <span className="inline-flex items-center gap-2 rounded-full border border-white/14 bg-white/[0.09] px-4 py-2.5 text-xs font-medium text-blue-50/88 shadow-[0_18px_45px_rgba(0,0,0,0.22)] ring-1 ring-white/5 backdrop-blur-xl">
            <span className="size-1.5 rounded-full bg-blue-200 shadow-[0_0_14px_rgba(191,219,254,0.9)]" />
            Evidências auditáveis
          </span>
          <span className="inline-flex items-center gap-2 rounded-full border border-white/14 bg-white/[0.09] px-4 py-2.5 text-xs font-medium text-blue-50/88 shadow-[0_18px_45px_rgba(0,0,0,0.22)] ring-1 ring-white/5 backdrop-blur-xl">
            <span className="size-1.5 rounded-full bg-blue-200 shadow-[0_0_14px_rgba(191,219,254,0.9)]" />
            Fluxos seguros
          </span>
        </div>
      </section>

      <section
        aria-labelledby="login-title"
        className="relative flex min-h-screen items-center justify-center overflow-x-hidden overflow-y-auto bg-surface-page px-4 py-4 [min-height:100dvh] sm:px-6 sm:py-6 lg:px-6 xl:px-8 2xl:px-14"
      >
        <img
          src={loginFormBackground}
          alt=""
          className="absolute inset-0 h-full w-full object-cover object-right-bottom sm:object-right"
        />
        <div className="absolute inset-0 bg-white/35" />

        <div className="relative w-full max-w-[430px] rounded-[20px] border border-white/80 bg-white/88 px-4 py-5 shadow-[0_24px_76px_rgba(15,23,42,0.13)] backdrop-blur-xl sm:max-w-[460px] sm:rounded-[24px] sm:px-6 sm:py-6 md:px-7 md:py-7 xl:max-w-[470px] 2xl:max-w-[520px] 2xl:rounded-[28px] 2xl:px-10 2xl:py-10">
          <div className="mb-4 flex justify-center sm:mb-5 2xl:mb-7">
            <img
              src={xsignLogo}
              alt="Xsign"
              className="w-44 max-w-full sm:w-48 xl:w-52 2xl:w-56"
            />
          </div>

          <div className="text-center">
            <h1
              id="login-title"
              className="login-reveal-block text-2xl font-semibold tracking-tight text-ink [--login-reveal-y:84px] [animation-delay:150ms] sm:text-[1.75rem] 2xl:text-3xl"
            >
              Acesse sua conta
            </h1>

            <p className="login-reveal-block mt-2 text-sm leading-5 text-muted [--login-reveal-y:60px] [animation-delay:220ms] sm:mt-3 sm:leading-6">
              Entre com seus dados para continuar no Xsign.
            </p>
          </div>

          <LoginForm />

          <p className="mt-5 text-center text-xs leading-5 text-muted">
            Ao acessar, você concorda com nossa{' '}
            <Link
              className="font-medium text-brand-500 transition-colors hover:text-brand-600"
              to="/politica-de-privacidade"
            >
              Política de Privacidade
            </Link>
            .
          </p>
        </div>
      </section>
    </main>
  )
}
