package br.edu.ifrs.poa.infra;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static br.edu.ifrs.poa.model.atividades.AtividadesRepository.*;

@Singleton
public class PdfService {
  @ConfigProperty(name = "br.edu.ifrs.poa.atividades-complementares.uploads-folder")
  public String caminhoDestino;

  @Nullable
  public String agregarArquivos(EnvelopeDTO envelopeDTO)
      throws IOException {

    var utility = new PDFMergerUtility();

    var destino = Path.of(
        caminhoDestino,
        UUID.randomUUID() + ".pdf").toString();

    utility.setDestinationFileName(destino);

    for (var atividade : envelopeDTO.atividades()) {

      // página descritiva
      var descricaoPdf = criarPaginaDescricao(atividade);
      utility.addSource(new File(descricaoPdf));
      var partesDoCaminho = atividade.certificado().split("/");

      // certificado original
      var caminhoArquivo = Path.of(
          caminhoDestino,
          partesDoCaminho[partesDoCaminho.length - 1]).toString();

      utility.addSource(new File(caminhoArquivo));
    }

    try {
      utility.mergeDocuments(null);
      return destino;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private String criarPaginaDescricao(AtividadeDTO atividade)
      throws IOException {

    var arquivo = Path.of(
        caminhoDestino,
        "descricao-" + UUID.randomUUID() + ".pdf").toString();

    Document document = new Document(PageSize.A4);

    try {
      PdfWriter.getInstance(document, new FileOutputStream(arquivo));

      document.open();

      var titulo = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
      var normal = new Font(Font.FontFamily.HELVETICA, 12);

      Paragraph header = new Paragraph(
          "Atividade Complementar",
          titulo);

      header.setSpacingAfter(20f);

      document.add(header);

      document.add(new Paragraph(
          "Tipo: " + atividade.tipoAtividade(),
          normal));

      document.add(new Paragraph(
          "Título: " + atividade.titulo(),
          normal));

      document.add(new Paragraph(
          "Horas homologadas: " + atividade.horas(),
          normal));

      if (atividade.observacao() != null &&
          !atividade.observacao().isBlank()) {

        Paragraph observacao = new Paragraph("Observação: " + atividade.observacao(), normal);

        observacao.setSpacingBefore(15f);

        document.add(observacao);
      }

    } catch (DocumentException e) {
      throw new IOException(e);
    } finally {
      document.close();
    }

    return arquivo;
  }
}
