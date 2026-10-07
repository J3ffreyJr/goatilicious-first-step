(function () {
  const CONDICOES = { A_VISTA: 'À vista', CARTAO: 'Cartão', PAGO_NA_ENTREGA: 'Pagamento na entrega' };
  const ESTADOS = { RASCUNHO: 'Rascunho', FINAL: 'Final' };
  const COLUNAS = 7;

  const corpo = document.getElementById('corpo-tabela');
  const contagem = document.getElementById('contagem');
  const btnAtualizar = document.getElementById('btn-atualizar');

  function mt(v) {
    return Number(v).toLocaleString('pt-PT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' MT';
  }

  function formatarData(iso) {
    if (!iso) return '';
    const m = String(iso).match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
    return m ? m[3] + '/' + m[2] + '/' + m[1] + ' ' + m[4] + ':' + m[5] : iso;
  }

  function mostrarEstado(texto) {
    corpo.replaceChildren();
    const tr = document.createElement('tr');
    const td = document.createElement('td');
    td.colSpan = COLUNAS; td.className = 'estado'; td.textContent = texto;
    tr.appendChild(td);
    corpo.appendChild(tr);
  }

  function celula(tr, valor) {
    const td = document.createElement('td');
    td.textContent = valor == null ? '' : valor;   // textContent: nunca interpreta HTML
    tr.appendChild(td);
    return td;
  }

  async function carregar() {
    btnAtualizar.disabled = true;
    contagem.textContent = '';
    mostrarEstado('A carregar facturas...');
    try {
      const r = await fetch(API_BASE + '/facturas');
      if (!r.ok) throw new Error('HTTP ' + r.status);
      const facturas = await r.json();
      facturas.sort(function (a, b) { return b.idFactura - a.idFactura; });

      if (facturas.length === 0) {
        mostrarEstado('Ainda não há facturas. Elas são geradas quando um pedido é registado.');
      } else {
        corpo.replaceChildren();
        facturas.forEach(function (f) {
          const tr = document.createElement('tr');
          celula(tr, f.idFactura);
          celula(tr, f.idPedido);
          celula(tr, f.nomeCliente);
          celula(tr, mt(f.valorTotal));
          celula(tr, CONDICOES[f.condicaoPagamento] || f.condicaoPagamento);
          const estado = celula(tr, '');
          const selo = document.createElement('span');
          selo.className = 'selo' + (f.estado === 'FINAL' ? ' final' : '');
          selo.textContent = ESTADOS[f.estado] || f.estado;
          estado.appendChild(selo);
          celula(tr, formatarData(f.dataEmissao));
          corpo.appendChild(tr);
        });
      }
      contagem.textContent = facturas.length + (facturas.length === 1 ? ' factura' : ' facturas');
    } catch (e) {
      mostrarEstado('Não foi possível carregar as facturas. Verifique se o back-end está a correr.');
    } finally {
      btnAtualizar.disabled = false;
    }
  }

  btnAtualizar.addEventListener('click', carregar);
  carregar();
})();
