package ru.pflb.httpserver.modules;

import ru.pflb.httpserver.core.HTTPConstants;
import ru.pflb.httpserver.core.HTTPModule;
import ru.pflb.httpserver.utils.Table;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static ru.pflb.httpserver.core.HTTPConstants.CODE.C400;

/**
 * Модуль виртуальных таблиц.
 */
public class VTSModule extends HTTPModule {
    private final ArrayList<Table.TableParams> mTableParams = new ArrayList<>(); //параметры таблиц
    private final HashMap<String, Table> mTables = new HashMap<>(); //Таблицы

    /**
     * Парсит параметры запуска.
     *
     * @param arg    Текущий аргумент.
     * @param args   Список всех аргументов.
     * @param curPos Текущий индекс аргумента.
     * @return Количество распарсеных параметров.
     */
    @Override
    public int parseArgs(String arg, String[] args, int curPos) {
        if (arg.equalsIgnoreCase(ArgCommands.MODULE_CMD.toString())) {
            Table.TableParams params = new Table.TableParams();
            mTableParams.add(params);
            return parseParams(params, args, curPos + 1) + 1;
        }
        return 0;
    }

    /**
     * Разбор параметров.
     *
     * @param params Параметры таблицы.
     * @param args   Параметры.
     * @param curPos Текущая позиция.
     * @return Количество распарсеных аргументов.
     */
    private int parseParams(Table.TableParams params, String[] args, int curPos) {
        int parsedCount = 0;
        boolean end = false;
        for (int i = curPos; i < args.length && !end; ++i) {
            String arg = args[i];
            String param, value = null;
            String buf[];

            //Следующий кусок, заканчиваем
            if (arg.startsWith("-")) {
                return parsedCount;
            }

            //Парсим параметр
            int eqPos = arg.indexOf("=");
            if (eqPos < 0) {
                param = arg;
            } else if (eqPos == arg.length() - 1) {
                param = arg.split("=")[0];
            } else {
                buf = arg.split("=");
                param = buf[0];
                value = buf[1];
            }

            //Получаем команду
            ArgCommands cmd;
            try {
                cmd = ArgCommands.valueOf(param.toUpperCase());
            } catch (IllegalArgumentException e) {
                return parsedCount;
            }
            switch (cmd) {
                case NAME:
                    params.name = value;
                    parsedCount++;
                    break;
                case FILENAME:
                    params.filename = value;
                    parsedCount++;
                    break;
                case DELIMITER:
                    params.delimiter = value;
                    parsedCount++;
                    break;
                default:
                    end = true;
                    break;
            }
        }
        return parsedCount;
    }

    /**
     * Инициализация таблицы.
     *
     * @throws Exception При ошибке.
     */
    @Override
    public void init() throws Exception {
        for (Table.TableParams params : mTableParams) {
            if (params.name == null || params.name.isEmpty()) {
                throw new IllegalArgumentException("Не задано имя таблицы");
            }
            params.name = params.name.toLowerCase();
            mTables.put(params.name, new Table(params));//Вставка новой таблицы
        }
    }

    /**
     * Метод обработки запросов.
     *
     * @param response Объект ответа.
     * @param socket   Сокет входящего подключения.
     * @param method   Метод.
     * @param urls     URL структура запроса.
     * @param level    Текущий уровень в URL структуре.
     * @param header   Заголовок запроса.
     * @param params   Параметры запроса (url?params...).
     * @param data     POST body запроса.   @return Успешность ответа.
     */
    @Override
    public void processSocket(Response response, Socket socket, HTTPConstants.METHOD method, String[] urls, int level, String[] header, HashMap<String, String> params, String data) {
        if (HTTPConstants.checkUrlLength(urls, level)) {

            URLS url = URLS.valueOf(urls[level].toUpperCase());
            switch (url) {
                case TABLE:
                    invokeTable(response, method, urls, level + 1, params, data);
                    break;
                case MANAGER:
                    invokeManager(response, method, urls, level + 1, params, data);
                    break;
                default:
                    response.setData(getModuleFullDescription());
                    break;
            }
        } else {
            response.setData(getModuleFullDescription());
        }
    }

    /**
     * Полное описание модуля.
     *
     * @return Полное описание модуля.
     */
    private String getModuleFullDescription() {
        return "Модуль поддерживает два режима работы: доступ к таблице и менеджер таблиц.<br/>" +
                "Доступ к таблице можно получить, если вызвать host:port/vts/table/<table_name>, доступ к менеджеру осуществляется при помощи команды /vts/manager";
    }

    /**
     * Вызов менеджера... Пока не реализовано.
     *
     * @param response Ответ.
     * @param method   Метод.
     * @param urls     УРЛЫ.
     * @param level    Уровень в урлах.
     * @param params   Параметры.
     * @param data     Пост-боди.
     */
    private void invokeManager(Response response, HTTPConstants.METHOD method, String[] urls, int level, HashMap<String, String> params, String data) {
        if (params == null || params.isEmpty() || HTTPConstants.checkUrlLength(urls, level)) {
            response.setData(getManagerDescription());
            return;
        }

        COMMANDS cmd;
        //Разбор команды команду
        if (params.get(Table.PARAMS.CMD.toString()) == null) {
            cmd = COMMANDS.GET;
        } else {
            try {
                cmd = COMMANDS.valueOf(params.get(Table.PARAMS.CMD.toString()).toUpperCase());
            } catch (IllegalArgumentException e) {//Команда не найдена
                response.setData("Неизвестная команда " + params.get(Table.PARAMS.CMD.toString()) + "<br/><br/>" + getTableDescription());
                response.setCode(C400);
                return;
            }
        }

        invokeManagerCmd(response, cmd, params, data);
    }

    private void invokeManagerCmd(Response response, COMMANDS cmd, HashMap<String, String> params, String data) {
        switch (cmd) {
            case CREATE:
                String tableName = params.get("table");
                String fileName = params.get("filename");
                String delimiter = params.get("delimiter");
                if (tableName == null || tableName.isEmpty()) {
                    response.setData("Не указано имя таблицы");
                    response.setCode(C400);
                    return;
                }
                Table.TableParams tableParams = new Table.TableParams();
                tableParams.name = tableName.toLowerCase();
                tableParams.filename = fileName;
                tableParams.delimiter = delimiter;

                try {
                    Table table = new Table(tableParams);
                    mTables.put(tableName, table);
                } catch (IOException e) {
                    throw new RuntimeException("Невозможно создать таблицу", e);
                }
                response.setData("OK");
                break;
            case DELETE:
                tableName = params.get("table");
                if (tableName == null || tableName.isEmpty()) {
                    response.setData("Не указано имя таблицы");
                    response.setCode(C400);
                    return;
                }
                if (!mTables.containsKey(tableName.toLowerCase())) {
                    response.setData("Таблица " + tableName + " не найдена");
                    response.setCode(C400);
                    return;
                }

                mTables.remove(tableName);
                response.setData("OK");
                break;
            case RELOAD:
                tableName = params.get("table");
                fileName = params.get("filename");
                delimiter = params.get("delimiter");
                if (tableName == null || tableName.isEmpty()) {
                    response.setData("Не указано имя таблицы");
                    response.setCode(C400);
                    return;
                }
                if (!mTables.containsKey(tableName.toLowerCase())) {
                    response.setData("Таблица " + tableName + " не найдена");
                    response.setCode(C400);
                    return;
                }
                Table table = mTables.get(tableName);
                try {
                    table.reload(fileName, delimiter);
                } catch (IOException e) {
                    throw new RuntimeException("Невозможно перезагрузить таблицу", e);
                }
                response.setData("OK");
                break;
            default:
                response.setData("Команда " + cmd.toString() + " не найдена");
                response.setCode(C400);
                break;
        }
    }

    private String getManagerDescription() {
        return "Менеджер таблиц:<br/>" +
                "Пример: http://localhost:8080/vts/manager??param=value&amp;...&amp;param=value<br/>" +
                "Список команд:<br/>" +
                "cmd - команда менеджеру, возможные значения:<br/>" +
                space(1, "create - создать новую таблицу. Параметры:<br/>") +
                space(2, "table, имя таблицы<br/>") +
                space(2, "[filename], если указано - будет загружена из файла<br/>") +
                space(2, "[delimiter], если не указан, используется '" + Table.DEFAULT_DELIMITER + "'<br/>") +
                space(1, "delete - удалить таблицу. Параметры:<br/>") +
                space(2, "table, имя таблицы для удаления<br/>") +
                space(1, "reload - перезагрузить таблицу из файла. Параметры:<br/>") +
                space(2, "table, имя таблицы<br/>") +
                space(2, "[filename], обязателен для таблиц, созданных не из файлов. Указывает путь к файлу<br/>") +
                space(2, "[delimiter], если не указан, используется '" + Table.DEFAULT_DELIMITER + "'<br/>") +
                //space(2, "")+

                "Список имеющихся таблиц:<br/>" +
                getTablesName(1);
    }

    /**
     * Вызов таблицы... Пока не реализовано.
     *
     * @param response Ответ.
     * @param method   Метод.
     * @param urls     УРЛЫ.
     * @param level    Уровень в урлах.
     * @param params   Параметры.
     * @param data     Пост-боди.
     */
    private void invokeTable(Response response, HTTPConstants.METHOD method, String[] urls, int level, HashMap<String, String> params, String data) {
        if (HTTPConstants.checkUrlLength(urls, level)) {
            String tableName = urls[level].toLowerCase();//Имя таблицы
            Table table = mTables.get(tableName); //Сама таблица
            if (table != null) {
                COMMANDS cmd;
                //Разбор команды команду
                if (params.get(Table.PARAMS.CMD.toString()) == null) {
                    cmd = COMMANDS.GET;
                } else {
                    try {
                        cmd = COMMANDS.valueOf(params.get(Table.PARAMS.CMD.toString()).toUpperCase());
                    } catch (IllegalArgumentException e) {//Команда не найдена
                        response.setData("Неизвестная команда " + params.get(Table.PARAMS.CMD.toString()) + "<br/><br/>" + getTableDescription());
                        response.setCode(C400);
                        return;
                    }
                }

                //Проверяем метод
                if ((cmd.equals(COMMANDS.GET) || cmd.equals(COMMANDS.POP)) && !method.equals(HTTPConstants.METHOD.GET)) {
                    response.setCode(C400);
                    response.setData("Неверный тип запроса. Используйте GET запрос.");
                    return;
                } else if ((cmd.equals(COMMANDS.PUSH)) && !method.equals(HTTPConstants.METHOD.POST) && !method.equals(HTTPConstants.METHOD.PUT)) {
                    response.setCode(C400);
                    response.setData("Неверный тип запроса. Используйте POST или PUT запрос.");
                    return;
                }

                //Вызываем обработку
                invokeTableCmd(response, table, cmd, params, data);
            } else {
                response.setData("Таблица " + tableName + " не найдена!<br/><br/>" + getTableDescription());
                response.setCode(C400);
            }
        } else {
            response.setData("Таблица не выбрана<br/><br/>" + getTableDescription());
        }
    }

    /**
     * Обработка команды.
     *
     * @param response Ответ.
     * @param table    Таблица.
     * @param cmd      Команда.
     * @param params   Параметры.
     * @param data     POST-body.
     */
    private void invokeTableCmd(Response response, Table table, COMMANDS cmd, HashMap<String, String> params, String data) {
        switch (cmd) {
            case GET:
                response.setData(table.get(params));
                break;
            case POP:
                response.setData(table.pop(params));
                break;
            case PUSH:
                response.setData(table.push(params, data));
                break;
            default:
                response.setData("Команда не найдена<br/><br/>" + getTableDescription());
                response.setCode(C400);
                break;
        }
    }

    /**
     * Описание таблицы.
     *
     * @return Описание таблицы.
     */
    private String getTableDescription() {
        return "Получение доступа к таблице:<br/>" +
                "Пример: http://localhost:8080/vts/table/&lt;table_name&gt;[?param=value&amp;...&amp;param=value]<br/>" +
                "Список команд:<br/>" +
                "cmd - команда таблице, возможные значения:<br/>" +
                space(1, "get - прочитать значение строки. Параметры:<br/>") +
                space(2, "[row], при отсутствии - получить текущую строчку и сдвинуть указатель на следующий элемент, иначе - вернуть указанную строку<br/>") +
                space(2, "[col], если указан - возвращает не строку, а только выбранную ячейку<br/>") +
                space(2, "[delimiter], если не указан, используется '" + Table.DEFAULT_DELIMITER + "'<br/>") +
                space(1, "pop - забрать строку/ячейку из таблицы с удалением строки. Если забирается последний элемент, строка будет удалена в любом случае. Параметры: см. get, а так же:<br/>") +
                space(2, "[deleterow] -  удалить целую строку, иначе только указанную ячейку. По умолчанию - true<br/>") +
                space(1, "push - вставить строку в таблицу. Значение передаётся в POST Body. Параметры:<br/>") +
                space(2, "[row], при указании - вставить перед указанной позицией, иначе - перед текущей<br/>") +
                space(2, "[col], вставить новую ячейку перед указанной позицией, если значение не указано - в конец<br/>") +
                space(2, "[delimiter], если не указан, используется '" + Table.DEFAULT_DELIMITER + "'<br/>") +
                //space(2, "")+

                "Список имеющихся таблиц:<br/>" +
                getTablesName(1);


        //space(, "<br/>")
    }

    /**
     * Возвращает список имен таблиц.
     *
     * @return Список имен таблиц.
     */
    private String getTablesName(int level) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Table> t : mTables.entrySet()) {
            sb.append(space(level, t.getValue().getName() + " - " + t.getValue().size() + " строк<br/>"));
        }
        return sb.toString();
    }

    /**
     * Возвращает описание модуля.
     *
     * @return Описание модуля.
     */
    @Override
    public String getModuleDescription() {
        return "/" + URLS.VTS + " - VTSModule - сервер виртуальных таблиц.";
    }

    /**
     * Регистрация URL.
     *
     * @param requests Карта запросов и модулей.
     */
    @Override
    public void registerUrls(HashMap<String, HTTPModule> requests) {
        requests.put(URLS.VTS.toString(), this);
    }

    /**
     * Команды запуска.
     */
    private enum ArgCommands {
        MODULE_CMD("-vts"),
        NAME("name"),
        FILENAME("filename"),
        DELIMITER("delimiter");

        private final String mValue;

        ArgCommands(String value) {
            mValue = value;
        }

        @Override
        public final String toString() {
            return mValue;
        }
    }

    /**
     * Поддерживаемые URL`ы первого уровня для модуля.
     */
    private enum URLS {
        TABLE("table"),
        MANAGER("manager"),
        VTS("vts");

        private final String mValue;

        URLS(String value) {
            mValue = value;
        }

        @Override
        public String toString() {
            return mValue;
        }
    }

    /**
     * Команды.
     */
    private enum COMMANDS {
        GET("get"),
        POP("pop"),
        PUSH("push"),
        CREATE("create"),
        DELETE("delete"),
        RELOAD("reload");

        private final String mValue;

        COMMANDS(String value) {
            mValue = value;
        }

        @Override
        public String toString() {
            return mValue;
        }
    }
}
