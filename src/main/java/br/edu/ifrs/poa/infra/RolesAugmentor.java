package br.edu.ifrs.poa.infra;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RolesAugmentor implements SecurityIdentityAugmentor {

  @Override
  public Uni<SecurityIdentity> augment(SecurityIdentity identity,
      AuthenticationRequestContext context) {

    QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

    String dn = identity.getPrincipal().getName();

    if (dn.contains("OU=Discente")) {
      builder.addRole("aluno");
    }

    if (dn.contains("OU=Docente")) {
      builder.addRole("professor");
    }

    return Uni.createFrom().item(builder.build());
  }
}
