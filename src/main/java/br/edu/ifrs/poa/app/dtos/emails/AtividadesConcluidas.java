package br.edu.ifrs.poa.app.dtos.emails;

import java.nio.file.Path;

import br.edu.ifrs.poa.infra.EmailService;

public record AtividadesConcluidas(
        String para, Double horas,
        Path arquivoPdf) implements EmailService.MailTemplate {

    @Override
    public String assunto() {
        return "Horas complementares mínimas atingidas";
    }

    @Override
    public Path[] anexos() {
        return new Path[] { arquivoPdf };
    }

    @Override
    public EmailService.Mensagem mensagem() {
        var html = """
                <p>As horas complementares mínimas foram atingidas.</p>
                <p>Total de horas homologadas: <strong>%.2f</strong>.</p>
                <p>O dossiê com os certificados homologados está anexado a este email.</p>
                """
                .formatted(horas);
        var text = """
                As horas complementares mínimas foram atingidas.
                Total de horas homologadas: %.2f.

                O dossiê com os certificados homologados está anexado a este email.
                """
                .formatted(horas);

        return new EmailService.Mensagem(html, text);
    }
}
