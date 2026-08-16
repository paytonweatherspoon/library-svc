package com.weatherspoon.library;

import com.weatherspoon.library.entity.Book;
import com.weatherspoon.library.entity.User;
import com.weatherspoon.library.repository.BookRepository;
import com.weatherspoon.library.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Seeds the H2 database with sample books and users on startup,
 * but only when `seed.data=true` is set. Intended for local dev/demo use.
 */
@Component
@ConditionalOnProperty(name = "seed.data", havingValue = "true")
@AllArgsConstructor
public class DataSeed implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    /**
     * Seeds sample books and users on application startup. No-op if either table is
     * already populated, so it's safe to leave {@code seed.data=true} on across restarts.
     *
     * @param args unused, required by {@link CommandLineRunner}
     */
    @Override
    public void run(String... args) {

        if (bookRepository.count() > 0 || userRepository.count() > 0) {
            return;
        }

        Book cleanCode = new Book("Clean Code", "Robert Martin","978-0135398579",
                LocalDate.of(2008, 8, 1));
        Book ingram = new Book("Golf Is Not a Game of Perfect", "Dr. Bob Rotella","978-0743492478",
                LocalDate.of(1995, 5, 9));
        Book hired = new Book("60 Seconds and You're Hired", "Robin Ryan","978-0143128502",
                LocalDate.of(2016, 1, 5));
        Book dataIntensiveApplications = new Book("Data-Intensive Applications", "Martin Kleppmann",
                "978-1098119065", LocalDate.of(2026, 3, 24));

        User payton = new User("payton", "weatherspoon", "paytonweatherspoon@gmail.com");
        User leoM = new User("leo", "messi", "leomessi@gmail.com");

        List<Book> books = List.of(cleanCode, ingram, hired, dataIntensiveApplications);
        List<User> users = List.of(payton, leoM);

        userRepository.saveAll(users);
        bookRepository.saveAll(books);
    }
}
