/*
 * Sessão e acesso — Goatilicious (HTTP Basic).
 * Duas zonas independentes: "cliente" (portal) e "staff" (painel). Cada uma guarda a sua sessão em
 * sessionStorage, por isso uma conta de cliente nunca abre o painel e o contrário também não.
 * Funciona como script clássico (admin) e como import de efeito (páginas do cliente com Vite).
 * Todo o texto do utilizador entra por textContent.
 */
(function () {
  'use strict';

  if (window.GoatAuth) return;

  var ZONAS = {
    cliente: { papel: 'CLIENTE', chave: 'goatilicious.sessao.cliente', login: '/cliente/login.html' },
    staff: { papel: 'STAFF', chave: 'goatilicious.sessao.staff', login: '/admin/login.html' }
  };

  function api() { return window.API_BASE || 'http://localhost:8080/api'; }

  // ---------- sessão ----------
  function ler(zona) {
    try {
      var bruto = window.sessionStorage.getItem(ZONAS[zona].chave);
      var s = bruto ? JSON.parse(bruto) : null;
      return s && s.cabecalho ? s : null;
    } catch (e) { return null; }
  }
  function gravar(zona, s) {
    try { window.sessionStorage.setItem(ZONAS[zona].chave, JSON.stringify(s)); } catch (e) { /* sem sessionStorage: sem sessão */ }
  }
  function limpar(zona) {
    try { window.sessionStorage.removeItem(ZONAS[zona].chave); } catch (e) { /* opcional */ }
  }

  function cabecalhoBasic(email, senha) {
    // UTF-8 seguro: nomes e senhas com acentos.
    return 'Basic ' + window.btoa(unescape(encodeURIComponent(email + ':' + senha)));
  }

  function irParaLogin(zona, expirou) {
    var q = [];
    if (expirou) q.push('expirou=1');
    if (zona === 'cliente') q.push('volta=' + encodeURIComponent(window.location.pathname));
    window.location.replace(ZONAS[zona].login + (q.length ? '?' + q.join('&') : ''));
  }

  // ---------- API pública ----------
  function sessao(zona) { return ler(zona); }

  /** Devolve a sessão da zona; sem sessão, manda para o login e devolve null. */
  function exigir(zona) {
    var s = ler(zona);
    if (!s) irParaLogin(zona, false);
    return s;
  }

  /**
   * Valida as credenciais em GET /api/me e confirma o papel da zona.
   * Devolve { ok: true, sessao } ou { ok: false, motivo: 'credenciais' | 'papel' | 'rede' | 'erro' }.
   */
  async function entrar(zona, email, senha) {
    var cabecalho = cabecalhoBasic(String(email).trim(), senha);
    var r;
    try {
      r = await window.fetch(api() + '/me', { headers: { Authorization: cabecalho } });
    } catch (e) {
      return { ok: false, motivo: 'rede' };
    }
    if (r.status === 401) return { ok: false, motivo: 'credenciais' };
    if (!r.ok) return { ok: false, motivo: 'erro' };

    var me = await r.json();
    if (me.papel !== ZONAS[zona].papel) return { ok: false, motivo: 'papel' };

    var s = { cabecalho: cabecalho, nome: me.nome, email: me.email, papel: me.papel, clienteId: me.clienteId };
    gravar(zona, s);
    return { ok: true, sessao: s };
  }

  function sair(zona, redirecionar) {
    limpar(zona);
    if (redirecionar !== false) irParaLogin(zona, false);
  }

  /** fetch com o cabeçalho Authorization da zona. Em 401 a sessão acabou: volta ao login. */
  async function authFetch(zona, url, opcoes) {
    var s = ler(zona);
    if (!s) {
      irParaLogin(zona, true);
      return new Promise(function () { /* a página vai mudar */ });
    }
    var op = Object.assign({}, opcoes || {});
    op.headers = Object.assign({}, op.headers || {}, { Authorization: s.cabecalho });
    var r = await window.fetch(url, op);
    if (r.status === 401) {
      limpar(zona);
      irParaLogin(zona, true);
      return new Promise(function () { /* a página vai mudar */ });
    }
    return r;
  }

  /**
   * Abre um PDF protegido (que exige Authorization) numa nova aba.
   * A aba abre-se já no clique (evita o bloqueio de pop-ups) e recebe o PDF quando chega.
   * Devolve a Response, para quem chama tratar 404/409.
   */
  async function abrirPdf(zona, url) {
    var aba = window.open('', '_blank');
    var r;
    try {
      r = await authFetch(zona, url);
    } catch (e) {
      if (aba) aba.close();
      throw e;
    }
    if (!r.ok) {
      if (aba) aba.close();
      return r;
    }
    var blob = await r.blob();
    var endereco = window.URL.createObjectURL(blob);
    if (aba) {
      aba.location.href = endereco;
    } else {
      var a = document.createElement('a');
      a.href = endereco; a.download = 'factura.pdf';
      document.body.appendChild(a); a.click(); a.remove();
    }
    window.setTimeout(function () { window.URL.revokeObjectURL(endereco); }, 60000);
    return r;
  }

  /** Só aceita destinos internos (evita redirecionamento para sites externos). */
  function destinoSeguro(valor, padrao) {
    if (typeof valor === 'string' && valor.charAt(0) === '/' && valor.charAt(1) !== '/' && valor.indexOf('\\') < 0) {
      return valor;
    }
    return padrao;
  }

  // ---------- menus ----------
  function criar(tag, classe, texto) {
    var e = document.createElement(tag);
    if (classe) e.className = classe;
    if (texto != null) e.textContent = texto;
    return e;
  }

  function pintarMenuCliente() {
    var nav = document.getElementById('site-nav');
    if (!nav) return;
    var botao = document.querySelector('.header .contact-btn');
    var s = ler('cliente');

    if (s) {
      var meus = criar('a', 'nav-item', 'Os meus pedidos');
      meus.href = '/cliente/meus-pedidos.html';
      nav.appendChild(meus);
      if (botao) {
        botao.textContent = 'Olá, ' + s.nome + ' · Sair';
        botao.href = '#';
        botao.addEventListener('click', function (ev) { ev.preventDefault(); sair('cliente'); });
      }
    } else {
      var entrarLink = criar('a', 'nav-item', 'Entrar');
      entrarLink.href = '/cliente/login.html';
      nav.appendChild(entrarLink);
      if (botao) { botao.textContent = 'Criar Conta'; botao.href = '/cliente/cadastro.html'; }
    }
  }

  function pintarMenuStaff() {
    var nav = document.querySelector('nav.menu-staff');
    var s = ler('staff');
    if (!nav || !s) return;
    var bloco = criar('span', 'sessao-staff');
    bloco.appendChild(criar('span', 'sessao-nome', s.nome));
    var btn = criar('button', 'btn-sair', 'Sair');
    btn.type = 'button';
    btn.addEventListener('click', function () { sair('staff'); });
    bloco.appendChild(btn);
    nav.appendChild(bloco);
  }

  // ---------- guarda de acesso ----------
  // <body data-exige="staff|cliente">: sem sessão da zona, vai para o login. O servidor valida sempre (401/403).
  var estilo = document.createElement('style');
  estilo.textContent = 'body[data-exige]:not(.autorizado){visibility:hidden}';
  (document.head || document.documentElement).appendChild(estilo);

  function iniciar() {
    var zona = document.body && document.body.getAttribute('data-exige');
    if (zona && ZONAS[zona]) {
      if (!ler(zona)) { irParaLogin(zona, false); return; }
      document.body.classList.add('autorizado');
    }
    pintarMenuCliente();
    pintarMenuStaff();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', iniciar);
  else iniciar();

  window.GoatAuth = {
    sessao: sessao, exigir: exigir, entrar: entrar, sair: sair, authFetch: authFetch,
    abrirPdf: abrirPdf, destinoSeguro: destinoSeguro
  };
})();
