Nota:

- los limites de dinero y cantidad de producto deben validarse en infrestrtutra,

-- cambiar todos los nombres que terminene en obj por el tipo de dato que son sus contraposiciones

-- evitar esar esto en todo el proyecto .compareTo(BigDecimal.ZERO) < 0

-- en los dto recibidos falta agregar logica de validacion, pero por factor tiempo se dejo asi nomas

-- deberia agregar un pepper a la encriptación

-- debria agregar un palabra caracteristirca a cada entidad para su id a la hora de registrarse

-- revisar que no se este usando expetion en todo el proyecto, solo se debe usar domain exepction

-- ver el tema de vulnerabilidades del pom.xml las pendencias

-- falta agregar el refreshToken
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

-- borrar los docs inecesarios

-- que cada clase no confie y valide los nulos y a su vez esto traera consigo un mejor mensaje

-- escanear que no se este usando domain exepciont en el proyecto

-- se deberia agregar una bandera para los productos que son peresibles y asi poder obligar a ingresar un fecha de caducidad

-- debo maximizar el uso de los set en los repository intrerface
example
    void save(Sale sale, Set<Product> products);

-- falta el map de sale

-- una tabla para todas las personas (fullname, dni, etc)

-- una tabla para todas relacionar las distitnas presentaciones de un producto

-- verificar que realemnte se este usando unbterface para los valores (pk sobre todo)

-- usar el nombre de las variables en vez de temp usar Value al final

-- Registrar read an write en usaer action, que operacion en la db seria


------------------------------------------------------------------------------------
Mejorar el nombre de los atributos, por ejemplo en vez de usar quantity usar productQuantity, esto se debe reflejar en la db, aplicar esto a todas las entidades
class SaleDetail extends Entity<SaleDetailId> {
    private final SaleId saleId;
    private final ProductId productId;
    private final ProductQuantity quantity;
    private final Money unitPrice;


-- hacer que result reciba como parametro los expecion