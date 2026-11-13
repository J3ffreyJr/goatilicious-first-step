(function () {
  const TAMANHOS = { ML_125: '125 ml', ML_500: '500 ml', L_1: '1 L', L_5: '5 L' };
  const mensagem = document.getElementById('mensagem');

  function mt(valor) {
    return Number(valor).toLocaleString('pt-PT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' MT';
  }

  function definir(id, texto) { document.getElementById(id).textContent = texto; }   // textContent: nunca interpreta HTML

  function el(tag, classe, texto) {
    const e = document.createElement(tag);
    if (classe) e.className = classe;
    if (texto != null) e.textContent = texto;
    return e;
  }

  function mostrarMensagem(texto, tipo) {
    mensagem.textContent = texto;
    mensagem.className = 'mensagem ' + tipo;
    mensagem.hidden = false;
  }

  function vazio(lista) {
    lista.replaceChildren(el('li', 'dash-vazio', 'Ainda sem dados.'));
  }

  function pintarProdutos(produtos) {
    const lista = document.getElementById('lista-produtos');
    if (produtos.length === 0) { vazio(lista); return; }
    const maximo = Math.max.apply(null, produtos.map(function (p) { return p.quantidade; }));
    lista.replaceChildren();
    produtos.forEach(function (p) {
      const li = el('li', 'dash-item');
      const topo = el('div', 'dash-item-topo');
      topo.appendChild(el('span', null, p.sabor + ' · ' + (TAMANHOS[p.tamanho] || p.tamanho)));
      topo.appendChild(el('strong', null, p.quantidade + ' unid.'));
      const barra = el('div', 'dash-barra');
      const enchimento = el('div', 'dash-barra-cheia');
      enchimento.style.width = Math.max(4, Math.round((p.quantidade / maximo) * 100)) + '%';
      barra.appendChild(enchimento);
      li.append(topo, barra);
      lista.appendChild(li);
    });
  }

  function pintarClientes(clientes) {
    const lista = document.getElementById('lista-clientes');
    if (clientes.length === 0) { vazio(lista); return; }
    lista.replaceChildren();
    clientes.forEach(function (c) {
      const li = el('li', 'dash-item');
      const topo = el('div', 'dash-item-topo');
      topo.appendChild(el('span', null, c.nome));
      topo.appendChild(el('strong', null, c.facturas + (c.facturas === 1 ? ' factura' : ' facturas')));
      li.appendChild(topo);
      li.appendChild(el('small', 'dash-sub', mt(c.valorTotal) + ' facturados'));
      lista.appendChild(li);
    });
  }

  async function carregar() {
    mensagem.hidden = true;
    try {
      const r = await GoatAuth.authFetch('staff', API_BASE + '/dashboard');
      if (r.status === 403) { mostrarMensagem('Esta conta não tem acesso ao dashboard.', 'erro'); return; }
      if (!r.ok) throw new Error('HTTP ' + r.status);
      const d = await r.json();

      definir('receita-total', mt(d.receita.total));
      definir('receita-paga', mt(d.receita.paga));
      definir('receita-pendente', mt(d.receita.pendente));
      definir('receita-facturas', String(d.receita.facturasFinais));
      definir('ped-total', String(d.pedidos.total));
      definir('ped-feitos', String(d.pedidos.feitos));
      definir('ped-pendentes', String(d.pedidos.pendentes));
      definir('ped-cancelados', String(d.pedidos.cancelados));
      pintarProdutos(d.produtosMaisPedidos);
      pintarClientes(d.clientesComMaisFacturas);
    } catch (e) {
      mostrarMensagem('Não foi possível carregar o dashboard. Verifique se o back-end está ativo na porta 8080.', 'erro');
    }
  }

  document.getElementById('btn-atualizar').addEventListener('click', carregar);
  carregar();
})();
