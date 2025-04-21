package com.shizzy.moneytransfer.util;

import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.util.CsvContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FmtLocalDateTime extends CellProcessorAdaptor {
    private final DateTimeFormatter formatter;

    public FmtLocalDateTime(String pattern) {
        super();
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    public FmtLocalDateTime(String pattern, CellProcessor next) {
        super(next);
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public Object execute(Object value, CsvContext context) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof LocalDateTime localDateTime)) {
            throw new SuperCsvCellProcessorException(
                    "the input value should be of type LocalDateTime but is " + value.getClass().getName(),
                    context, this);
        }

        String formattedDate = localDateTime.format(formatter);
        return next.execute(formattedDate, context);
    }
}
