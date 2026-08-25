package com.minerva.domain.entities;

import com.minerva.domain.entities.userAction.Auditable;
import com.minerva.domain.exceptions.UnexpectedDomainException;
import com.minerva.domain.valueObject.id.Id;

import java.util.Objects;

public abstract class Entity<I extends Id<?>> implements Auditable {
    private final I id;

    public Entity(I id) {
        if (id == null) throw new UnexpectedDomainException("El ID no puede ser nulo");
        this.id = id;
    }

    @Override
    public String getAuditSubject() {
        return getClass().getSimpleName();
    }

    public I getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> entity = (Entity<?>) o;
        return Objects.equals(getId(), entity.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
/*
public class EntityMapper {

    // Convierte cualquier Entity en un Map donde:
    // - La clave es el nombre del atributo.
    // - El valor es el valor del atributo.
    //
    // Ejemplo:
    // Customer {
    //     id = CustomerId("123"),
    //     customerName = CustomerName("John")
    // }
    //
    // Se convierte en:
    // {
    //     "id": "123",
    //     "customerName": "John"
    // }
    public static Map<String, Object> toMap(Entity<?> entity) {

        // Creamos el Map que almacenará los atributos de la entidad.
        //
        // LinkedHashMap mantiene el orden en el que agregamos los atributos.
        Map<String, Object> result = new LinkedHashMap<>();

        // Obtenemos la clase REAL del objeto recibido.
        //
        // Si entity contiene un Customer:
        //     entity.getClass() → Customer.class
        //
        // Si contiene un Product:
        //     entity.getClass() → Product.class
        Class<?> clazz = entity.getClass();

        // Recorremos la jerarquía de clases de la entidad.
        //
        // Por ejemplo:
        //
        // Customer
        //    ↓
        // Entity
        //    ↓
        // Object
        //
        // Esto es necesario porque algunos atributos pueden estar
        // declarados en la clase padre (por ejemplo, "id" está en Entity).
        while (clazz != null) {

            // Obtenemos todos los atributos DECLARADOS directamente
            // en la clase actual.
            //
            // Si clazz == Customer.class:
            //     obtiene los campos declarados en Customer.
            //
            // Si clazz == Entity.class:
            //     obtiene los campos declarados en Entity.
            //
            // getDeclaredFields() también obtiene campos private.
            for (Field field : clazz.getDeclaredFields()) {

                // Permite acceder mediante Reflection a campos privados,
                // protected, etc.
                //
                // Por ejemplo, aunque tengamos:
                //
                // private CustomerName customerName;
                //
                // podremos leer su valor mediante field.get(entity).
                field.setAccessible(true);

                try {

                    // Obtiene el valor que tiene este atributo
                    // dentro de la instancia concreta "entity".
                    //
                    // Por ejemplo, si field representa:
                    //
                    // private CustomerName customerName;
                    //
                    // y entity es:
                    //
                    // Customer("John")
                    //
                    // entonces value será:
                    //
                    // CustomerName("John")
                    Object value = field.get(entity);

                    // Agregamos el atributo al Map.
                    //
                    // field.getName() devuelve el nombre del atributo.
                    //
                    // Ejemplo:
                    //
                    // field.getName() → "customerName"
                    //
                    // unwrap(value) convierte un ValueObject:
                    //
                    // CustomerName("John")
                    //        ↓
                    //      "John"
                    //
                    // Resultado:
                    //
                    // "customerName" → "John"
                    result.put(
                            field.getName(),
                            unwrap(value)
                    );

                    // field.get() puede lanzar IllegalAccessException
                    // si Java no permite acceder al campo mediante Reflection.
                } catch (IllegalAccessException e) {

                    // Convertimos la excepción checked en una
                    // RuntimeException.
                    throw new RuntimeException(e);
                }
            }

            // Pasamos a la clase padre.
            //
            // Por ejemplo:
            //
            // Primera iteración:
            //     Customer.class
            //
            // Después:
            //     Entity.class
            //
            // Después:
            //     Object.class
            //
            // Finalmente:
            //     null
            //
            // Cuando sea null, el while termina.
            clazz = clazz.getSuperclass();
        }

        // Devolvemos el Map con todos los atributos encontrados.
        return result;
    }

    // Convierte los Value Objects en sus valores internos.
    //
    // Ejemplo:
    //
    // CustomerName("John")
    //       ↓
    // "John"
    //
    // PhoneNumber("999999999")
    //       ↓
    // "999999999"
    private static Object unwrap(Object value) {

        // Comprobamos si el objeto es una instancia de ValueObject.
        //
        // Si lo es, Java además nos permite obtener directamente
        // la referencia como ValueObject<?> mediante "valueObject".
        if (value instanceof ValueObject<?> valueObject) {

            // Extraemos el valor interno del ValueObject.
            //
            // Si tenemos:
            //
            // CustomerName.value = "John"
            //
            // devolvemos:
            //
            // "John"
            return valueObject.value;
        }

        // Si no es un ValueObject, devolvemos el objeto tal cual.
        //
        // Ejemplos:
        //
        // Integer → se devuelve el Integer
        // Boolean → se devuelve el Boolean
        // BigDecimal → se devuelve el BigDecimal
        // String → se devuelve el String
        // LocalDateTime → se devuelve el LocalDateTime
        return value;
    }
}

*/