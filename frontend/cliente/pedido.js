import '../src/styles/pedido.css';
import { initBubbles } from '../src/modules/bubbles.js';
import { initMenu } from '../src/modules/menu.js';

(function () {
  initMenu();
  initBubbles();

  const API_BASE = window.API_BASE || 'http://localhost:8080/api';
  const CHAVE = 'goatilicious.cliente';
  const TAMANHOS = { ML_125: '125 ml', ML_500: '500 ml', L_1: '1 L', L_5: '5 L' };
  const STOCK = { PRONTA_ENTREGA: 'Pronta entrega', SOB_ENCOMENDA: 'Sob encomenda' };

  const formIdentificar = document.getElementById('form-identificar');
  const inputEmail = document.getElementById('email');
  const btnIdentificar = document.getElementById('btn-identificar');
  const passoPedido = document.getElementById('passo-pedido');
  const nomeCliente = document.getElementById('nome-cliente');
  const catalogo = document.getElementById('catalogo');
  const totalEl = document.getElementById('total');
  const btnEnviar = document.getElementById('btn-enviar');
  const btnTrocar = document.getElementById('btn-trocar');
  const mensagem = document.getElementById('mensagem');

  let cliente = null;          // { idCliente, nome }
  let produtos = [];
  const quantidades = {};      // idProduto -> quantidade

  // ---------- utilitários ----------
  function mt(valor) {
    return Number(valor).toLocaleString('pt-PT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' MT';
  }

  function guardar(c) { try { localStorage.setItem(CHAVE, JSON.stringify(c)); } catch (e) { /* opcional */ } }
  function lembrado() { try { return JSON.parse(localStorage.getItem(CHAVE)); } catch (e) { return null; } }
  function esquecer() { try { localStorage.removeItem(CHAVE); } catch (e) { /* opcional */ } }

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

  // ---------- total ----------
  function atualizarTotal() {
    let total = 0;
    let linhas = 0;
    produtos.forEach(function (p) {
      const q = quantidades[p.idProduto] || 0;
      if (q > 0) { total += q * Number(p.precoUnitario); linhas++; }
    });
    totalEl.textContent = mt(total);
    btnEnviar.disabled = linhas === 0;
  }

  // ---------- catálogo ----------
  function linhaProduto(p) {
    const linha = el('div', 'linha-produto');

    const info = el('div');
    info.appendChild(el('span', 'tam', TAMANHOS[p.tamanho] || p.tamanho));
    info.appendChild(el('span', 'stock', STOCK[p.tipoEstoque] || ''));

    const preco = el('span', 'preco', mt(p.precoUnitario));

    const saida = document.createElement('output');
    saida.textContent = '0';
    const menos = el('button', null, '−');
    const mais = el('button', null, '+');
    menos.type = 'button'; mais.type = 'button';
    menos.setAttribute('aria-label', 'Diminuir quantidade');
    mais.setAttribute('aria-label', 'Aumentar quantidade');

    function definir(q) {
      quantidades[p.idProduto] = q;
      saida.textContent = String(q);
      linha.classList.toggle('ativa', q > 0);
      atualizarTotal();
    }
    menos.addEventListener('click', function () { definir(Math.max(0, (quantidades[p.idProduto] || 0) - 1)); });
    mais.addEventListener('click', function () { definir(Math.min(99, (quantidades[p.idProduto] || 0) + 1)); });

    const qtd = el('div', 'qtd');
    qtd.append(menos, saida, mais);
    linha.append(info, preco, qtd);
    return linha;
  }

  function renderizarCatalogo() {
    catalogo.replaceChildren();
    if (produtos.length === 0) {
      catalogo.appendChild(el('p', 'estado-texto', 'De momento não há produtos disponíveis.'));
      return;
    }
    const porSabor = new Map();
    produtos.forEach(function (p) {
      if (!porSabor.has(p.sabor)) porSabor.set(p.sabor, []);
      porSabor.get(p.sabor).push(p);
    });
    porSabor.forEach(function (lista, sabor) {
      const bloco = el('div', 'sabor');
      bloco.appendChild(el('h2', null, sabor));
      lista.forEach(function (p) { bloco.appendChild(linhaProduto(p)); });
      catalogo.appendChild(bloco);
    });
  }

  async function carregarCatalogo() {
    catalogo.replaceChildren(el('p', 'estado-texto', 'A carregar catálogo...'));
    try {
      const r = await fetch(API_BASE + '/produtos');
      if (!r.ok) throw new Error('HTTP ' + r.status);
      produtos = await r.json();
      renderizarCatalogo();
    } catch (e) {
      catalogo.replaceChildren(el('p', 'estado-texto', 'Não foi possível carregar o catálogo. Verifique se o back-end está ativo.'));
    }
    atualizarTotal();
  }

  // ---------- passos ----------
  function entrarNoPedido(c) {
    cliente = c;
    guardar(c);
    nomeCliente.textContent = c.nome;
    formIdentificar.hidden = true;
    passoPedido.hidden = false;
    limparMensagem();
    carregarCatalogo();
  }

  formIdentificar.addEventListener('submit', async function (evento) {
    evento.preventDefault();
    limparMensagem();
    if (!formIdentificar.checkValidity()) { formIdentificar.reportValidity(); return; }

    btnIdentificar.disabled = true;
    try {
      const r = await fetch(API_BASE + '/clientes/por-email?email=' + encodeURIComponent(inputEmail.value.trim()));
      if (r.ok) {
        entrarNoPedido(await r.json());
      } else if (r.status === 404) {
        mostrarMensagem('Não encontrámos esse email. Crie primeiro a sua conta.', 'erro');
      } else {
        mostrarMensagem('Ocorreu um erro inesperado (código ' + r.status + ').', 'erro');
      }
    } catch (e) {
      mostrarMensagem('Não foi possível conectar ao servidor. Verifique se o back-end está ativo na porta 8080.', 'erro');
    } finally {
      btnIdentificar.disabled = false;
    }
  });

  btnTrocar.addEventListener('click', function () {
    esquecer();
    cliente = null;
    Object.keys(quantidades).forEach(function (k) { delete quantidades[k]; });
    passoPedido.hidden = true;
    formIdentificar.hidden = false;
    formIdentificar.reset();
    limparMensagem();
  });

  btnEnviar.addEventListener('click', async function () {
    limparMensagem();
    const itens = produtos
      .filter(function (p) { return (quantidades[p.idProduto] || 0) > 0; })
      .map(function (p) { return { produtoId: p.idProduto, quantidade: quantidades[p.idProduto] }; });
    if (itens.length === 0) return;

    btnEnviar.disabled = true;
    try {
      const r = await fetch(API_BASE + '/pedidos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clienteId: cliente.idCliente, origem: 'ONLINE', itens: itens })
      });
      if (r.status === 201) {
        const pedido = await r.json();
        mostrarMensagem('Pedido #' + pedido.idPedido + ' registado com sucesso! Total: ' + mt(pedido.total) + '. Entraremos em contacto para a entrega.', 'sucesso');
        Object.keys(quantidades).forEach(function (k) { delete quantidades[k]; });
        renderizarCatalogo();
      } else if (r.status === 404) {
        mostrarMensagem('Cliente ou produto não encontrado. Recarregue a página e tente novamente.', 'erro');
      } else if (r.status === 400) {
        mostrarMensagem('Pedido inválido. Verifique as quantidades.', 'erro');
      } else {
        mostrarMensagem('Ocorreu um erro inesperado (código ' + r.status + ').', 'erro');
      }
    } catch (e) {
      mostrarMensagem('Não foi possível conectar ao servidor. Verifique se o back-end está ativo na porta 8080.', 'erro');
    } finally {
      atualizarTotal();
    }
  });

  // Cliente que acabou de se registar (ou já fez pedido antes) entra direto no passo 2.
  const guardado = lembrado();
  if (guardado && guardado.idCliente && guardado.nome) entrarNoPedido(guardado);
})();
