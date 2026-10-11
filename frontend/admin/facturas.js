(function () {
  const CONDICOES = { A_VISTA: 'À vista', CARTAO: 'Cartão', PAGO_NA_ENTREGA: 'Pagamento na entrega' };
  const ESTADOS = { RASCUNHO: 'Rascunho', FINAL: 'Final' };
  const COLUNAS = 8;

  const corpo = document.getElementById('corpo-tabela');
  const contagem = document.getElementById('contagem');
  const btnAtualizar = document.getElementById('btn-atualizar');
  const mensagem = document.getElementById('mensagem');
  const painel = document.getElementById('painel-historico');
  const historicoTitulo = document.getElementById('historico-titulo');
  const corpoHistorico = document.getElementById('corpo-historico');
  const btnFecharHistorico = document.getElementById('btn-fechar-historico');

  function mt(v) {
    return Number(v).toLocaleString('pt-PT', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' MT';
  }

  function formatarData(iso) {
    if (!iso) return '';
    const m = String(iso).match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
    return m ? m[3] + '/' + m[2] + '/' + m[1] + ' ' + m[4] + ':' + m[5] : iso;
  }

  function mostrarMensagem(texto, tipo) {
    mensagem.textContent = texto;
    mensagem.className = 'mensagem ' + tipo;
    mensagem.hidden = false;
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

  // ---------- ações da fatura final ----------
  function imprimir(f) {
    // O PDF abre numa nova aba; dali o funcionário imprime ou grava.
    window.open(API_BASE + '/facturas/' + f.idFactura + '/pdf', '_blank', 'noopener');
  }

  async function enviar(f, botao) {
    mensagem.hidden = true;
    const destino = f.emailCliente || 'o cliente';
    if (!window.confirm('Enviar a factura ' + f.idFactura + ' para ' + destino + '?')) return;

    botao.disabled = true;
    try {
      const r = await fetch(API_BASE + '/facturas/' + f.idFactura + '/envio', { method: 'POST' });
      if (r.ok) {
        const e = await r.json();
        mostrarMensagem('Factura ' + f.idFactura + ' enviada para ' + e.destinatario + '.', 'sucesso');
      } else if (r.status === 409) {
        mostrarMensagem('A factura ainda é um rascunho. Reveja o pedido antes de a enviar.', 'erro');
      } else if (r.status === 404) {
        mostrarMensagem('Factura não encontrada.', 'erro');
      } else if (r.status === 503) {
        mostrarMensagem('O envio de e-mail não está configurado no servidor (veja o README).', 'erro');
      } else if (r.status === 502) {
        mostrarMensagem('Não foi possível enviar o e-mail. A falha ficou registada no histórico; tente novamente.', 'erro');
      } else {
        mostrarMensagem('Erro inesperado (código ' + r.status + ').', 'erro');
      }
    } catch (e) {
      mostrarMensagem('Não foi possível conectar ao servidor.', 'erro');
    } finally {
      botao.disabled = false;
    }
  }

  async function verHistorico(f) {
    mensagem.hidden = true;
    try {
      const r = await fetch(API_BASE + '/facturas/' + f.idFactura + '/envios');
      if (!r.ok) throw new Error('HTTP ' + r.status);
      const envios = await r.json();

      historicoTitulo.textContent = 'Histórico de envios — factura ' + f.idFactura;
      corpoHistorico.replaceChildren();
      if (envios.length === 0) {
        const tr = document.createElement('tr');
        const td = document.createElement('td');
        td.colSpan = 3; td.className = 'estado'; td.textContent = 'Esta factura ainda não foi enviada.';
        tr.appendChild(td);
        corpoHistorico.appendChild(tr);
      } else {
        envios.forEach(function (e) {
          const tr = document.createElement('tr');
          celula(tr, formatarData(e.dataEnvio));
          celula(tr, e.destinatario);
          celula(tr, e.sucesso ? 'Enviada' : 'Falhou: ' + (e.erro || 'erro desconhecido'));
          corpoHistorico.appendChild(tr);
        });
      }
      painel.hidden = false;
      painel.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (e) {
      mostrarMensagem('Não foi possível carregar o histórico de envios.', 'erro');
    }
  }

  function botao(texto, aoClicar) {
    const b = document.createElement('button');
    b.type = 'button';
    b.textContent = texto;
    b.addEventListener('click', function () { aoClicar(b); });
    return b;
  }

  function acoes(tr, f) {
    const td = document.createElement('td');
    if (f.estado === 'FINAL') {
      td.append(
        botao('Imprimir PDF', function () { imprimir(f); }),
        ' ',
        botao('Enviar por e-mail', function (b) { enviar(f, b).then(carregar); }),
        ' ',
        botao('Histórico', function () { verHistorico(f); })
      );
    }
    tr.appendChild(td);
  }

  // ---------- lista ----------
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
          acoes(tr, f);
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

  btnFecharHistorico.addEventListener('click', function () { painel.hidden = true; });
  btnAtualizar.addEventListener('click', carregar);
  carregar();
})();
