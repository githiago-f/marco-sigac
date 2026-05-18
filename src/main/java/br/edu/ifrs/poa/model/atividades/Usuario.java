package br.edu.ifrs.poa.model.atividades;

import org.wildfly.security.authz.SimpleAttributesEntry;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Usuario {
  public String uid;
  @Column(name = "nome_aluno")
  public String nome;

  public Usuario() {
  }

  public Usuario(String uid, String nome) {
    this.uid = uid;
    this.nome = nome;
  }

  public Usuario(SecurityIdentity identity) {
    var uid = identity.getPrincipal().getName();
    var name = ((SimpleAttributesEntry) identity.getAttribute("displayName")).getFirst();
    this(uid, name);
  }
}
