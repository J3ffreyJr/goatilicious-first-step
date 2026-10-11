package com.goatilicious.mail;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    @Mock
    private ObjectProvider<JavaMailSender> provider;
    @Mock
    private JavaMailSender javaMailSender;

    private SmtpEmailSender sender;

    @BeforeEach
    void preparar() {
        sender = new SmtpEmailSender(provider, "loja@goatilicious.test");
    }

    private void comSmtpConfigurado() {
        when(provider.getIfAvailable()).thenReturn(javaMailSender);
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    private MimeMessage mensagemEnviada() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        return captor.getValue();
    }

    private boolean temAnexo(Part parte, String nome) throws Exception {
        if (nome.equals(parte.getFileName())) {
            return true;
        }
        Object conteudo = parte.getContent();
        if (conteudo instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                if (temAnexo(multipart.getBodyPart(i), nome)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void enviaComAssuntoDestinatarioERemetente() throws Exception {
        comSmtpConfigurado();

        sender.enviar("ana@mail.com", "Factura 7", "Segue a factura.", "factura-7.pdf", new byte[]{1, 2, 3});

        MimeMessage mensagem = mensagemEnviada();
        assertEquals("Factura 7", mensagem.getSubject());
        assertEquals("ana@mail.com", mensagem.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("loja@goatilicious.test", mensagem.getFrom()[0].toString());
    }

    @Test
    void anexaOPdfComONomeIndicado() throws Exception {
        comSmtpConfigurado();

        sender.enviar("ana@mail.com", "Factura 7", "Segue a factura.", "factura-7.pdf", new byte[]{1, 2, 3});

        assertTrue(temAnexo(mensagemEnviada(), "factura-7.pdf"));
    }

    @Test
    void semJavaMailSenderLancaEmailNaoConfigurado() {
        when(provider.getIfAvailable()).thenReturn(null);

        assertThrows(EmailNaoConfiguradoException.class,
                () -> sender.enviar("ana@mail.com", "Assunto", "Texto", "f.pdf", new byte[]{1}));

        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void falhaDoSmtpLancaEmailFalhouComAMensagemOriginal() {
        comSmtpConfigurado();
        doThrow(new MailSendException("SMTP em baixo")).when(javaMailSender).send(any(MimeMessage.class));

        EmailFalhouException erro = assertThrows(EmailFalhouException.class,
                () -> sender.enviar("ana@mail.com", "Assunto", "Texto", "f.pdf", new byte[]{1}));

        assertTrue(erro.getMessage().contains("SMTP em baixo"), erro.getMessage());
    }
}
