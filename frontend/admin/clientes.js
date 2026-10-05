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


  // ---- Registo de cliente pelo funcionário (mesmo endpoint do site) ----
  const form = document.getElementById('form-cliente');
  const botaoGuardar = document.getElementById('btn-guardar');
  const mensagem = document.getElementById('mensagem');

  function mostrarMensagem(texto, tipo) {
    mensagem.textContent = texto;
    mensagem.className = 'mensagem ' + tipo;
    mensagem.hidden = false;
  }

  form.addEventListener('submit', async function (evento) {
    evento.preventDefault();
    mensagem.hidden = true;
    if (!form.checkValidity()) { form.reportValidity(); return; }

    const c = form.elements;
    botaoGuardar.disabled = true;
    try {
      const resposta = await fetch(API_BASE + '/clientes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          nome: c.nome.value.trim(),
          email: c.email.value.trim(),
          telefone: c.telefone.value.trim(),
          endereco: c.endereco.value.trim()
        })
      });
      if (resposta.status === 201) {
        const criado = await resposta.json();
        mostrarMensagem('Cliente "' + criado.nome + '" registado (ID ' + criado.idCliente + ').', 'sucesso');
        form.reset();
        carregarClientes();
      } else if (resposta.status === 409) {
        mostrarMensagem('Este email já está registado.', 'erro');
      } else if (resposta.status === 400) {
        mostrarMensagem('Dados inválidos. Verifique o nome e o email.', 'erro');
      } else {
        mostrarMensagem('Erro inesperado (código ' + resposta.status + ').', 'erro');
      }
    } catch (erro) {
      mostrarMensagem('Não foi possível conectar ao servidor. Verifique se o back-end está a correr.', 'erro');
    } finally {
      botaoGuardar.disabled = false;
    }
  });

  botaoAtualizar.addEventListener('click', carregarClientes);
  carregarClientes();
})();
