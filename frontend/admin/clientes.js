(function () {
  const corpo = document.getElementById('corpo-tabela');
  const contagem = document.getElementById('contagem');
  const botaoAtualizar = document.getElementById('btn-atualizar');
  const COLUNAS = 6;

  // Mostra uma linha de estado (a carregar, vazio, erro) ocupando a tabela toda.
  function mostrarEstado(texto) {
    corpo.replaceChildren();
    const linha = document.createElement('tr');
    const celula = document.createElement('td');
    celula.colSpan = COLUNAS;
    celula.className = 'estado';
    celula.textContent = texto;
    linha.appendChild(celula);
    corpo.appendChild(linha);
  }

  // "2026-10-03" -> "03/10/2026" (sem passar por Date, para evitar desvios de fuso horário)
  function formatarData(iso) {
    if (!iso) return '';
    const partes = String(iso).split('-');
    return partes.length === 3 ? partes[2] + '/' + partes[1] + '/' + partes[0] : iso;
  }

  function renderizar(clientes) {
    corpo.replaceChildren();
    clientes.forEach(function (c) {
      const linha = document.createElement('tr');
      [c.idCliente, c.nome, c.email, c.telefone, c.endereco, formatarData(c.dataCadastro)]
        .forEach(function (valor) {
          const celula = document.createElement('td');
          // textContent (e não innerHTML): impede injeção de HTML vinda dos formulários públicos.
          celula.textContent = valor == null ? '' : valor;
          linha.appendChild(celula);
        });
      corpo.appendChild(linha);
    });
  }

  async function carregarClientes() {
    botaoAtualizar.disabled = true;
    contagem.textContent = '';
    mostrarEstado('A carregar clientes...');

    try {
      const resposta = await fetch(API_BASE + '/clientes');
      if (!resposta.ok) {
        throw new Error('HTTP ' + resposta.status);
      }
      const clientes = await resposta.json();

      if (clientes.length === 0) {
        mostrarEstado('Ainda não há clientes registados.');
      } else {
        renderizar(clientes);
      }
      contagem.textContent = clientes.length + (clientes.length === 1 ? ' cliente' : ' clientes');
    } catch (erro) {
      mostrarEstado('Não foi possível carregar os clientes. Verifique se o back-end está a correr.');
    } finally {
      botaoAtualizar.disabled = false;
    }
  }

  botaoAtualizar.addEventListener('click', carregarClientes);
  carregarClientes();
})();
