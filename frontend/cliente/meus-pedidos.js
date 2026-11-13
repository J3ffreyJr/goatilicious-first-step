import '../src/styles/pedido.css';
import '../shared/auth.js';
import { initBubbles } from '../src/modules/bubbles.js';
import { initMenu } from '../src/modules/menu.js';

(function () {
  initMenu();
  initBubbles();

  const GoatAuth = window.GoatAuth;
  if (!GoatAuth.exigir('cliente')) return;

  const API_BASE = window.API_BASE || 'http://localhost:8080/api';
  const TAMANHOS = { ML_125: '125 ml', ML_500: '500 ml', L_1: '1 L', L_5: '5 L' };
  const ESTADOS = { PENDENTE: 'Em análise', REVISADO: 'Confirmado', CANCELADO: 'Cancelado', ENVIADO: 'Enviado' };

  const lista = document.getElementById('lista-pedidos');
  const mensagem = document.getElementById('mensagem');

  function mt(valor) {
    return Number(valor).toLocaleString('pt-PT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' MT';
  }

  function dataCurta(iso) {
    if (!iso) return '';
    const d = String(iso).slice(0, 10).split('-');
    return d.length === 3 ? d[2] + '/' + d[1] + '/' + d[0] : iso;
  }

  function el(tag, classe, texto) {
    const e = document.createElement(tag);
    if (classe) e.className = classe;
    if (texto != null) e.textContent = texto;   // textContent: nunca interpreta HTML
    return e;
  }

  function mostrarMensagem(texto, tipo) {
    mensagem.textContent = texto;
    mensagem.className = 'mensagem-box ' + tipo;
    mensagem.hidden = false;
  }

  async function abrirFactura(idPedido, botao) {
    mensagem.hidden = true;
    botao.disabled = true;
    try {
      // A fatura final existe depois de o pedido ser revisto; o PDF só abre nesse caso.
      const rf = await GoatAuth.authFetch('cliente', API_BASE + '/pedidos/' + idPedido + '/factura');
      if (!rf.ok) { mostrarMensagem('Não foi possível encontrar a fatura deste pedido.', 'erro'); return; }
      const factura = await rf.json();
      if (factura.estado !== 'FINAL') {
        mostrarMensagem('A fatura ainda não está pronta: o pedido está em análise.', 'erro');
        return;
      }
      const r = await GoatAuth.abrirPdf('cliente', API_BASE + '/facturas/' + factura.idFactura + '/pdf');
      if (!r.ok) mostrarMensagem('Não foi possível abrir a fatura (código ' + r.status + ').', 'erro');
    } catch (e) {
      mostrarMensagem('Não foi possível conectar ao servidor.', 'erro');
    } finally {
      botao.disabled = false;
    }
  }

  function cartao(p) {
    const bloco = el('div', 'sabor');
    bloco.appendChild(el('h2', null, 'Pedido #' + p.idPedido + ' · ' + dataCurta(p.dataPedido)));

    (p.itens || []).forEach(function (i) {
      const linha = el('div', 'linha-produto');
      const info = el('div');
      info.appendChild(el('span', 'tam', i.produto.sabor + ' · ' + (TAMANHOS[i.produto.tamanho] || i.produto.tamanho)));
      info.appendChild(el('span', 'stock', i.quantidade + ' × ' + mt(i.precoAplicado)));
      linha.append(info, el('span', 'preco', mt(i.quantidade * Number(i.precoAplicado))));
      bloco.appendChild(linha);
    });

    const resumo = el('div', 'resumo');
    resumo.appendChild(el('span', null, ESTADOS[p.status] || p.status));
    resumo.appendChild(el('strong', null, mt(p.total)));
    bloco.appendChild(resumo);

    const botao = el('button', 'btn-submit', 'Fatura PDF');
    botao.type = 'button';
    botao.addEventListener('click', function () { abrirFactura(p.idPedido, botao); });
    bloco.appendChild(botao);
    return bloco;
  }

  async function carregar() {
    try {
      const r = await GoatAuth.authFetch('cliente', API_BASE + '/pedidos');
      if (!r.ok) throw new Error('HTTP ' + r.status);
      const pedidos = await r.json();
      lista.replaceChildren();
      if (pedidos.length === 0) {
        lista.appendChild(el('p', 'estado-texto', 'Ainda não fez nenhum pedido.'));
        return;
      }
      pedidos.slice().sort(function (a, b) { return b.idPedido - a.idPedido; })
        .forEach(function (p) { lista.appendChild(cartao(p)); });
    } catch (e) {
      lista.replaceChildren(el('p', 'estado-texto', 'Não foi possível carregar os pedidos. Verifique se o back-end está ativo.'));
    }
  }

  carregar();
})();
