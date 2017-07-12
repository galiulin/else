package ru.pflb.httpserver.core;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;

/**
 * Базовый класс модулей для сервера.
 */
public abstract class HTTPModule {

    /**
     * Метод парсинга параметров запуска приложения.
     * @param arg Текущий аргумент.
     * @param args Список всех аргументов.
     * @param curPos Текущий индекс аргумента.
     * @return Количество принятых аргументов.
     */
    public abstract int parseArgs(String arg, String[] args, int curPos);

    /**
     * Метод инициализации модуля.
     * @throws Exception Бросает Exception при проблемах в инициализации.
     */
    public abstract void init() throws Exception;

    /**
     * Метод обработки входящих запросов.
     *
     * @param response Объект ответа.
     * @param socket   Сокет входящего подключения.
     * @param method   Метод.
     * @param urls     URL структура запроса.
     * @param level    Текущий уровень в URL структуре.
     * @param header   Заголовок запроса.
     * @param params   Параметры запроса (url?params...).
     * @param data     POST body запроса.   @return {@code true}, если запрос обработан, иначе {@code false}.
     */
    public abstract void processSocket(Response response, Socket socket, HTTPConstants.METHOD method, String[] urls, int level, String[] header, HashMap<String, String> params, String data);

    /**
     * Текстовое описание модуля.
     * @return Описание модуля.
     */
    public abstract String getModuleDescription();

    /**
     * Регистрация url, при вызове которого будет вызван метод {@link HTTPModule#processSocket(Response, Socket, HTTPConstants.METHOD, String[], int, String[], HashMap, String)}.
     * @param requests Карта запросов и модулей.
     */
    public abstract void registerUrls(HashMap<String, HTTPModule> requests);

    /**
     * Метод, который делает отступы.
     * @param level Уровень отступов.
     * @param str Строка, которую нужно сдвинуть.
     * @return Сдвинутая строка.
     */
    protected String space(int level, String str) {
        StringBuilder sb = new StringBuilder(level * 5 * 6);
        for (int i = 0; i < level; i++) {
            sb.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        }
        return sb.append(str).toString();
    }

    /**
     * Класс ответа.
     */
    public static class Response {
        private Socket mSocket; //Сокет для ответа
        private HTTPConstants.CODE mCode;//Код ответа
        private String mData; //Ответ

        /**
         * Конструктор.
         * @param socket Сокет.
         * @param code Код ответа.
         */
        public Response(Socket socket, HTTPConstants.CODE code) {
            this(socket, code, null);
        }

        /**
         * Конструктор.
         * @param socket Сокет.
         * @param data Ответ.
         */
        public Response(Socket socket, String data) {
            this(socket, HTTPConstants.CODE.C200, data);
        }

        /**
         * Конструктор.
         * @param socket Сокет.
         */
        public Response(Socket socket) {
            this(socket, HTTPConstants.CODE.C200, null);
        }

        /**
         * Полный конструктор.
         *
         * @param socket Сокет.
         * @param code   Код ответа.
         * @param data   Ответ.
         */
        public Response(Socket socket, HTTPConstants.CODE code, String data) {
            mSocket = socket;
            mCode = code;
            mData = data;
        }

        /**
         * Метод отправки ответа клиенту.
         * @throws IOException Бросает исключение при ошибках.
         */
        public void send() throws IOException {
            StringBuilder response = new StringBuilder();
            //Заполнение заголовка
            response.append("HTTP/1.1 ").append(mCode.name().substring(1)).append(" ").append(mCode.toString()).append("\r\n")
                    .append("Server: PFLBServer/2016\r\n")
                    .append("Content-Type: text/html; charset=UTF-8\r\n")
                    .append("Content-Length: ").append(mData == null ? 0 : mData.getBytes("UTF-8").length).append("\r\n")
                    .append("Connection: close\r\n\r\n");
            //Заполнение ответа
            if (mData != null && !mData.isEmpty())
                response.append(mData);
            //Запись ответа
            OutputStream out = mSocket.getOutputStream();
            out.write((response.toString()).getBytes("UTF-8"));
            out.flush();
        }

        /**
         * Устанавливает сокет ответа.
         *
         * @param socket Сокет.
         */
        public void setSocket(Socket socket) {
            mSocket = socket;
        }

        /**
         * Устанавливает код ответа.
         *
         * @param code Код ответа.
         */
        public void setCode(HTTPConstants.CODE code) {
            mCode = code;
        }

        /**
         * Устанавливает ответ.
         *
         * @param data Ответ.
         */
        public void setData(String data) {
            mData = data;
        }

        /**
         * Устанавливает код ответа 200.
         */
        public void setOK() {
            setOK(HTTPConstants.CODE.C200.toString());
        }

        /**
         * Устанавливает код ответа 200 и сам ответ.
         * @param data Ответ.
         */
        public void setOK(String data) {
            mCode = HTTPConstants.CODE.C200;
            mData = data;
        }
    }
}
