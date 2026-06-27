package br.edu.ifrs.poa.model.atividades;

import java.util.Objects;

import org.wildfly.security.authz.SimpleAttributesEntry;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {
  @Id
  public String uid;
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
    this.uid = uid;
    this.nome = name;
    this.email = email;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Usuario usuario = (Usuario) o;
    return Objects.equals(uid, usuario.uid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uid);
  }

  @Override
  public String toString() {
    return "Usuario [uid=" + uid + ", nome=" + nome + ", email=" + email + "]";
  }
}
