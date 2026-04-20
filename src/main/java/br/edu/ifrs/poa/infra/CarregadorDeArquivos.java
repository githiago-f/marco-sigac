package br.edu.ifrs.poa.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.inject.Singleton;

@Singleton
public class CarregadorDeArquivos {
  @ConfigProperty(name = "br.edu.ifrs.poa.atividades-complementares.uploads-folder")
  public String caminhoDestino;

  public String persistir(FileUpload arquivo) throws IOException {
    if (!arquivo.contentType().equals("application/pdf")) {
      System.out.println(arquivo.contentType());
      throw new RuntimeException("Arquivos aceitos: pdf");
    }
    Files.createDirectories(Path.of(caminhoDestino));

    String uuid = UUID.randomUUID().toString() + ".pdf";

    var destino = caminhoDestino + uuid;
    Files.move(arquivo.filePath(), Path.of(destino));

    return "/atividades/certificados/" + uuid;
  }
}
