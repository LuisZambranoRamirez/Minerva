Nota:

- los limites de dinero y cantidad de producto deben validarse en infrestrtutra,


- revisar que solo se use BigDecimal en las value keys
-- cambiar todos los nombres que terminene en obj por el tipo de dato que son sus contraposiciones

-- evitar esar esto en todo el proyecto .compareTo(BigDecimal.ZERO) < 0

-- invertir esta logica             this.phoneNumber = (newPhoneNumber != null)
                ? new PhoneNumber(newPhoneNumber)
                : null;

-- ⚠️ Otra mejora

Aquí:

@Column(name = "ruc", columnDefinition = "CHAR(11)", unique = true)

columnDefinition te acopla al SQL del motor.

Más portable:

@Column(name = "ruc", length = 11, unique = true)

igual para:

phoneNumber
barCode




-- en los dto recibidos falta agregar logica de validacion, pero por factor tiempo se dejo asi nomas

-- deberia agregar un pepper a la encriptación

-- debria agregar un palabra caracteristirca a cada entidad para su id a la hora de registrarse

-- recordar: desaparecer la carpeta id al final, simplemente es para no marearme xd

-- revisar que no se este usando expetion en todo el proyecto, solo se debe usar domain exepction

-- Agregar un campo de estado activo en user
-- ver el tema de vulnerabilidades del pom.xml las pendencias

-- falta agregar el refreshToken

-- El AllId es solo para registrar el caso donde se hacen consultas a muchas entidades

ojo revisar con chatcito
--  Corregir la sitnexis en ingels                  exists by bar code <-- esto es corrrecto
                                                    exist by bar code <-- esto es incorrrecto

para que pinses como solucionarlo: 
product_name UNIQUE puede darte problemas

Tienes:

product_name VARCHAR(100) NOT NULL UNIQUE

Ejemplo:

Coca Cola 500ml
Coca-Cola 500 ML
Coca Cola Botella 500 ml

Son productos iguales pero PostgreSQL los acepta como diferentes.

Normalmente:

product_name VARCHAR(100) NOT NULL

sin UNIQUE.


-- cambiar el tamano de la contrsean en el sql para que se adapte a lo generado

-- pensar un mejor nombre para productimpl, la palabra impl lo hace ver feo xd

-- pensar si es necesario que una entidad deberia conocer si un objeto el id de otro dominio o deberia conocerlo por atributo

-- se deberia pasar el uuid como uuid y no hacer la transformacion a string.

-- borrar los docs inecesarios

-- que cada clase no confie y valide los nulos y a su vez esto traera consigo un mejor mensaje

-- escanear que no se este usando domain exepciont en el proyecto

-- se deberia agregar una bandera para los productos que son peresibles y asi poder obligar a ingresar un fecha de caducidad

-- revisar los dto de pay, porque creo que deberia haber una de lectura y otro de escritura, una recibe datos crudos (para la creacion) y otro lo valueobjects para su lectura

-- debo maximizar el uso de los set en los repository intrerface
example
    void save(Sale sale, Set<Product> products);

-- falta el map de sale
