package br.edu.ifrs.poa.model.atividades;

import org.wildfly.security.authz.SimpleAttributesEntry;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.persistence.Embeddable;

@Embeddable
public class Aluno {
  public String uid, nome;

  public Aluno() {
  }

  public Aluno(String uid, String nome) {
    this.uid = uid;
    this.nome = nome;
  }

  public Aluno(SecurityIdentity identity) {
    var uid = identity.getPrincipal().getName();
    var name = ((SimpleAttributesEntry) identity.getAttribute("displayName")).getFirst();
    this(uid, name);
  }
}
