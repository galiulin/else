package ru.pflb.httpserver.core;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

/**
 * HTTP сервер с поддержкой модулей.
 */
public class HTTPServer {
    private final HashMap<String, HTTPModule> mRequests = new HashMap<>(); //Карта соответствий запросов и модулей
    private int PORT = 8888; //Порт для прослушивания входящих запросов
    private Vector<HTTPModule> mModules;//Список всех загруженных модулей
    private ServerProcess mServerProcessor; //Обработчик входящих запросов
    private StopListener mListener;//Листенер остановки сервера

    /**
     * Конструктор по умолчанию
     *
     * @throws Exception При ошибках.
     */
    public HTTPServer() throws Exception {
        this(null, (HTTPModule) null);
    }

    /**
     * Конструктор с аргументами.
     *
     * @param args Аргументы.
     * @throws Exception При ошибках.
     */
    public HTTPServer(String[] args) throws Exception {
        this(args, (HTTPModule) null);
    }

    /**
     * Полный конструктор с аргументами и модулями.
     *
     * @param args    Аргументы.
     * @param modules Модули.
     * @throws Exception При ошибках.
     */
    public HTTPServer(String[] args, HTTPModule... modules) throws Exception {
        //Сохранение списка модулей
        if (modules != null) {
            mModules = new Vector<>(modules.length);
            Collections.addAll(mModules, modules);
        }
        //Разбор аргументов
        if (args != null)
            setArgs(args);
        //Запуск модулей
        if (mModules != null) {
            for (HTTPModule m : mModules)
                try {
                    m.init();//Инициализация
                    m.registerUrls(mRequests);//Регистрация
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Ошибка при загрузке модуля " + m.getClass().getSimpleName() + ", модуль не активирован");
                }
        }
    }

    /**
     * Запуск сервера.
     *
     * @param listener Листенер остановки сервера.
     */
    public void start(StopListener listener) {
        mListener = listener;
        start();
    }

    /**
     * Запуск сервера.
     */
    private void start() {
        System.out.println("Старт VTS...");

        //Запуск нового обработчика входящих запросов
        mServerProcessor = new ServerProcess();
        mServerProcessor.start();
    }

    /**
     * Остановка сервера.
     *
     * @throws IOException При ошибках.
     */
    public void stop() throws IOException {
        //Проверка на запущенность
        if (mServerProcessor != null && mServerProcessor.isAlive()) {
            mServerProcessor.safeStop();
        }
        //Если нужно вызвать листенер
        if (mListener != null) {
            mListener.onStop();
        }
    }

    /**
     * Разбор аргументов.
     *
     * @param args Аргументы.
     * @throws Exception При ошибках.
     */
    private void setArgs(String[] args) throws Exception {
        String arg;
        //Проход по всем аргументам
        for (int i = 0; i < args.length; ++i) {
            arg = args[i].toLowerCase();//Понижаем регистр чтобы точно совпало
            switch (arg) {
                case "-port"://Устанавливаем порт
                    try {
                        PORT = Integer.parseInt(args[++i]);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Невозможно распарсить порт: " + args[i], e);
                    }
                    break;
                default://Если не знаем такой, проверяем, может, аргумент модуля
                    if (mModules != null) {
                        boolean found = false;//Признак того, что модуль смог разобрать аргумент(ы)
                        int offset;//Сколько параметров съел модуль
                        for (HTTPModule m : mModules) {//Проход по модулям

                            offset = m.parseArgs(arg, args, i);//Запуск парсера модуля
                            //Если были съедены аргументы, значит, все ок
                            if (offset != 0) {
                                i += offset - 1;
                                found = true;
                                break;
                            }
                        }
                        //Если нет, значит, неизвестный аргумент
                        if (!found) {
                            throw new IllegalArgumentException("Неизвестный аргумент: " + args[i]);
                        }
                    }
            }
        }
    }

    /**
     * Выводит справку по серверу.
     *
     * @return Справка.
     */
    private String printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("Доступные страницы:<br/>");
        for (Map.Entry<String, HTTPModule> m : mRequests.entrySet()) {
            sb.append("<a href='"+m.getKey()+"'>"+m.getKey()+"</a> - "+m.getValue().getModuleDescription()).append("<br/><br/>");
        }

        return sb.toString();
    }

    /**
     * Проверяет на запущенность.
     *
     * @return {@code true}, если запущен, иначе {@code false}.
     */
    public boolean isRunning() {
        return mServerProcessor.isAlive();
    }

    /**
     * Ожидание остановки сервера.
     *
     * @throws InterruptedException При ошибке.
     */
    public void join() throws InterruptedException {
        mServerProcessor.join();
    }

    /**
     * Листенер остановки сервера.
     */
    public static abstract class StopListener implements EventListener {
        public abstract void onStop();
    }

    /**
     * Обработчик входящих соединений.
     */
    private class ServerProcess extends Thread {
        private volatile boolean mStopped = false; //Состояние
        private volatile ServerSocket serverSocket; //Сокет входящих соединений

        /**
         * Запуск серверного сокета.
         */
        @Override
        public synchronized void start() {
            try {
                serverSocket = new ServerSocket(PORT);
            } catch (IOException e) {
                throw new RuntimeException("Невозможно открыть порт " + PORT + ". Проверьте, не занят ли он");
            }
            super.start();
        }

        /**
         * Метод обработки входящих соединений.
         */
        @Override
        public void run() {
            try {
                //Создание сокета
                System.out.println("Сервер запущен на порту " + PORT);
                while (!mStopped) {
                    //Принятие входящего запроса
                    try {
                        Socket s = serverSocket.accept();
                        //Выдача ответа
                        new SocketProcessor(s).start();
                    } catch (SocketException e) {
                        if (!mStopped)
                            throw e;
                    }
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        /**
         * Остановка сервера.
         *
         * @throws IOException При ошибках.
         */
        public void safeStop() throws IOException {
            mStopped = true;
            serverSocket.close();
        }
    }

    /**
     * Обработчик входящих запросов.
     */
    private class SocketProcessor extends Thread {
        private final Socket mSocket; //Клиентский сокет.

        /**
         * Конструктор.
         *
         * @param socket Сокет.
         */
        private SocketProcessor(Socket socket) {
            this.mSocket = socket;
        }

        /**
         * Запуск потока обработки.
         */
        public void run() {
            try {
                processSocket();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                try {
                    mSocket.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        /**
         * Чтение входных данных и запуск обработчиков от модулей.
         *
         * @throws Throwable При ошибках.
         */
        private void processSocket() throws Throwable {
            BufferedReader reader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

            //Чтение заголовка
            String line = reader.readLine();
            String[] str = line.split(" ");

            //Парсинг метода и URL
            HTTPConstants.METHOD method = HTTPConstants.parseMethod(str[0]);
            String address = str[1];

            //Нормализация URL
            if (address.startsWith("/"))
                address = address.substring(1);
            String[] params = null;

            //Разбор параметров
            str = address.split("\\?");
            address = str[0];
            if (str.length > 1)
                params = str[1].split("&");

            //Разбор на уровни URL
            address = address.toLowerCase();
            String[] urls = address.split("/");
            if (urls.length == 0)
                urls = new String[]{""};

            //Тупо чтобы не заморачиваться на этот запрос
            if (urls[0].equalsIgnoreCase("favicon.ico")) {
                HTTPModule.Response response = new HTTPModule.Response(mSocket);
                //response.setCode(HTTPConstants.CODE.C404);
                response.send();
                return;
            }

            StringBuilder sb = new StringBuilder();
            String[] header;
            //Дочитываение заголовка
            while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
                sb.append(line);
            }
            header = sb.toString().replaceAll("\r\n", "\n").split("\n");

            sb.setLength(0);
            //Чтение POST-body
            String postData = null;
            while (reader.ready())
                sb.append((char) reader.read());
            if (sb.length() != 0)
                postData = sb.toString().replaceAll("\r\n", "\n");

            //Проверяем, что есть модуль для такой URL
            if (mRequests.containsKey(urls[0])) {
                try {
                    //Создаем пустой ответ
                    HTTPModule.Response response = new HTTPModule.Response(mSocket);
                    //Запуск обработчика и проверка, удалась ли обработка
                    mRequests.get(urls[0]).processSocket(response, mSocket, method, urls, 1, header, parseParams(params), postData);
                    response.send();//Отправка запроса
                } catch (IllegalArgumentException e) {
                    //Ошибка во входящих аргументах
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    (new HTTPModule.Response(mSocket, HTTPConstants.CODE.C400, sw.toString().replaceAll("\n", "<br/>"))).send();
                } catch (Throwable e) {
                    //Прочие ошибки
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    (new HTTPModule.Response(mSocket, HTTPConstants.CODE.C500, e.getMessage() + "\n\n" + sw.toString().replaceAll("\n", "<br/>"))).send();
                }
            } else {
                //Модуль не найден
                (new HTTPModule.Response(mSocket, HTTPConstants.CODE.C200, "Страница не найдена" + "<br/><br/>" + printHelp())).send();
            }
        }

        /**
         * Парсинг параметров вида param=value
         *
         * @param params Массив параметров.
         * @return Карта параметров.
         */
        private HashMap<String, String> parseParams(String[] params) {
            if (params == null) {//Если параметров нет
                return new HashMap<>();
            }
            HashMap<String, String> result = new HashMap<>(params.length);

            //Перебор всех параметров
            for (String s : params) {
                String buf[] = s.toLowerCase().split("=");//Делим параметр на имя и значение
                String[] param;
                if (buf.length < 2) {
                    param = new String[2];
                    param[0] = buf[0];
                } else if (buf.length > 2) {
                    param = new String[2];
                    param[0] = buf[0];
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < buf.length; ++i) {
                        sb.append(buf[i]);
                        if (i < buf.length - 1) {
                            sb.append("=");
                        }
                    }
                    param[1] = sb.toString();
                } else {
                    param = buf;
                }
                result.put(param[0], param[1]);
            }
            return result;
        }
    }
}