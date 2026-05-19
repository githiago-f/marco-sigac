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
  public String email;

  public Usuario() {
  }

  public Usuario(String uid, String nome, String email) {
    this.uid = uid;
    this.nome = nome;
    this.email = email;
  }

  public Usuario(SecurityIdentity identity) {
    var uid = identity.getPrincipal().getName();
    var name = ((SimpleAttributesEntry) identity.getAttribute("displayName")).getFirst();
    var email = ((SimpleAttributesEntry) identity.getAttribute("email")).getFirst();
    this(uid, name, email);
  }

  @Override
  public String toString() {
    return "Usuario [uid=" + uid + ", nome=" + nome + ", email=" + email + "]";
  }

}
