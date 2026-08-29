package br.edu.ifrs.poa.infra;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;

/**
 * Simula um usuário final definindo a configuração de um evento via variável de
 * ambiente (representada aqui por um @TestProfile, que injeta a chave de config
 * da mesma forma que a env var BR_EDU_IFRS_POA_ATIVIDADES_COMPLEMENTARES_*).
 */
@QuarkusTest
@TestProfile(EmailEventoSobPerfilTest.Perfil.class)
class EmailEventoSobPerfilTest {

  public static class Perfil implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of(
          "br.edu.ifrs.poa.atividades-complementares.homologado.receivers",
          "interessado-perfil@example.com",
          "br.edu.ifrs.poa.atividades-complementares.homologado.subject",
          "Perfil homologado - {titulo}",
          "br.edu.ifrs.poa.atividades-complementares.homologado.message-template",
          "Atividade {titulo} homologada para {aluno}.");
    }
  }

  static final String SMTP4DEV = "http://localhost:5000";

  @Inject
  EmailEvento emailEvento;

  private final HttpClient client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5)).build();
  private final ObjectMapper mapper = new ObjectMapper();

  @AfterEach
  void limpa() {
    try {
      client.send(
          HttpRequest.newBuilder(URI.create(SMTP4DEV + "/api/Messages/*")).DELETE().build(),
          HttpResponse.BodyHandlers.discarding());
    } catch (Exception e) {
      // ignorado
    }
  }

  JsonNode aguarda(String destinatario, String fragmentoAssunto, int tentativas) throws Exception {
    for (int i = 0; i < tentativas; i++) {
      var req = HttpRequest.newBuilder(
          URI.create(SMTP4DEV + "/api/Messages?mailboxName=Default&pageSize=100")).GET().build();
      var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
      var root = mapper.readTree(resp.body());
      for (var node : root.get("results")) {
        var para = node.get("to");
        boolean ehDestino = false;
        if (para != null && para.isArray()) {
          for (var destino : para) {
            if (destino.asText().equalsIgnoreCase(destinatario)) {
              ehDestino = true;
            }
          }
        }
        if (ehDestino && node.get("subject").asText().contains(fragmentoAssunto)) {
          return node;
        }
      }
      Thread.sleep(300);
    }
    return null;
  }

  @Test
  void configuracoesDeEnvControlamRecipientesEConteudo() throws Exception {
    var aluno = "aluno-perfil@example.com";
    var interessado = "interessado-perfil@example.com";

    emailEvento.enviar("homologado", aluno, Map.of(
        "aluno", "Paula",
        "alunoEmail", aluno,
        "estado", "homologado",
        "titulo", "Curso de Docker",
        "observacao", "Ok"));

    assertTrue(aguarda(aluno, "Perfil homologado - Curso de Docker", 40) != null,
        "Aluno deve receber com assunto definido pela env var");
    assertTrue(aguarda(interessado, "Perfil homologado - Curso de Docker", 40) != null,
        "Interessado da env var deve receber");
  }
}
