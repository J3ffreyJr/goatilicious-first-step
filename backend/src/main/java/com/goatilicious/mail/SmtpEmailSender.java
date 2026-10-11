package com.goatilicious.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envia por SMTP. O {@link JavaMailSender} só existe quando {@code spring.mail.host} está definido
 * (variável de ambiente SPRING_MAIL_HOST); sem ele, o envio falha com {@link EmailNaoConfiguradoException}.
 */
@Component
public class SmtpEmailSender implements EmailSender {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remetente;

    public SmtpEmailSender(ObjectProvider<JavaMailSender> mailSender,
                           @Value("${goatilicious.mail.from}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    @Override
    public void enviar(String para, String assunto, String texto, String nomeAnexo, byte[] anexoPdf) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new EmailNaoConfiguradoException();
        }
        try {
            MimeMessage mensagem = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(para);
            helper.setSubject(assunto);
            helper.setText(texto);
            helper.addAttachment(nomeAnexo, new ByteArrayResource(anexoPdf), "application/pdf");
            sender.send(mensagem);
        } catch (MessagingException | MailException e) {
            throw new EmailFalhouException(e.getMessage(), e);
        }
    }
}
