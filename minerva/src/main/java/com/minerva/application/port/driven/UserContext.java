package com.minerva.application.port.driven;

/**
 * Datos mínimos del usuario autenticado necesarios para ejecutar los casos de uso.
 *
 * Se mantiene en la capa de aplicación para que los servicios no dependan
 * directamente de clases de Spring Security. La infraestructura obtiene estos
 * valores desde el SecurityContext y los entrega mediante CurrentUserProvider.
 */
public record UserContext(
        String userId,
        String role
) {
}
