
package ru.cinema.cinema.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.cinema.cinema.entity.Session;

public interface SessionRepository extends JpaRepository<Session, Long> {
}
