import '../src/styles/cadastro.css';
import '../shared/auth.js';
import { initBubbles } from '../src/modules/bubbles.js';
import { initMenu } from '../src/modules/menu.js';

(function () {
  initMenu();
  initBubbles();

  const form = document.getElementById('form-login');
  const botao = document.getElementById('btn-entrar');
  const mensagem = document.getElementById('mensagem');
  const params = new URLSearchParams(window.location.search);

  function mostrarMensagem(texto, tipo) {
    mensagem.textContent = texto;            // textContent: nunca interpreta HTML
    mensagem.className = 'mensagem-box ' + tipo;
    mensagem.hidden = false;
  }

  function seguir() {
    window.location.replace(window.GoatAuth.destinoSeguro(params.get('volta'), '/cliente/pedido.html'));
  }

  if (params.has('expirou')) mostrarMensagem('A sua sessão terminou. Entre novamente.', 'erro');
  if (window.GoatAuth.sessao('cliente')) seguir();

  form.addEventListener('submit', async function (evento) {
    evento.preventDefault();
    mensagem.hidden = true;
    if (!form.checkValidity()) { form.reportValidity(); return; }

    botao.disabled = true;
    try {
      const r = await window.GoatAuth.entrar('cliente', form.elements.email.value, form.elements.senha.value);
      if (r.ok) {
        seguir();
      } else if (r.motivo === 'credenciais') {
        mostrarMensagem('Email ou palavra-passe incorretos.', 'erro');
      } else if (r.motivo === 'papel') {
        mostrarMensagem('Esta conta não é de cliente.', 'erro');
      } else if (r.motivo === 'rede') {
        mostrarMensagem('Não foi possível conectar ao servidor. Verifique se o back-end está ativo na porta 8080.', 'erro');
      } else {
        mostrarMensagem('Ocorreu um erro inesperado. Tente novamente.', 'erro');
      }
    } finally {
      botao.disabled = false;
    }
  });
})();
