package ru.pflb.httpserver.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static ru.pflb.httpserver.core.HTTPConstants.getValue;

/**
 * Потокобезопасная(?) таблица
 */
public class Table {
    public static final String DEFAULT_DELIMITER = ";"; //Разделитель по умолчанию

    private final String mName; //Имя таблицы
    private final List<Vector<String>> mTable = Collections.synchronizedList(new ArrayList<Vector<String>>());//Сама таблица
    private String mFileName; //Имя файла для загрузки
    private String mDelimiter; //Разделитель
    private int mCursor = 0; //Текущая позиция

    /**
     * Конструктор.
     *
     * @param params Параметры таблицы
     * @throws IOException При ошибке.
     */
    public Table(TableParams params) throws IOException {
        mName = params.name;
        mFileName = params.filename;
        mDelimiter = params.delimiter == null ? DEFAULT_DELIMITER : params.delimiter;

        if (mFileName != null && !mFileName.isEmpty()) {
            loadFromFile();
        }
    }

    /**
     * Загрузка из файла.
     * @throws IOException При ошибке.
     */
    private void loadFromFile() throws IOException {
        synchronized (this) {
            File f = new File(mFileName);
            if (!f.exists() || !f.canRead())
                throw new IOException("Невозможно прочесть файл: " + f.getAbsolutePath());
            BufferedReader br = new BufferedReader(new FileReader(f));
            String row;
            while (br.ready()) {
                row = br.readLine();
                insertNewRow(row);
            }
        }
    }

    /**
     * Вставка строки в таблицу.
     * @param row Строка.
     */
    private void insertNewRow(String row) {
        synchronized (this) {
            String[] cells = row.split(mDelimiter);
            Vector<String> v = new Vector<>(cells.length);
            v.addAll(Arrays.asList(cells));
            mTable.add(v);
        }
    }

    /**
     * Размер таблицы.
     *
     * @return Размер таблицы.
     */
    public int size() {
        return mTable.size();
    }

    /**
     * Получение строки/ячейки.
     *
     * @param params Параметры.
     * @return Строка/ячейка.
     */
    public String get(HashMap<String, String> params) {
        Integer row = getValue(params, PARAMS.ROW.toString(), (Integer) null);
        Integer col = getValue(params, PARAMS.COL.toString(), (Integer) null);
        String delimiter = getValue(params, PARAMS.DELIMITER.toString(), mDelimiter);

        return get(row, col, delimiter);
    }

    /**
     * Получение строки/ячейки.
     *
     * @param row       Строка.
     * @param col       Столбец.
     * @param delimiter Разделитель.
     * @return Строка/ячейка.
     */
    private String get(Integer row, Integer col, String delimiter) {
        Vector<String> res;
        synchronized (this) {
            if(mTable.size()==0){
                throw new IllegalArgumentException("Таблица пуста");
            }
            if (row == null) {
                row = mCursor;
            } else {
                if (row >= mTable.size()) {
                    throw new IllegalArgumentException("Индекс строки равен или превышает размер таблицы: " + row + " из " + mTable.size());
                }
            }
            res = mTable.get(row);
            incrementCursor();
        }
        if (col == null) {
            return createStringFromRow(res, delimiter);
        } else {
            if (col >= res.size() || col < 0) {
                throw new IllegalArgumentException("Индекс столбца равен или превышает размер строки: " + col + " из" + res.size());
            }
            return res.get(col);
        }
    }

    /**
     * Создание строки из параметров, разделенных разделителем.
     *
     * @param values    Строка из ячеек.
     * @param delimiter Разделитель.
     * @return Составленная строка.
     */
    private String createStringFromRow(Vector<String> values, String delimiter) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < values.size(); ++i) {
            sb.append(values.get(i));
            if (i != values.size() - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    /**
     * Увеличение курсора.
     */
    private void incrementCursor() {
        mCursor++;
        if (mCursor >= mTable.size()) {
            mCursor = 0;
        }
    }

    private void checkCursor(){
        if (mCursor >= mTable.size()) {
            mCursor = 0;
        }
    }

    /**
     * Забор из таблицы.
     *
     * @param params Параметры.
     * @return Строка/ячейка.
     */
    public String pop(HashMap<String, String> params) {
        Integer row = getValue(params, PARAMS.ROW.toString(), (Integer) null);
        Integer col = getValue(params, PARAMS.COL.toString(), (Integer) null);
        String delimiter = getValue(params, PARAMS.DELIMITER.toString(), mDelimiter);
        Boolean deleteRow = getValue(params, PARAMS.DELETEROW.toString(), true);

        return pop(row, col, delimiter, deleteRow);
    }

    /**
     * Забор из таблицы.
     *
     * @param row       Строка.
     * @param col       Колонка.
     * @param delimiter Разделитель.
     * @param deleteRow Удаление строки.
     * @return Строка/ячейка.
     */
    private String pop(Integer row, Integer col, String delimiter, Boolean deleteRow) {
        Vector<String> res;
        synchronized (this) {
            if(mTable.size()==0){
                throw new IllegalArgumentException("Таблица пуста");
            }
            if (row == null) {
                row = mCursor;
            } else {
                if (row >= mTable.size()) {
                    throw new IllegalArgumentException("Индекс струоки равен или превышает размер таблицы: " + row + " из " + mTable.size());
                }
            }
            res = mTable.get(row);
            if (deleteRow || col == null) {
                mTable.remove(row.intValue());
            }
            mCursor--;
            incrementCursor();
            if (col == null) {
                return createStringFromRow(res, delimiter);
            } else {
                if (col >= res.size() || col < 0) {
                    throw new IllegalArgumentException("Индекс столбца равен или превышает размер строки: " + col + " из " + res.size());
                }
                if (res.size() <= 1) {
                    mTable.remove(row.intValue());
                    return createStringFromRow(res, delimiter);
                }
                String result = res.get(col);
                res.remove(col.intValue());
                return result;
            }
        }
    }

    /**
     * Загрузить в таблицу
     *
     * @param params Параметры.
     * @param data   Строка.
     * @return Результат.
     */
    public String push(HashMap<String, String> params, String data) {
        Integer row = getValue(params, PARAMS.ROW.toString(), (Integer) null);
        Integer col = getValue(params, PARAMS.COL.toString(), (Integer) null);
        String delimiter = getValue(params, PARAMS.DELIMITER.toString(), mDelimiter);

        return push(row, col, delimiter, data);
    }

    /**
     * Загрузить в таблицу.
     *
     * @param row       Строка.
     * @param col       Столбец.
     * @param delimiter Разделитель.
     * @param data      Данные.
     * @return Результат.
     */
    private String push(Integer row, Integer col, String delimiter, String data) {
        synchronized (this) {
            if (row == null) {
                row = mCursor;
            } else {
                int maxRow = mTable.size() + (col == null ? 1 : 0);
                if (row > maxRow) {
                    throw new IllegalArgumentException("Индекс строки равен или превышает размер таблицы: " + row + " из " + mTable.size());
                }
            }
            if (col == null) {
                mTable.add(mCursor, createRowFromString(data, delimiter));
                incrementCursor();
                checkCursor();
            } else {
                Vector<String> res = mTable.get(row);
                if (col > res.size()) {
                    throw new IllegalArgumentException("Индекс столбца равен или превышает размер строки: " + col + " из " + res.size());
                }
                res.add(col, data);
            }
        }
        return "OK";
    }

    /**
     * Создание строки таблицы из строки.
     *
     * @param data      Данные.
     * @param delimiter Разделитель.
     * @return Строка таблицы.
     */
    private Vector<String> createRowFromString(String data, String delimiter) {
        Vector<String> res = new Vector<>();
        String[] buf = data.split(delimiter);
        Collections.addAll(res, buf);
        return res;
    }

    public String getName() {
        return mName;
    }

    public void reload(String fileName, String delimiter) throws IOException {
        if (fileName != null && !fileName.isEmpty()) {
            mFileName = fileName;
        }
        if (delimiter != null && !delimiter.isEmpty()) {
            mDelimiter = delimiter;
        }
        clear();
        loadFromFile();
    }

    private void clear() {
        synchronized (this) {
            mTable.clear();
        }
    }

    /**
     * Параметры.
     */
    public enum PARAMS {
        CMD("cmd"),
        ROW("row"),
        COL("col"),
        DELIMITER("delimiter"),
        DELETEROW("deleterow");

        private final String mValue;

        PARAMS(String value) {
            mValue = value;
        }

        @Override
        public String toString() {
            return mValue;
        }
    }

    /**
     * Параметры таблицы.
     */
    public static class TableParams {
        public String name, filename, delimiter;
    }
}
