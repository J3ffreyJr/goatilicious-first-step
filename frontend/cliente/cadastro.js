(function () {
  const form = document.getElementById('form-cadastro');
  const botao = document.getElementById('btn-enviar');
  const mensagem = document.getElementById('mensagem');

  function mostrarMensagem(texto, tipo) {
    mensagem.textContent = texto;
    mensagem.className = 'mensagem ' + tipo;
    mensagem.hidden = false;
  }

  function lerFormulario() {
    const campos = form.elements;
    return {
      nome: campos.nome.value.trim(),
      email: campos.email.value.trim(),
      telefone: campos.telefone.value.trim(),
      endereco: campos.endereco.value.trim()
    };
  }

  form.addEventListener('submit', async function (evento) {
    evento.preventDefault();
    mensagem.hidden = true;

    if (!form.checkValidity()) {
      form.reportValidity();
      return;
    }

    botao.disabled = true;
    botao.textContent = 'A enviar...';

    try {
      const resposta = await fetch(API_BASE + '/clientes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(lerFormulario())
      });

      if (resposta.status === 201) {
        const cliente = await resposta.json();
        mostrarMensagem('Registo concluído com sucesso! Bem-vindo(a), ' + cliente.nome + '.', 'sucesso');
        form.reset();
      } else if (resposta.status === 409) {
        mostrarMensagem('Este email já está registado.', 'erro');
      } else if (resposta.status === 400) {
        mostrarMensagem('Dados inválidos. Verifique o nome e o email.', 'erro');
      } else {
        mostrarMensagem('Ocorreu um erro inesperado (código ' + resposta.status + '). Tente novamente.', 'erro');
      }
    } catch (erro) {
      mostrarMensagem('Não foi possível ligar ao servidor. Verifique se o back-end está a correr.', 'erro');
    } finally {
      botao.disabled = false;
      botao.textContent = 'Registar';
    }
  });
})();
