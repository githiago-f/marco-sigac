package br.edu.ifrs.poa.infra;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.StreamingOutput;

@Singleton
public class ProvedorDeArmazenamento {
  @ConfigProperty(name = "br.edu.ifrs.poa.atividades-complementares.uploads-folder")
  public String caminhoDestino;

  public StreamingOutput lerArquivo(String arquivo) {
    var caminhoArquivo = Path.of(caminhoDestino, arquivo).toString();
    return output -> {
      try (InputStream is = new FileInputStream(caminhoArquivo)) {
        is.transferTo(output);
      } catch (IOException e) {
        e.printStackTrace();
      }
    };
  }

  public String persistir(FileUpload arquivo) throws IOException {
    if (!arquivo.contentType().equals("application/pdf")) {
      System.out.println(arquivo.contentType());
      throw new RuntimeException("Arquivos aceitos: pdf");
    }
    Files.createDirectories(Path.of(caminhoDestino));

    String uuid = UUID.randomUUID().toString() + ".pdf";

    var destino = Path.of(caminhoDestino, uuid);
    Files.move(arquivo.filePath(), destino);

    return "/atividades/certificados/" + uuid;
  }
}
