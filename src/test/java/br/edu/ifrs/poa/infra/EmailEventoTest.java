package br.edu.ifrs.poa.infra;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class EmailEventoTest {

  static final String SMTP4DEV = "http://localhost:5000";

  @Inject
  EmailEvento emailEvento;

  private final HttpClient client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5)).build();
  private final ObjectMapper mapper = new ObjectMapper();

  @AfterEach
  void limpaCaixa() {
    limparMensagens();
  }

  void limparMensagens() {
    try {
      client.send(
          HttpRequest.newBuilder(URI.create(SMTP4DEV + "/api/Messages/*")).DELETE().build(),
          HttpResponse.BodyHandlers.discarding());
    } catch (Exception e) {
      // ignorado
    }
  }

  List<JsonNode> mensagensPara(String destinatario) throws Exception {
    var uri = URI.create(SMTP4DEV + "/api/Messages?mailboxName=Default&pageSize=100");
    var request = HttpRequest.newBuilder(uri).GET().build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    var root = mapper.readTree(response.body());
    var resultados = new java.util.ArrayList<JsonNode>();
    for (var node : root.get("results")) {
      boolean corresponde = false;
      var para = node.get("to");
      if (para != null && para.isArray()) {
        for (var destino : para) {
          if (destino.asText().equalsIgnoreCase(destinatario)) {
            corresponde = true;
          }
        }
      }
      if (corresponde) {
        resultados.add(node);
      }
    }
    return resultados;
  }

  String corpoDaMensagem(String id) throws Exception {
    var uri = URI.create(SMTP4DEV + "/api/Messages/" + id + "/source");
    var request = HttpRequest.newBuilder(uri).GET().build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  JsonNode aguardaMensagem(String destinatario, String fragmentoAssunto, int tentativas) throws Exception {
    for (int i = 0; i < tentativas; i++) {
      var mensagens = mensagensPara(destinatario);
      for (var m : mensagens) {
        var assunto = m.get("subject") != null ? m.get("subject").asText() : "";
        if (assunto.contains(fragmentoAssunto)) {
          return m;
        }
      }
      Thread.sleep(300);
    }
    return null;
  }

  @Test
  void homologadoEnviaParaAlunoEInteressado() throws Exception {
    var aluno = "aluno-homologado@example.com";
    var interessado = "interessado-homologado@example.com";

    emailEvento.enviar("homologado", aluno, Map.of(
        "aluno", "Maria",
        "alunoEmail", aluno,
        "estado", "homologado",
        "titulo", "Curso de Java",
        "observacao", "Ok"));

    var msgAluno = aguardaMensagem(aluno, "TST homologado - Curso de Java", 40);
    var msgInteressado = aguardaMensagem(interessado, "TST homologado - Curso de Java", 40);

    assertTrue(msgAluno != null, "Aluno deve receber o email de homologado");
    assertTrue(msgInteressado != null, "Interessado deve receber o email de homologado");

    var corpoAluno = corpoDaMensagem(msgAluno.get("id").asText());
    assertTrue(corpoAluno.contains("Curso de Java"), "Corpo deve conter o titulo");
  }

  @Test
  void rejeitadoEnviaParaAlunoEInteressado() throws Exception {
    var aluno = "aluno-rejeitado@example.com";
    var interessado = "interessado-rejeitado@example.com";

    emailEvento.enviar("rejeitado", aluno, Map.of(
        "aluno", "Joao",
        "alunoEmail", aluno,
        "estado", "rejeitado",
        "titulo", "Palestra",
        "observacao", "Documento invalido"));

    assertTrue(aguardaMensagem(aluno, "TST rejeitado - Palestra", 40) != null, "Aluno deve receber");
    assertTrue(aguardaMensagem(interessado, "TST rejeitado - Palestra", 40) != null, "Interessado deve receber");
  }

  @Test
  void recebidoEnviaParaAlunoEInteressado() throws Exception {
    var aluno = "aluno-recebido@example.com";
    var interessado = "interessado-recebido@example.com";

    emailEvento.enviar("recebido", aluno, Map.of(
        "aluno", "Ana",
        "alunoEmail", aluno,
        "titulo", "Curso de Python",
        "horas", 40.0));

    var msgAluno = aguardaMensagem(aluno, "TST recebido - Curso de Python", 40);
    var msgInteressado = aguardaMensagem(interessado, "TST recebido - Curso de Python", 40);

    assertTrue(msgAluno != null, "Aluno deve receber o email de recebido");
    assertTrue(msgInteressado != null, "Interessado deve receber o email de recebido");

    var corpo = corpoDaMensagem(msgAluno.get("id").asText());
    assertTrue(corpo.contains("40.0"), "Corpo deve conter as horas renderizadas");
  }

  @Test
  void recebidoEnviaSomenteParaAlunoQuandoNaoHaInteressados() throws Exception {
    var aluno = "aluno-sem-interessado@example.com";

    emailEvento.enviar("pendente", aluno, Map.of(
        "aluno", "Bia",
        "alunoEmail", aluno,
        "estado", "pendente",
        "titulo", "Estagio",
        "observacao", "Em analise"));

    assertTrue(aguardaMensagem(aluno, "TST pendente - Estagio", 40) != null, "Aluno deve receber");
  }

  @Test
  void concluidoEnviaComAnexo() throws Exception {
    var aluno = "aluno-concluido@example.com";
    var interessado = "interessado-concluido@example.com";
    var pdf = Path.of("target", "teste-anexo.pdf");
    Files.createDirectories(pdf.getParent());
    Files.write(pdf, new byte[] { 0x25, 0x50, 0x44, 0x46 });

    emailEvento.enviar("concluido", aluno, Map.of(
        "aluno", "Carlos",
        "alunoEmail", aluno,
        "horas", 72.0), pdf);

    var msgAluno = aguardaMensagem(aluno, "TST concluido - Carlos", 40);
    var msgInteressado = aguardaMensagem(interessado, "TST concluido - Carlos", 40);

    assertTrue(msgAluno != null, "Aluno deve receber o email de conclusao");
    assertTrue(msgInteressado != null, "Interessado deve receber o email de conclusao");
    assertEquals(1, msgAluno.get("attachmentCount").asInt(), "Deve conter um anexo");
  }
}
