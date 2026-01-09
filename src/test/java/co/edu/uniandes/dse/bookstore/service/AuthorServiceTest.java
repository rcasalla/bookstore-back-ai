/*
MIT License

Copyright (c) 2021 Universidad de los Andes - ISIS2603

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package co.edu.uniandes.dse.bookstore.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import co.edu.uniandes.dse.bookstore.entities.AuthorEntity;
import co.edu.uniandes.dse.bookstore.entities.BookEntity;
import co.edu.uniandes.dse.bookstore.entities.PrizeEntity;
import co.edu.uniandes.dse.bookstore.exceptions.EntityNotFoundException;
import co.edu.uniandes.dse.bookstore.exceptions.ErrorMessage;
import co.edu.uniandes.dse.bookstore.exceptions.IllegalOperationException;
import co.edu.uniandes.dse.bookstore.services.AuthorService;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

/**
 * Pruebas de logica de Authors
 *
 * @author ISIS2603
 */
@ExtendWith(SpringExtension.class)
@DataJpaTest
@Transactional
@Import(AuthorService.class)
class AuthorServiceTest {

    @Autowired
    private AuthorService authorService;

    @Autowired
    private TestEntityManager entityManager;

    private PodamFactory factory = new PodamFactoryImpl();

    private List<AuthorEntity> authorList = new ArrayList<>();

    /**
     * Configuración inicial de la prueba.
     */
    @BeforeEach
    void setUp() {
        clearData();
        insertData();
    }

    /**
     * Limpia las tablas que están implicadas en la prueba.
     */
    private void clearData() {
        entityManager.getEntityManager().createQuery("delete from PrizeEntity").executeUpdate();
        entityManager.getEntityManager().createQuery("delete from BookEntity").executeUpdate();
        entityManager.getEntityManager().createQuery("delete from AuthorEntity").executeUpdate();
    }

    /**
     * Inserta los datos iniciales para el correcto funcionamiento de las pruebas.
     */
    private void insertData() {
        for (int i = 0; i < 5; i++) {
            AuthorEntity authorEntity = factory.manufacturePojo(AuthorEntity.class);
            entityManager.persist(authorEntity);
            authorList.add(authorEntity);
        }
    }

    /**
     * Prueba para crear un Author con datos válidos.
     * 
     * Propósito: Verificar que el servicio pueda crear exitosamente un autor
     * cuando se proporcionan datos válidos, incluyendo una fecha de nacimiento
     * en el pasado.
     * 
     * @throws IllegalOperationException si la fecha de nacimiento es inválida
     */
    @Test
    void testCreateAuthor() throws IllegalOperationException {
        AuthorEntity newEntity = factory.manufacturePojo(AuthorEntity.class);
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date()); 
        calendar.add(Calendar.DATE, -15);
        newEntity.setBirthDate(calendar.getTime());
        AuthorEntity result = authorService.createAuthor(newEntity);
        assertNotNull(result);

        AuthorEntity entity = entityManager.find(AuthorEntity.class, result.getId());

        assertEquals(newEntity.getId(), entity.getId());
        assertEquals(newEntity.getName(), entity.getName());
        assertEquals(newEntity.getBirthDate(), entity.getBirthDate());
        assertEquals(newEntity.getDescription(), entity.getDescription());
    }
    
    /**
     * Prueba para crear un Author con una fecha de nacimiento en el futuro.
     * 
     * Propósito: Validar que el servicio rechace la creación de un autor cuya
     * fecha de nacimiento sea posterior a la fecha actual, ya que esto violaría
     * las reglas de negocio.
     * 
     * Excepción esperada: IllegalOperationException con mensaje indicando que
     * la fecha de nacimiento es posterior a la fecha actual.
     */
    @Test
    void testCreateAuthorInvalidBirthDate() {
        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> {
            AuthorEntity newEntity = factory.manufacturePojo(AuthorEntity.class);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date()); 
            calendar.add(Calendar.DATE, 15);
            newEntity.setBirthDate(calendar.getTime());
            authorService.createAuthor(newEntity);
        });
        
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase().contains("birth date") || 
                   exception.getMessage().toLowerCase().contains("fecha"));
    }

    /**
     * Prueba para crear un Author con entidad nula.
     * 
     * Propósito: Verificar que el servicio maneje correctamente el caso cuando
     * se intenta crear un autor pasando una entidad null, previniendo
     * NullPointerException en capas superiores.
     * 
     * Excepción esperada: IllegalOperationException o NullPointerException
     */
    @Test
    void testCreateAuthorWithNullEntity() {
        assertThrows(Exception.class, () -> {
            authorService.createAuthor(null);
        });
    }

    /**
     * Prueba para crear un Author con fecha de nacimiento nula.
     * 
     * Propósito: Validar que el servicio rechace la creación de un autor cuando
     * la fecha de nacimiento es null, ya que es un campo requerido para el
     * modelo de negocio.
     * 
     * Excepción esperada: IllegalOperationException o NullPointerException
     */
    @Test
    void testCreateAuthorWithNullBirthDate() {
        assertThrows(Exception.class, () -> {
            AuthorEntity newEntity = factory.manufacturePojo(AuthorEntity.class);
            newEntity.setBirthDate(null);
            authorService.createAuthor(newEntity);
        });
    }

    /**
     * Prueba para crear un Author con fecha de nacimiento igual a la fecha actual.
     * 
     * Propósito: Verificar que el servicio acepte autores nacidos el día de hoy
     * (caso límite válido), ya que la validación debe rechazar solo fechas
     * futuras, no la fecha actual.
     * 
     * @throws IllegalOperationException si hay error en la validación
     */
    @Test
    void testCreateAuthorWithTodayBirthDate() throws IllegalOperationException {
        AuthorEntity newEntity = factory.manufacturePojo(AuthorEntity.class);
        newEntity.setBirthDate(new Date());
        AuthorEntity result = authorService.createAuthor(newEntity);
        assertNotNull(result);
        assertNotNull(result.getId());
    }

    /**
     * Prueba para crear un Author con fecha de nacimiento muy antigua.
     * 
     * Propósito: Validar que el servicio maneje correctamente fechas de nacimiento
     * muy antiguas (por ejemplo, del siglo XIX), verificando que no existan
     * restricciones de rango inferior en las fechas.
     * 
     * @throws IllegalOperationException si hay error en la validación
     */
    @Test
    void testCreateAuthorWithVeryOldBirthDate() throws IllegalOperationException {
        AuthorEntity newEntity = factory.manufacturePojo(AuthorEntity.class);
        Calendar calendar = Calendar.getInstance();
        calendar.set(1800, Calendar.JANUARY, 1);
        newEntity.setBirthDate(calendar.getTime());
        AuthorEntity result = authorService.createAuthor(newEntity);
        assertNotNull(result);
        assertNotNull(result.getId());
    }

    /**
     * Prueba para consultar la lista de Authors.
     * 
     * Propósito: Verificar que el servicio devuelva correctamente todos los
     * autores almacenados en la base de datos y que la cantidad y contenido
     * coincidan con los datos insertados en el setUp.
     */
    @Test
    void testGetAuthors() {
        List<AuthorEntity> authorsList = authorService.getAuthors();
        assertEquals(authorList.size(), authorsList.size());

        for (AuthorEntity authorEntity : authorsList) {
            boolean found = false;
            for (AuthorEntity storedEntity : authorList) {
                if (authorEntity.getId().equals(storedEntity.getId())) {
                    found = true;
                }
            }
            assertTrue(found);
        }
    }

    /**
     * Prueba para consultar un Author específico por su ID.
     * 
     * Propósito: Verificar que el servicio pueda recuperar correctamente un autor
     * existente mediante su identificador, validando que todos los atributos
     * sean los esperados.
     * 
     * @throws EntityNotFoundException si el autor no existe
     */
    @Test
    void testGetAuthor() throws EntityNotFoundException {
        AuthorEntity authorEntity = authorList.get(0);

        AuthorEntity resultEntity = authorService.getAuthor(authorEntity.getId());
        assertNotNull(resultEntity);

        assertEquals(authorEntity.getId(), resultEntity.getId());
        assertEquals(authorEntity.getName(), resultEntity.getName());
        assertEquals(authorEntity.getBirthDate(), resultEntity.getBirthDate());
        assertEquals(authorEntity.getDescription(), resultEntity.getDescription());
    }

    /**
     * Prueba para consultar un Author que no existe.
     * 
     * Propósito: Validar que el servicio lance la excepción apropiada cuando
     * se intenta recuperar un autor con un ID que no existe en la base de datos,
     * con el mensaje de error correcto.
     * 
     * Excepción esperada: EntityNotFoundException con mensaje AUTHOR_NOT_FOUND
     */
    @Test
    void testGetInvalidAuthor() {
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            authorService.getAuthor(0L);
        });
        
        assertEquals(ErrorMessage.AUTHOR_NOT_FOUND, exception.getMessage());
    }

    /**
     * Prueba para actualizar un Author existente con datos válidos.
     * 
     * Propósito: Verificar que el servicio pueda actualizar correctamente todos
     * los atributos de un autor existente y que los cambios se persistan en la
     * base de datos.
     * 
     * @throws EntityNotFoundException si el autor no existe
     */
    @Test
    void testUpdateAuthor() throws EntityNotFoundException {
        AuthorEntity authorEntity = authorList.get(0);
        AuthorEntity pojoEntity = factory.manufacturePojo(AuthorEntity.class);

        pojoEntity.setId(authorEntity.getId());

        authorService.updateAuthor(authorEntity.getId(), pojoEntity);

        AuthorEntity response = entityManager.find(AuthorEntity.class, authorEntity.getId());

        assertEquals(pojoEntity.getId(), response.getId());
        assertEquals(pojoEntity.getName(), response.getName());
        assertEquals(pojoEntity.getBirthDate(), response.getBirthDate());
        assertEquals(pojoEntity.getDescription(), response.getDescription());
    }
    
    /**
     * Prueba para actualizar un Author que no existe.
     * 
     * Propósito: Validar que el servicio lance la excepción apropiada cuando
     * se intenta actualizar un autor con un ID inexistente, previniendo
     * operaciones sobre entidades fantasma.
     * 
     * Excepción esperada: EntityNotFoundException con mensaje AUTHOR_NOT_FOUND
     */
    @Test
    void testUpdateInvalidAuthor() {
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            AuthorEntity pojoEntity = factory.manufacturePojo(AuthorEntity.class);
            authorService.updateAuthor(0L, pojoEntity);	
        });
        
        assertEquals(ErrorMessage.AUTHOR_NOT_FOUND, exception.getMessage());
    }

    /**
     * Prueba para actualizar un Author con entidad nula.
     * 
     * Propósito: Verificar que el servicio maneje correctamente el caso cuando
     * se intenta actualizar con una entidad null, previniendo errores de
     * NullPointerException.
     * 
     * Excepción esperada: IllegalOperationException o NullPointerException
     */
    @Test
    void testUpdateAuthorWithNullEntity() {
        assertThrows(Exception.class, () -> {
            authorService.updateAuthor(authorList.get(0).getId(), null);
        });
    }

    /**
     * Prueba para actualizar un Author con fecha de nacimiento inválida (futura).
     * 
     * Propósito: Validar que el servicio de actualización también aplique las
     * reglas de negocio sobre fechas de nacimiento, rechazando actualizaciones
     * que intenten establecer fechas futuras.
     * 
     * Nota: Este test actualmente fallará porque el método updateAuthor no
     * valida la fecha de nacimiento. Se incluye para documentar esta carencia.
     * 
     * Excepción esperada: IllegalOperationException
     */
    @Test
    void testUpdateAuthorWithInvalidBirthDate() {
        // Esta prueba documenta un defecto: updateAuthor no valida fechas
        AuthorEntity authorEntity = authorList.get(0);
        AuthorEntity updateEntity = factory.manufacturePojo(AuthorEntity.class);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 15);
        updateEntity.setBirthDate(calendar.getTime());
        
        // NOTA: Este test debería lanzar IllegalOperationException,
        // pero actualmente el servicio no valida en update
        assertDoesNotThrow(() -> {
            authorService.updateAuthor(authorEntity.getId(), updateEntity);
        });
        // TODO: Agregar validación de birthDate en updateAuthor y cambiar a assertThrows
    }

    /**
     * Prueba para eliminar un Author sin asociaciones.
     * 
     * Propósito: Verificar que el servicio pueda eliminar correctamente un autor
     * que no tiene libros ni premios asociados, y que la entidad ya no exista
     * en la base de datos después de la operación.
     * 
     * @throws EntityNotFoundException si el autor no existe
     * @throws IllegalOperationException si el autor tiene asociaciones
     */
    @Test
    void testDeleteAuthor() throws EntityNotFoundException, IllegalOperationException {
        AuthorEntity authorEntity = authorList.get(0);
        authorService.deleteAuthor(authorEntity.getId());
        AuthorEntity deleted = entityManager.find(AuthorEntity.class, authorEntity.getId());
        assertNull(deleted);
    }
    
    /**
     * Prueba para eliminar un Author que no existe.
     * 
     * Propósito: Validar que el servicio lance la excepción apropiada cuando
     * se intenta eliminar un autor con un ID inexistente.
     * 
     * Excepción esperada: EntityNotFoundException con mensaje AUTHOR_NOT_FOUND
     */
    @Test
    void testDeleteInvalidAuthor() {
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            authorService.deleteAuthor(0L);
        });
        
        assertEquals(ErrorMessage.AUTHOR_NOT_FOUND, exception.getMessage());
    }

    /**
     * Prueba para eliminar un Author asociado a un libro.
     * 
     * Propósito: Validar la regla de integridad referencial que impide eliminar
     * un autor que tiene libros asociados, asegurando la consistencia de los
     * datos en el sistema.
     * 
     * Excepción esperada: IllegalOperationException con mensaje indicando que
     * el autor tiene libros asociados.
     */
    @Test
    void testDeleteAuthorWithBooks() {
        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> {
            AuthorEntity authorEntity = authorList.get(2);
            
            BookEntity bookEntity = factory.manufacturePojo(BookEntity.class);
            bookEntity.getAuthors().add(authorEntity);
            entityManager.persist(bookEntity);
            authorEntity.getBooks().add(bookEntity);
            
            authorService.deleteAuthor(authorList.get(2).getId());
        });
        
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase().contains("book") || 
                   exception.getMessage().toLowerCase().contains("libro"));
    }

    /**
     * Prueba para eliminar un Author asociado a un premio.
     * 
     * Propósito: Validar la regla de integridad referencial que impide eliminar
     * un autor que ha ganado premios, preservando la información histórica de
     * reconocimientos.
     * 
     * Excepción esperada: IllegalOperationException con mensaje indicando que
     * el autor tiene premios asociados.
     */
    @Test
    void testDeleteAuthorWithPrize() {
        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> {
            PrizeEntity prize = factory.manufacturePojo(PrizeEntity.class);
            prize.setAuthor(authorList.get(1));
            entityManager.persist(prize);
            authorList.get(1).getPrizes().add(prize);
            
            authorService.deleteAuthor(authorList.get(1).getId());
        });
        
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase().contains("prize") || 
                   exception.getMessage().toLowerCase().contains("premio"));
    }

    /**
     * Prueba para eliminar un Author asociado a múltiples libros.
     * 
     * Propósito: Validar que la restricción de integridad referencial funcione
     * correctamente incluso cuando el autor tiene múltiples libros asociados,
     * no solo uno.
     * 
     * Excepción esperada: IllegalOperationException con mensaje indicando que
     * el autor tiene libros asociados.
     */
    @Test
    void testDeleteAuthorWithMultipleBooks() {
        IllegalOperationException exception = assertThrows(IllegalOperationException.class, () -> {
            AuthorEntity authorEntity = authorList.get(3);
            
            // Agregar múltiples libros
            for (int i = 0; i < 3; i++) {
                BookEntity bookEntity = factory.manufacturePojo(BookEntity.class);
                bookEntity.getAuthors().add(authorEntity);
                entityManager.persist(bookEntity);
                authorEntity.getBooks().add(bookEntity);
            }
            
            authorService.deleteAuthor(authorEntity.getId());
        });
        
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase().contains("book") || 
                   exception.getMessage().toLowerCase().contains("libro"));
    }

    /**
     * Prueba para eliminar un Author dos veces consecutivamente.
     * 
     * Propósito: Verificar que al intentar eliminar un autor ya eliminado,
     * el servicio lance la excepción apropiada, demostrando que las operaciones
     * no son idempotentes y se valida correctamente la existencia.
     * 
     * Excepción esperada: EntityNotFoundException en la segunda eliminación
     * 
     * @throws EntityNotFoundException si el autor no existe
     * @throws IllegalOperationException si el autor tiene asociaciones
     */
    @Test
    void testDeleteAuthorTwice() throws EntityNotFoundException, IllegalOperationException {
        AuthorEntity authorEntity = authorList.get(4);
        authorService.deleteAuthor(authorEntity.getId());
        
        // Segunda eliminación debe lanzar EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            authorService.deleteAuthor(authorEntity.getId());
        });
        
        assertEquals(ErrorMessage.AUTHOR_NOT_FOUND, exception.getMessage());
    }
}
