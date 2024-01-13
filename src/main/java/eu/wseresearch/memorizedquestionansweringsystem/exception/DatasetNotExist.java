package eu.wseresearch.memorizedquestionansweringsystem.exception;

import java.util.List;

@SuppressWarnings("serial")
public class DatasetNotExist extends Exception {
    public DatasetNotExist(String message, List<String> allDatasets) {
        super(message + " - available datasets: " + allDatasets.toString());
    }

    public DatasetNotExist(String message, List<String> allDatasets, Throwable cause) {
        super(message + " - available datasets: " + allDatasets.toString(), cause);
    }
}
