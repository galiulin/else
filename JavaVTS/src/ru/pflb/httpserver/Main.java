package ru.pflb.httpserver;

import ru.pflb.httpserver.core.HTTPModule;
import ru.pflb.httpserver.core.HTTPServer;
import ru.pflb.httpserver.modules.VTSModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Запускатор всего и вся.
 */
class Main {
    private static final HTTPModule[] MODULES = new HTTPModule[]{new VTSModule()};//Список активных модулей
    private static CommandHandler mCommandHandler;
    private static HTTPServer mServer;

    public static void main(String[] args) throws IOException {
        try {
            //Создание сервера
            mServer = new HTTPServer(args, MODULES);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        //Запуск
        mServer.start(new HTTPServer.StopListener() {
            @Override
            public void onStop() {
                mCommandHandler.safeStop();
            }
        });

        //Обработчик команд с консоли
        mCommandHandler = new CommandHandler();
        mCommandHandler.start();

        //Ожидание потоков выполнения
        try {
            mCommandHandler.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            mServer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Обработчик команд.
     */
    public static class CommandHandler extends Thread {
        /**
         * Поток обработки.
         */
        @Override
        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (mServer.isRunning()) {
                String cmd;
                try {
                    cmd = reader.readLine();
                    if (cmd.equalsIgnoreCase("exit")) {
                        mServer.stop();
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Безопасная остановка.
         */
        public void safeStop() {
            interrupt();
        }
    }
}
