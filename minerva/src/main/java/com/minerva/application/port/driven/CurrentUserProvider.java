package com.minerva.application.port.driven;

/**
 * Puerto de salida utilizado por la aplicación para conocer al usuario que
 * ejecuta la petición actual sin acoplarse a Spring Security.
 *
 * La implementación concreta está en SpringSecurityCurrentUserAdapter, que lee
 * la autenticación almacenada en el SecurityContext de la petición.
 */
public interface CurrentUserProvider {

    UserContext currentUser();
}
