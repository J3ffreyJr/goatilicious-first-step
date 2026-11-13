import '../shared/config.js';
import '../shared/auth.js';

(function () {
  const form = document.getElementById('form-login');
  const botao = document.getElementById('btn-entrar');
  const mensagem = document.getElementById('mensagem');

  function mostrarMensagem(texto, tipo) {
    mensagem.textContent = texto;            // textContent: nunca interpreta HTML
    mensagem.className = 'mensagem ' + tipo;
    mensagem.hidden = false;
  }

  if (new URLSearchParams(window.location.search).has('expirou')) {
    mostrarMensagem('A sua sessão terminou. Entre novamente.', 'erro');
  }

  // Já tem sessão de staff: vai direto ao painel.
  if (GoatAuth.sessao('staff')) window.location.replace('/admin/dashboard.html');

  form.addEventListener('submit', async function (evento) {
    evento.preventDefault();
    mensagem.hidden = true;
    if (!form.checkValidity()) { form.reportValidity(); return; }

    botao.disabled = true;
    try {
      const r = await GoatAuth.entrar('staff', form.elements.email.value, form.elements.senha.value);
      if (r.ok) {
        window.location.replace('/admin/dashboard.html');
      } else if (r.motivo === 'credenciais') {
        mostrarMensagem('Email ou palavra-passe incorretos.', 'erro');
      } else if (r.motivo === 'papel') {
        mostrarMensagem('Esta conta não tem acesso ao painel.', 'erro');
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
