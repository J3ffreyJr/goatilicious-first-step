import '../src/styles/pedido.css';
import '../shared/auth.js';
import { initBubbles } from '../src/modules/bubbles.js';
import { initMenu } from '../src/modules/menu.js';

(function () {
  initMenu();
  initBubbles();

  const API_BASE = window.API_BASE || 'http://localhost:8080/api';
  const TAMANHOS = { ML_125: '125 ml', ML_500: '500 ml', L_1: '1 L', L_5: '5 L' };
  const STOCK = { PRONTA_ENTREGA: 'Pronta entrega', SOB_ENCOMENDA: 'Sob encomenda' };
  const SEGUNDOS_ATE_INICIO = 5;

  const $ = function (id) { return document.getElementById(id); };
  const passos = { escolher: $('passo-escolher'), rever: $('passo-rever') };
  const popup = $('popup-sucesso');
  const nomeCliente = $('nome-cliente');
  const catalogo = $('catalogo');
  const pesquisa = $('pesquisa');
  const filtroTamanho = $('filtro-tamanho');
  const totalEl = $('total');
  const totalRever = $('total-rever');
  const carrinhoItens = $('carrinho-itens');
  const btnLimpar = $('btn-limpar');
  const btnRever = $('btn-rever');
  const btnVoltar = $('btn-voltar');
  const btnEnviar = $('btn-enviar');
  const resumoItens = $('resumo-itens');
  const mensagem = $('mensagem');

  const sessao = window.GoatAuth.exigir('cliente');   // { nome, clienteId, ... }; sem sessão vai ao login
  if (!sessao) return;

  let produtos = [];
  const quantidades = {};      // idProduto -> quantidade

  // ---------- utilitários ----------
  function mt(valor) {
    return Number(valor).toLocaleString('pt-PT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' MT';
  }

  function mostrarMensagem(texto, tipo) {
    mensagem.textContent = texto;               // textContent: nunca interpreta HTML
    mensagem.className = 'mensagem-box ' + tipo;
    mensagem.hidden = false;
  }
  function limparMensagem() { mensagem.hidden = true; }

  function el(tag, classe, texto) {
    const e = document.createElement(tag);
    if (classe) e.className = classe;
    if (texto != null) e.textContent = texto;
    return e;
  }

  function semAcentos(texto) {
    return String(texto).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  }

  function escolhidos() {
    return produtos.filter(function (p) { return (quantidades[p.idProduto] || 0) > 0; });
  }

  // ---------- etapas ----------
  function irPara(passo) {
    Object.keys(passos).forEach(function (k) { passos[k].hidden = k !== passo; });
    $('etapa-1').className = passo === 'escolher' ? 'atual' : 'feita';
    $('etapa-2').className = passo === 'rever' ? 'atual' : (passo === 'concluido' ? 'feita' : '');
    $('etapa-3').className = passo === 'concluido' ? 'atual' : '';
    limparMensagem();
  }

  // ---------- carrinho ----------
  function atualizarCarrinho() {
    let total = 0;
    let unidades = 0;
    escolhidos().forEach(function (p) {
      const q = quantidades[p.idProduto];
      total += q * Number(p.precoUnitario);
      unidades += q;
    });
    totalEl.textContent = mt(total);
    totalRever.textContent = mt(total);
    carrinhoItens.textContent = unidades === 0 ? 'Carrinho vazio'
      : unidades + (unidades === 1 ? ' item' : ' itens') + ' no carrinho';
    btnRever.disabled = unidades === 0;
    btnLimpar.disabled = unidades === 0;
  }

  // ---------- catálogo ----------
  function linhaProduto(p) {
    const linha = el('div', 'linha-produto');

    const info = el('div');
    info.appendChild(el('span', 'tam', TAMANHOS[p.tamanho] || p.tamanho));
    info.appendChild(el('span', 'stock', STOCK[p.tipoEstoque] || ''));

    const preco = el('span', 'preco', mt(p.precoUnitario));

    const saida = document.createElement('output');
    const menos = el('button', null, '−');
    const mais = el('button', null, '+');
    menos.type = 'button'; mais.type = 'button';
    menos.setAttribute('aria-label', 'Diminuir quantidade de ' + p.sabor + ' ' + (TAMANHOS[p.tamanho] || ''));
    mais.setAttribute('aria-label', 'Aumentar quantidade de ' + p.sabor + ' ' + (TAMANHOS[p.tamanho] || ''));

    function mostrar() {
      const q = quantidades[p.idProduto] || 0;
      saida.textContent = String(q);
      linha.classList.toggle('ativa', q > 0);
    }
    function definir(q) {
      quantidades[p.idProduto] = q;
      mostrar();
      atualizarCarrinho();
    }
    menos.addEventListener('click', function () { definir(Math.max(0, (quantidades[p.idProduto] || 0) - 1)); });
    mais.addEventListener('click', function () { definir(Math.min(99, (quantidades[p.idProduto] || 0) + 1)); });
    mostrar();

    const qtd = el('div', 'qtd');
    qtd.append(menos, saida, mais);
    linha.append(info, preco, qtd);
    return linha;
  }

  function filtrados() {
    const texto = semAcentos(pesquisa.value.trim());
    const tamanho = filtroTamanho.value;
    return produtos.filter(function (p) {
      return (!texto || semAcentos(p.sabor).indexOf(texto) >= 0) && (!tamanho || p.tamanho === tamanho);
    });
  }

  function renderizarCatalogo() {
    catalogo.replaceChildren();
    if (produtos.length === 0) {
      catalogo.appendChild(el('p', 'estado-texto', 'De momento não há produtos disponíveis.'));
      return;
    }
    const lista = filtrados();
    if (lista.length === 0) {
      catalogo.appendChild(el('p', 'estado-texto', 'Nenhum produto corresponde à pesquisa.'));
      return;
    }
    const porSabor = new Map();
    lista.forEach(function (p) {
      if (!porSabor.has(p.sabor)) porSabor.set(p.sabor, []);
      porSabor.get(p.sabor).push(p);
    });
    porSabor.forEach(function (itens, sabor) {
      const bloco = el('div', 'sabor');
      bloco.appendChild(el('h2', null, sabor));
      itens.forEach(function (p) { bloco.appendChild(linhaProduto(p)); });
      catalogo.appendChild(bloco);
    });
  }

  function preencherFiltroTamanho() {
    const presentes = Array.from(new Set(produtos.map(function (p) { return p.tamanho; })));
    Object.keys(TAMANHOS).filter(function (t) { return presentes.indexOf(t) >= 0; }).forEach(function (t) {
      const o = el('option', null, TAMANHOS[t]);
      o.value = t;
      filtroTamanho.appendChild(o);
    });
  }

  async function carregarCatalogo() {
    catalogo.replaceChildren(el('p', 'estado-texto', 'A carregar catálogo...'));
    try {
      const r = await fetch(API_BASE + '/produtos');
      if (!r.ok) throw new Error('HTTP ' + r.status);
      produtos = await r.json();
      preencherFiltroTamanho();
      renderizarCatalogo();
    } catch (e) {
      catalogo.replaceChildren(el('p', 'estado-texto', 'Não foi possível carregar o catálogo. Verifique se o back-end está ativo.'));
    }
    atualizarCarrinho();
  }

  // ---------- rever ----------
  function renderizarResumo() {
    resumoItens.replaceChildren();
    escolhidos().forEach(function (p) {
      const q = quantidades[p.idProduto];
      const linha = el('div', 'linha-produto');
      const info = el('div');
      info.appendChild(el('span', 'tam', p.sabor + ' · ' + (TAMANHOS[p.tamanho] || p.tamanho)));
      info.appendChild(el('span', 'stock', q + ' × ' + mt(p.precoUnitario)));
      const remover = el('button', 'btn-remover', 'Remover');
      remover.type = 'button';
      remover.addEventListener('click', function () {
        delete quantidades[p.idProduto];
        atualizarCarrinho();
        if (escolhidos().length === 0) { renderizarCatalogo(); irPara('escolher'); }
        else renderizarResumo();
      });
      linha.append(info, el('span', 'preco', mt(q * Number(p.precoUnitario))), remover);
      resumoItens.appendChild(linha);
    });
  }

  btnRever.addEventListener('click', function () {
    if (escolhidos().length === 0) return;
    renderizarResumo();
    irPara('rever');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });
  btnVoltar.addEventListener('click', function () { renderizarCatalogo(); irPara('escolher'); });
  btnLimpar.addEventListener('click', function () {
    Object.keys(quantidades).forEach(function (k) { delete quantidades[k]; });
    renderizarCatalogo();
    atualizarCarrinho();
  });
  pesquisa.addEventListener('input', renderizarCatalogo);
  filtroTamanho.addEventListener('change', renderizarCatalogo);

  // ---------- concluído: volta ao início ----------
  function concluir(pedido) {
    $('popup-titulo').textContent = 'Pedido #' + pedido.idPedido + ' feito com sucesso!';
    $('popup-texto').textContent = 'Total: ' + mt(pedido.total) + '. Entraremos em contacto para a entrega.';
    irPara('concluido');
    popup.hidden = false;
    document.body.classList.add('popup-aberto');
    $('popup-botao').focus();

    let restante = SEGUNDOS_ATE_INICIO;
    const contagem = $('contagem');
    contagem.textContent = String(restante);
    const relogio = window.setInterval(function () {
      restante -= 1;
      contagem.textContent = String(Math.max(0, restante));
      if (restante <= 0) {
        window.clearInterval(relogio);
        window.location.assign('/');
      }
    }, 1000);
  }

  // ---------- confirmar ----------
  btnEnviar.addEventListener('click', async function () {
    limparMensagem();
    const itens = escolhidos().map(function (p) { return { produtoId: p.idProduto, quantidade: quantidades[p.idProduto] }; });
    if (itens.length === 0) return;

    btnEnviar.disabled = true;
    btnVoltar.disabled = true;
    try {
      // O servidor usa o cliente da sessão e a origem ONLINE; clienteId/origem seguem só por compatibilidade com a API.
      const r = await window.GoatAuth.authFetch('cliente', API_BASE + '/pedidos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clienteId: sessao.clienteId, origem: 'ONLINE', itens: itens })
      });
      if (r.status === 201) {
        const pedido = await r.json();
        Object.keys(quantidades).forEach(function (k) { delete quantidades[k]; });
        concluir(pedido);
        return;
      }
      if (r.status === 403) {
        mostrarMensagem('Esta conta não pode fazer pedidos. Contacte a Goatilicious.', 'erro');
      } else if (r.status === 404) {
        mostrarMensagem('Cliente ou produto não encontrado. Recarregue a página e tente novamente.', 'erro');
      } else if (r.status === 400) {
        mostrarMensagem('Pedido inválido. Verifique as quantidades.', 'erro');
      } else {
        mostrarMensagem('Ocorreu um erro inesperado (código ' + r.status + ').', 'erro');
      }
    } catch (e) {
      mostrarMensagem('Não foi possível conectar ao servidor. Verifique se o back-end está ativo na porta 8080.', 'erro');
    }
    btnEnviar.disabled = false;
    btnVoltar.disabled = false;
  });

  nomeCliente.textContent = sessao.nome;
  carregarCatalogo();
})();
