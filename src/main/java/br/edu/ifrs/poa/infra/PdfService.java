package br.edu.ifrs.poa.infra;

import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;

import static br.edu.ifrs.poa.model.atividades.AtividadesRepository.*;

@Singleton
public class PdfService {
  private static final float MARGEM = 28;
  private static final float ALTURA_DO_CABECALHO = 92;
  private static final float ESPACO_ANTES_DO_CERTIFICADO = 12;
  private static final PDFont FONTE_TITULO = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
  private static final PDFont FONTE_TEXTO = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

  @ConfigProperty(name = "br.edu.ifrs.poa.atividades-complementares.uploads-folder")
  public String caminhoDestino;

  @Nullable
  public String agregarArquivos(EnvelopeDTO envelopeDTO)
      throws IOException {

    var utility = new PDFMergerUtility();
    var destinoPdf = UUID.randomUUID() + ".pdf";

    var destino = Path.of(caminhoDestino, destinoPdf).toString();

    utility.setDestinationFileName(destino);
    var temporarios = new ArrayList<String>();

    for (var atividade : envelopeDTO.atividades()) {
      var partesDoCaminho = atividade.certificado().split("/");
      var nomeDoArquivoOriginal = partesDoCaminho[partesDoCaminho.length - 1];
      var arquivoOriginal = Path.of(caminhoDestino, nomeDoArquivoOriginal).toFile();

      var pdfMarcado = adicionarDescricao(arquivoOriginal, atividade);
      temporarios.add(pdfMarcado.getAbsolutePath());

      utility.addSource(pdfMarcado);
    }

    try {
      utility.mergeDocuments(null);
      for (var temp : temporarios) {
        Files.delete(Path.of(temp));
      }

      return destinoPdf;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private File adicionarDescricao(File pdfOriginal, AtividadeDTO atividade) throws IOException {

    var destino = Path.of(
        caminhoDestino,
        "tmp-" + UUID.randomUUID() + ".pdf").toFile();

    try (PDDocument original = Loader.loadPDF(pdfOriginal);
        PDDocument marcado = new PDDocument()) {
      var utilitarioDeCamadas = new LayerUtility(marcado);

      for (int indice = 0; indice < original.getNumberOfPages(); indice++) {
        if (indice == 0) {
          adicionarPrimeiraPaginaComDescricao(original, marcado, utilitarioDeCamadas, atividade);
        } else {
          marcado.importPage(original.getPage(indice));
        }
      }

      marcado.save(destino);
    }

    return destino;
  }

  private void adicionarPrimeiraPaginaComDescricao(
      PDDocument original,
      PDDocument marcado,
      LayerUtility utilitarioDeCamadas,
      AtividadeDTO atividade) throws IOException {

    var paginaOriginal = original.getPage(0);
    var mediaBox = paginaOriginal.getMediaBox();
    var novaPagina = new PDPage(mediaBox);

    marcado.addPage(novaPagina);

    var certificado = utilitarioDeCamadas.importPageAsForm(original, paginaOriginal);

    try (PDPageContentStream contentStream = new PDPageContentStream(marcado, novaPagina)) {
      desenharCabecalho(contentStream, mediaBox, atividade);
      desenharCertificado(contentStream, certificado, mediaBox);
    }
  }

  private void desenharCabecalho(
      PDPageContentStream contentStream,
      PDRectangle mediaBox,
      AtividadeDTO atividade) throws IOException {

    var largura = mediaBox.getWidth();
    var topo = mediaBox.getHeight();
    var larguraUtil = largura - (MARGEM * 2);
    var baseCabecalho = topo - ALTURA_DO_CABECALHO;

    contentStream.setNonStrokingColor(0.98f, 0.99f, 0.98f);
    contentStream.addRect(0, baseCabecalho, largura, ALTURA_DO_CABECALHO);
    contentStream.fill();

    contentStream.setNonStrokingColor(0.10f, 0.42f, 0.28f);
    contentStream.addRect(0, baseCabecalho, 7, ALTURA_DO_CABECALHO);
    contentStream.fill();

    contentStream.setStrokingColor(0.82f, 0.86f, 0.84f);
    contentStream.setLineWidth(0.6f);
    contentStream.moveTo(MARGEM, baseCabecalho + 2);
    contentStream.lineTo(largura - MARGEM, baseCabecalho + 2);
    contentStream.stroke();

    contentStream.beginText();
    contentStream.setNonStrokingColor(0.10f, 0.12f, 0.11f);
    contentStream.setFont(FONTE_TITULO, 13);
    contentStream.newLineAtOffset(MARGEM, topo - 30);
    contentStream.showText("Atividade complementar homologada");

    contentStream.setFont(FONTE_TEXTO, 10);
    contentStream.newLineAtOffset(0, -20);
    contentStream.showText(textoQueCabe("Tipo: " + atividade.tipoAtividade(), FONTE_TEXTO, 10, larguraUtil));

    contentStream.newLineAtOffset(0, -17);
    contentStream.showText(textoQueCabe(
        "Titulo: " + atividade.titulo() + " | Horas: " + atividade.horas(),
        FONTE_TEXTO,
        10,
        larguraUtil));
    contentStream.endText();
  }

  private void desenharCertificado(
      PDPageContentStream contentStream,
      PDFormXObject certificado,
      PDRectangle mediaBox) throws IOException {

    var larguraDisponivel = mediaBox.getWidth() - (MARGEM * 2);
    var alturaDisponivel = mediaBox.getHeight() - ALTURA_DO_CABECALHO - ESPACO_ANTES_DO_CERTIFICADO - MARGEM;
    var escala = Math.min(
        larguraDisponivel / mediaBox.getWidth(),
        alturaDisponivel / mediaBox.getHeight());

    var larguraDoCertificado = mediaBox.getWidth() * escala;
    var alturaDoCertificado = mediaBox.getHeight() * escala;
    var x = (mediaBox.getWidth() - larguraDoCertificado) / 2;
    var y = MARGEM + ((alturaDisponivel - alturaDoCertificado) / 2);

    contentStream.saveGraphicsState();
    contentStream.transform(new Matrix(escala, 0, 0, escala, x, y));
    contentStream.drawForm(certificado);
    contentStream.restoreGraphicsState();
  }

  private String textoQueCabe(String texto, PDFont fonte, float tamanho, float larguraMaxima) throws IOException {
    var textoNormalizado = texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    if (largura(textoNormalizado, fonte, tamanho) <= larguraMaxima) {
      return textoNormalizado;
    }

    var sufixo = "...";
    var limite = larguraMaxima - largura(sufixo, fonte, tamanho);
    var resultado = new StringBuilder();

    for (int indice = 0; indice < textoNormalizado.length(); indice++) {
      var proximo = resultado.toString() + textoNormalizado.charAt(indice);
      if (largura(proximo, fonte, tamanho) > limite) {
        break;
      }
      resultado.append(textoNormalizado.charAt(indice));
    }

    return resultado.toString().stripTrailing() + sufixo;
  }

  private float largura(String texto, PDFont fonte, float tamanho) throws IOException {
    return fonte.getStringWidth(texto) / 1000 * tamanho;
  }
}
