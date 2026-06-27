package br.edu.ifrs.poa.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.security.authz.SimpleAttributesEntry;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RolesAugmentor implements SecurityIdentityAugmentor {
  private final Logger logger = LoggerFactory.getLogger(RolesAugmentor.class);

  @Override
  public int priority() {
    return 0;
  }

  @Override
  public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
    if (identity.isAnonymous()) {
      return Uni.createFrom().item(identity);
    }

    logger.debug("Principal={}", identity.getPrincipal().getName());
    logger.debug("Roles atuais={}", identity.getRoles());
    logger.debug("Attributes={}", identity.getAttributes());

    QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

    SimpleAttributesEntry dn = (SimpleAttributesEntry) identity.getAttribute("dn");

    if (dn != null) {
      var dnValue = dn.getFirst();
      logger.info("DN={}", dnValue);

      if (dnValue.contains("OU=Discente")) {
        builder.addRole("aluno");
      }

      if (dnValue.contains("OU=Docente")) {
        builder.addRole("professor");
      }
    } else {
      logger.warn("DN attribute not found — running without LDAP? Using available roles: {}", identity.getRoles());
    }

    return Uni.createFrom().item(builder.build());
  }
}
