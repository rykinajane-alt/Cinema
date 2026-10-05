
package ru.cinema.cinema.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.cinema.cinema.dto.DailySessionStat;
import ru.cinema.cinema.entity.Session;
import ru.cinema.cinema.service.SessionService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    // Список сеансов, поиск и сортировка
    @GetMapping
    public String getAllSessions(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model) {

        // Получаем все сеансы из базы данных
        List<Session> allSessions = sessionService.getAllSessions();

        // Отдельный список для поиска и сортировки
        List<Session> sessions = allSessions;

        // Поиск по названию фильма и киностудии
        if (keyword != null && !keyword.isBlank()) {
            String search = keyword.trim().toLowerCase(Locale.ROOT);

            sessions = sessions.stream()
                    .filter(cinemaSession ->
                            (cinemaSession.getFilmTitle() != null &&
                                    cinemaSession.getFilmTitle()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(search))
                                    ||
                                    (cinemaSession.getFilmStudio() != null &&
                                            cinemaSession.getFilmStudio()
                                                    .toLowerCase(Locale.ROOT)
                                                    .contains(search)))
                    .toList();
        }

        // Сортировка по дате и времени, затем по названию
        Comparator<Session> comparator =
                Comparator.comparing(
                                Session::getSessionDateTime,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(
                                Session::getFilmTitle,
                                Comparator.nullsLast(
                                        String.CASE_INSENSITIVE_ORDER));

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        sessions = sessions.stream()
                .sorted(comparator)
                .toList();

        model.addAttribute("sessions", sessions);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortDir", sortDir);

        // Общее количество записей во всей таблице
        model.addAttribute("recordCount", allSessions.size());

        return "sessions";
    }

    // Статистика за последние 30 дней
    @GetMapping("/statistics")
    public String getStatistics(Model model) {

        List<Session> sessions = sessionService.getAllSessions();

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);

        // Подсчёт сеансов по датам
        Map<LocalDate, Long> countsByDate = sessions.stream()
                .filter(s -> s.getSessionDateTime() != null)
                .filter(s -> {
                    LocalDate date = s.getSessionDateTime().toLocalDate();
                    return !date.isBefore(startDate)
                            && !date.isAfter(today);
                })
                .collect(Collectors.groupingBy(
                        s -> s.getSessionDateTime().toLocalDate(),
                        Collectors.counting()
                ));

        // Максимальное количество сеансов за один день
        long maxCount = countsByDate.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd.MM");

        // Формируем список из 30 дней, включая дни без сеансов
        List<DailySessionStat> dailyStats =
                java.util.stream.IntStream.range(0, 30)
                        .mapToObj(i -> {
                            LocalDate date = startDate.plusDays(i);

                            long count = countsByDate.getOrDefault(
                                    date, 0L);

                            // Высота столбца в процентах
                            int percent = maxCount == 0
                                    ? 0
                                    : (int) Math.round(
                                    count * 100.0 / maxCount);

                            return new DailySessionStat(
                                    date.format(formatter),
                                    count,
                                    percent
                            );
                        })
                        .toList();

        // Общее количество сеансов за период
        long totalInPeriod = countsByDate.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        model.addAttribute("dailyStats", dailyStats);
        model.addAttribute("totalInPeriod", totalInPeriod);
        model.addAttribute("startDate", startDate);
        model.addAttribute("today", today);

        return "statistics";
    }

    // Форма добавления сеанса
    @GetMapping("/new")
    public String newSessionForm(Model model) {
        model.addAttribute("cinemaSession", new Session());
        model.addAttribute("pageTitle", "Добавление сеанса");

        return "session-form";
    }

    // Форма редактирования сеанса
    @GetMapping("/edit/{id}")
    public String editSessionForm(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "cinemaSession",
                sessionService.getSessionById(id)
        );

        model.addAttribute(
                "pageTitle",
                "Редактирование сеанса"
        );

        return "session-form";
    }

    // Сохранение сеанса
    @PostMapping("/save")
    public String saveSession(
            @Valid @ModelAttribute("cinemaSession") Session cinemaSession,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "pageTitle",
                    cinemaSession.getId() == null
                            ? "Добавление сеанса"
                            : "Редактирование сеанса"
            );

            return "session-form";
        }

        sessionService.saveSession(cinemaSession);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Сеанс успешно сохранён"
        );

        return "redirect:/sessions";
    }

    // Удаление сеанса
    @PostMapping("/delete/{id}")
    public String deleteSession(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        sessionService.deleteSession(id);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Сеанс успешно удалён"
        );

        return "redirect:/sessions";
    }
}
