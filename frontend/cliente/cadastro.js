import '../src/styles/cadastro.css';
import { initBubbles } from '../src/modules/bubbles.js';
import { initMenu } from '../src/modules/menu.js';

(function () {
  // Inicialização das partículas flutuantes e menu responsivo
  initMenu();
  initBubbles();

  const form = document.getElementById('form-cadastro');
  const botao = document.getElementById('btn-enviar');
  const mensagem = document.getElementById('mensagem');
  const API_BASE = window.API_BASE || 'http://localhost:8080/api';

  function mostrarMensagem(texto, tipo) {
    const iconeSucesso = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>`;
    const iconeErro = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>`;

    mensagem.innerHTML = (tipo === 'sucesso' ? iconeSucesso : iconeErro) + `<span>${texto}</span>`;
    mensagem.className = 'mensagem-box ' + tipo;
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

    const textoOriginal = botao.innerHTML;
    botao.disabled = true;
    botao.innerHTML = `<span>A registar...</span>`;

    try {
      const resposta = await fetch(API_BASE + '/clientes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(lerFormulario())
      });

      if (resposta.status === 201) {
        const cliente = await resposta.json();
        mostrarMensagem('Registo concluído com sucesso! Bem-vindo(a) à Goatilicious, ' + cliente.nome + '.', 'sucesso');
        form.reset();
      } else if (resposta.status === 409) {
        mostrarMensagem('Este endereço de email já se encontra registado.', 'erro');
      } else if (resposta.status === 400) {
        mostrarMensagem('Dados inválidos. Verifique os campos de nome e email.', 'erro');
      } else {
        mostrarMensagem('Ocorreu um erro inesperado (código ' + resposta.status + '). Tente novamente.', 'erro');
      }
    } catch (erro) {
      mostrarMensagem('Não foi possível conectar ao servidor. Verifique se o back-end está ativo na porta 8080.', 'erro');
    } finally {
      botao.disabled = false;
      botao.innerHTML = textoOriginal;
    }
  });
})();
