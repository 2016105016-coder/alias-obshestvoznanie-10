package ru.school.aliassociety;

import java.util.ArrayList;
import java.util.List;

public final class GameData {
    private GameData() {}

    public static final String[] TEAM_ADJECTIVES = {
            "Громкие", "Молниеносные", "Железные", "Яркие", "Умные", "Хитрые", "Бодрые", "Непобедимые", "Космические", "Северные",
            "Южные", "Огненные", "Ледяные", "Золотые", "Серебряные", "Неоновые", "Турбо", "Супер", "Мега", "Экстра",
            "Реактивные", "Учёные", "Весёлые", "Серьёзные", "Гениальные", "Правовые", "Рыночные", "Социальные", "Политические", "Культурные"
    };

    public static final String[] TEAM_NOUNS = {
            "Мыслители", "Эксперты", "Знатоки", "Ораторы", "Стратеги", "Юристы", "Экономисты", "Дипломаты", "Граждане", "Аналитики",
            "Реформаторы", "Лидеры", "Профессора", "Дебатёры", "Комментаторы", "Капитаны", "Навигаторы", "Федералы", "Парламентарии", "Философы"
    };

    public static final String[] SOUND_TYPES = {
            "click", "start", "correct", "wrong", "skip", "pause", "resume", "victory", "defeat", "warning", "level", "team", "reset", "modal", "home", "target"
    };

    public static List<String> buildTeamNameOptions() {
        ArrayList<String> result = new ArrayList<>();
        for (String adjective : TEAM_ADJECTIVES) {
            for (String noun : TEAM_NOUNS) {
                result.add(adjective + " " + noun);
            }
        }
        return result;
    }

    public static int soundVariantCount() {
        return SOUND_TYPES.length * 10;
    }
}
