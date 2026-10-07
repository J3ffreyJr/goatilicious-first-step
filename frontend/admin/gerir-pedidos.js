(function () {
  const TAMANHOS = { ML_125: '125 ml', ML_500: '500 ml', L_1: '1 L', L_5: '5 L' };
  const ORIGENS = { ONLINE: 'Online', TELEFONE: 'Telefone' };
  const COLUNAS = 8;
  const CONDICOES = { A_VISTA: 'À vista', CARTAO: 'Cartão', PAGO_NA_ENTREGA: 'Pagamento na entrega' };

  const selCliente = document.getElementById('cliente');
  const form = document.getElementById('form-pedido');
  const itensEl = document.getElementById('itens');
  const totalEl = document.getElementById('total');
  const btnAdd = document.getElementById('btn-add');
  const btnGuardar = document.getElementById('btn-guardar');
  const mensagem = document.getElementById('mensagem');
  const corpo = document.getElementById('corpo-tabela');
  const contagem = document.getElementById('contagem');
  const btnAtualizar = document.getElementById('btn-atualizar');

  let produtos = [];
  let pedidosPorId = {};

  function mt(v) {
    return Number(v).toLocaleString('pt-PT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' MT';
  }
  function rotuloProduto(p) {
    return p.sabor + ' — ' + (TAMANHOS[p.tamanho] || p.tamanho) + ' — ' + mt(p.precoUnitario);
  }
  function mostrarMensagem(texto, tipo) {
    mensagem.textContent = texto;
    mensagem.className = 'mensagem ' + tipo;
    mensagem.hidden = false;
  }

  // ---------- formulário ----------
  function atualizarTotal() {
    let total = 0;
    itensEl.querySelectorAll('.linha-item').forEach(function (linha) {
      const p = produtos.find(function (x) { return String(x.idProduto) === linha.querySelector('select').value; });
      const q = parseInt(linha.querySelector('input').value, 10) || 0;
      if (p && q > 0) total += q * Number(p.precoUnitario);
    });
    totalEl.textContent = mt(total);
  }

  function novaLinha() {
    const linha = document.createElement('div');
    linha.className = 'linha-item';

    const select = document.createElement('select');
    select.required = true;
    const vazio = document.createElement('option');
    vazio.value = ''; vazio.textContent = 'Escolha o produto...';
    select.appendChild(vazio);
    produtos.forEach(function (p) {
      const o = document.createElement('option');
      o.value = p.idProduto;
      o.textContent = rotuloProduto(p);
      select.appendChild(o);
    });

    const qtd = document.createElement('input');
    qtd.type = 'number'; qtd.min = '1'; qtd.max = '99'; qtd.value = '1'; qtd.required = true;
    qtd.setAttribute('aria-label', 'Quantidade');

    const remover = document.createElement('button');
    remover.type = 'button'; remover.className = 'secundario'; remover.textContent = '×';
    remover.setAttribute('aria-label', 'Remover item');
    remover.addEventListener('click', function () {
      if (itensEl.children.length > 1) { linha.remove(); atualizarTotal(); }
    });

    select.addEventListener('change', atualizarTotal);
    qtd.addEventListener('input', atualizarTotal);
    linha.append(select, qtd, remover);
    itensEl.appendChild(linha);
  }

  async function carregarFormulario() {
    try {
      const [rc, rp] = await Promise.all([fetch(API_BASE + '/clientes'), fetch(API_BASE + '/produtos')]);
      if (!rc.ok || !rp.ok) throw new Error('HTTP');
      const clientes = await rc.json();
      produtos = await rp.json();

      selCliente.replaceChildren();
      const vazio = document.createElement('option');
      vazio.value = ''; vazio.textContent = clientes.length ? 'Escolha o cliente...' : 'Sem clientes registados';
      selCliente.appendChild(vazio);
      clientes.forEach(function (c) {
        const o = document.createElement('option');
        o.value = c.idCliente;
        o.textContent = c.nome + ' (' + c.email + ')';
        selCliente.appendChild(o);
      });

      itensEl.replaceChildren();
      novaLinha();
      atualizarTotal();
    } catch (e) {
      mostrarMensagem('Não foi possível carregar clientes e produtos. Verifique se o back-end está a correr.', 'erro');
    }
  }

  btnAdd.addEventListener('click', function () { if (produtos.length) novaLinha(); });

  form.addEventListener('submit', async function (evento) {
    evento.preventDefault();
    mensagem.hidden = true;
    if (!form.checkValidity()) { form.reportValidity(); return; }

    const itens = Array.from(itensEl.querySelectorAll('.linha-item')).map(function (linha) {
      return {
        produtoId: Number(linha.querySelector('select').value),
        quantidade: parseInt(linha.querySelector('input').value, 10)
      };
    });

    btnGuardar.disabled = true;
    try {
      const r = await fetch(API_BASE + '/pedidos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clienteId: Number(selCliente.value), origem: 'TELEFONE', itens: itens })
      });
      if (r.status === 201) {
        const p = await r.json();
        mostrarMensagem('Pedido #' + p.idPedido + ' registado. Total: ' + mt(p.total) + '.', 'sucesso');
        form.reset();
        itensEl.replaceChildren();
        novaLinha();
        atualizarTotal();
        carregarPedidos();
      } else if (r.status === 404) {
        mostrarMensagem('Cliente ou produto não encontrado.', 'erro');
      } else if (r.status === 400) {
        mostrarMensagem('Pedido inválido. Verifique o cliente, os produtos e as quantidades.', 'erro');
      } else {
        mostrarMensagem('Erro inesperado (código ' + r.status + ').', 'erro');
      }
    } catch (e) {
      mostrarMensagem('Não foi possível conectar ao servidor.', 'erro');
    } finally {
      btnGuardar.disabled = false;
    }
  });

  // ---------- lista de pedidos ----------
  function mostrarEstado(texto) {
    corpo.replaceChildren();
    const tr = document.createElement('tr');
    const td = document.createElement('td');
    td.colSpan = COLUNAS; td.className = 'estado'; td.textContent = texto;
    tr.appendChild(td);
    corpo.appendChild(tr);
  }

  function formatarData(iso) {
    if (!iso) return '';
    const m = String(iso).match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
    return m ? m[3] + '/' + m[2] + '/' + m[1] + ' ' + m[4] + ':' + m[5] : iso;
  }

  function resumoItens(p) {
    return (p.itens || []).map(function (i) {
      const prod = i.produto || {};
      return i.quantidade + '× ' + (prod.sabor || '?') + ' ' + (TAMANHOS[prod.tamanho] || '');
    }).join('; ');
  }

  async function carregarPedidos() {
    btnAtualizar.disabled = true;
    contagem.textContent = '';
    mostrarEstado('A carregar pedidos...');
    try {
      const r = await fetch(API_BASE + '/pedidos');
      if (!r.ok) throw new Error('HTTP ' + r.status);
      const pedidos = await r.json();
      pedidos.sort(function (a, b) { return b.idPedido - a.idPedido; });
      pedidosPorId = {};
      pedidos.forEach(function (p) { pedidosPorId[p.idPedido] = p; });

      if (pedidos.length === 0) {
        mostrarEstado('Ainda não há pedidos registados.');
      } else {
        corpo.replaceChildren();
        pedidos.forEach(function (p) {
          const tr = document.createElement('tr');
          [p.idPedido, p.cliente ? p.cliente.nome : '', ORIGENS[p.origem] || p.origem,
           resumoItens(p), mt(p.total), p.status, formatarData(p.dataPedido)].forEach(function (v) {
            const td = document.createElement('td');
            td.textContent = v == null ? '' : v;   // textContent: nunca interpreta HTML
            tr.appendChild(td);
          });
          const acoes = document.createElement('td');
          if (p.status === 'PENDENTE') {
            const b = document.createElement('button');
            b.type = 'button'; b.textContent = 'Revisar';
            b.addEventListener('click', function () { abrirRevisao(p.idPedido); });
            acoes.appendChild(b);
          }
          tr.appendChild(acoes);
          corpo.appendChild(tr);
        });
      }
      contagem.textContent = pedidos.length + (pedidos.length === 1 ? ' pedido' : ' pedidos');
    } catch (e) {
      mostrarEstado('Não foi possível carregar os pedidos. Verifique se o back-end está a correr.');
    } finally {
      btnAtualizar.disabled = false;
    }
  }

  // ---------- revisão do pedido (itens + condições de pagamento) ----------
  const painel = document.getElementById('painel-revisao');
  const revTitulo = document.getElementById('rev-titulo');
  const revItensEl = document.getElementById('rev-itens');
  const revProduto = document.getElementById('rev-produto');
  const revCondicao = document.getElementById('rev-condicao');
  const revTotal = document.getElementById('rev-total');
  const revAdd = document.getElementById('rev-add');
  const revFinalizar = document.getElementById('rev-finalizar');
  const revCancelar = document.getElementById('rev-cancelar');
  const revMensagem = document.getElementById('rev-mensagem');

  let revPedidoId = null;
  let revItens = [];   // { produtoId, quantidade, preco, rotulo }

  function mostrarMensagemRev(texto, tipo) {
    revMensagem.textContent = texto;
    revMensagem.className = 'mensagem ' + tipo;
    revMensagem.hidden = false;
  }

  function nomeProduto(p) {
    return p.sabor + ' ' + (TAMANHOS[p.tamanho] || p.tamanho);
  }

  function totalRevisao() {
    return revItens.reduce(function (soma, i) { return soma + i.quantidade * Number(i.preco); }, 0);
  }

  function desenharRevisao() {
    revItensEl.replaceChildren();
    revItens.forEach(function (item) {
      const linha = document.createElement('div');
      linha.className = 'linha-item';

      const rotulo = document.createElement('span');
      rotulo.className = 'rotulo-item';
      rotulo.textContent = item.rotulo + ' — ' + mt(item.preco);

      const qtd = document.createElement('input');
      qtd.type = 'number'; qtd.min = '1'; qtd.max = '99'; qtd.value = String(item.quantidade);
      qtd.setAttribute('aria-label', 'Quantidade de ' + item.rotulo);
      qtd.addEventListener('input', function () {
        item.quantidade = parseInt(qtd.value, 10) || 0;
        revTotal.textContent = mt(totalRevisao());
      });

      const remover = document.createElement('button');
      remover.type = 'button'; remover.className = 'secundario'; remover.textContent = '×';
      remover.setAttribute('aria-label', 'Remover ' + item.rotulo);
      remover.disabled = revItens.length <= 1;
      remover.addEventListener('click', function () {
        revItens = revItens.filter(function (x) { return x !== item; });
        desenharRevisao();
      });

      linha.append(rotulo, qtd, remover);
      revItensEl.appendChild(linha);
    });

    // só oferece produtos que ainda não estão no pedido
    revProduto.replaceChildren();
    produtos.filter(function (p) {
      return !revItens.some(function (i) { return i.produtoId === p.idProduto; });
    }).forEach(function (p) {
      const o = document.createElement('option');
      o.value = p.idProduto;
      o.textContent = rotuloProduto(p);
      revProduto.appendChild(o);
    });
    revAdd.disabled = revProduto.options.length === 0;
    revTotal.textContent = mt(totalRevisao());
  }

  function abrirRevisao(idPedido) {
    const pedido = pedidosPorId[idPedido];
    if (!pedido) return;
    revPedidoId = idPedido;
    revItens = (pedido.itens || []).map(function (i) {
      return {
        produtoId: i.produto.idProduto,
        quantidade: i.quantidade,
        preco: i.precoAplicado,   // preço congelado do pedido
        rotulo: nomeProduto(i.produto)
      };
    });
    revTitulo.textContent = 'Rever pedido #' + idPedido + ' — ' + (pedido.cliente ? pedido.cliente.nome : '');
    revCondicao.value = 'A_VISTA';
    revMensagem.hidden = true;
    desenharRevisao();
    painel.hidden = false;
    painel.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  function fecharRevisao() {
    painel.hidden = true;
    revPedidoId = null;
    revItens = [];
  }

  revAdd.addEventListener('click', function () {
    const p = produtos.find(function (x) { return String(x.idProduto) === revProduto.value; });
    if (!p) return;
    revItens.push({ produtoId: p.idProduto, quantidade: 1, preco: p.precoUnitario, rotulo: nomeProduto(p) });
    desenharRevisao();
  });

  revCancelar.addEventListener('click', fecharRevisao);

  revFinalizar.addEventListener('click', async function () {
    revMensagem.hidden = true;
    if (revItens.length === 0 || revItens.some(function (i) { return !(i.quantidade >= 1); })) {
      mostrarMensagemRev('Cada item precisa de uma quantidade de pelo menos 1.', 'erro');
      return;
    }

    revFinalizar.disabled = true;
    try {
      const r = await fetch(API_BASE + '/pedidos/' + revPedidoId + '/revisao', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          condicaoPagamento: revCondicao.value,
          itens: revItens.map(function (i) { return { produtoId: i.produtoId, quantidade: i.quantidade }; })
        })
      });
      if (r.ok) {
        const f = await r.json();
        const idRevisto = revPedidoId;
        fecharRevisao();
        mostrarMensagem('Pedido #' + idRevisto + ' revisto. Factura final: ' + mt(f.valorTotal)
          + ' (' + (CONDICOES[f.condicaoPagamento] || f.condicaoPagamento) + ').', 'sucesso');
        carregarPedidos();
      } else if (r.status === 409) {
        mostrarMensagemRev('Este pedido já foi revisto.', 'erro');
        carregarPedidos();
      } else if (r.status === 404) {
        mostrarMensagemRev('Pedido ou produto não encontrado.', 'erro');
      } else if (r.status === 400) {
        mostrarMensagemRev('Revisão inválida. Verifique os produtos (sem repetidos) e as quantidades.', 'erro');
      } else {
        mostrarMensagemRev('Erro inesperado (código ' + r.status + ').', 'erro');
      }
    } catch (e) {
      mostrarMensagemRev('Não foi possível conectar ao servidor.', 'erro');
    } finally {
      revFinalizar.disabled = false;
    }
  });

  btnAtualizar.addEventListener('click', carregarPedidos);
  carregarFormulario();
  carregarPedidos();
})();
