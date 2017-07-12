package ru.pflb.httpserver.core;

import java.util.HashMap;

/**
 * Класс с константами для сервера.
 */
public abstract class HTTPConstants {

    /**
     * Просто парсит метод...
     * @param method Строка с методом.
     * @return Enum значение метода.
     */
    public static METHOD parseMethod(String method) {
        return METHOD.valueOf(method);
    }

    /**
     * Проверяет, есть ли достаточное количество уровней в URL структуре.
     *
     * @param urls       Массив уровней URL запроса.
     * @param level      Текущий уровень.
     * @param needLevels Сколько уровней еще нужно.
     * @return {@code true}, если в URL достаточно уровней, иначе {@code false}.
     */
    public static boolean checkUrlLength(String[] urls, int level, int needLevels) {
        return urls.length > level + needLevels;
    }

    /**
     * Получить значение параметра.
     *
     * @param params       Параметры.
     * @param param        Имя параметра.
     * @param defaultValue Значение по умолчанию (если значение параметра = null).
     * @return Значение параметра.
     */
    public static String getValue(HashMap<String, String> params, String param, String defaultValue) {
        String res = params.get(param);
        if (res == null) {
            return defaultValue;
        }
        return res;
    }

    public static Integer getValue(HashMap<String, String> params, String param, Integer defaultValue) {
        String res = getValue(params, param, (String) null);
        if (res == null) {
            return defaultValue;
        } else {
            return Integer.parseInt(res);
        }
    }

    public static Boolean getValue(HashMap<String, String> params, String param, Boolean defaultValue) {
        String res = getValue(params, param, (String) null);
        if (res == null) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(res);
        }
    }

    /**
     * Проверяет, есть ли указанный уровень в URL структуре.
     *
     * @param urls       Массив уровней URL запроса.
     * @param level      Текущий уровень.
     * @return {@code true}, если в URL достаточно уровней, иначе {@code false}.
     */
    public static boolean checkUrlLength(String[] urls, int level) {
        return urls.length > level;
    }

    /**
     * Список возвращаемых кодов.
     */
    public enum CODE {
        C200("OK"),
        C400("Bad Request"),
        C404("Not Found"),
        C500("Internal Server Error"),
        C503("Service Unavailable");

        private final String desc;

        CODE(String msg) {
            desc = msg;
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    /**
     * Список методов
     */
    public enum METHOD {
        GET, POST, PUT, DELETE, SEARCH
    }
}
