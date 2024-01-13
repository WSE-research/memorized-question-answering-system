package eu.wseresearch.memorizedquestionansweringsystem.exception;


@SuppressWarnings("serial")
public class QuestionNotExist extends Exception {
    public QuestionNotExist(String message) {
        super(message);
    }

    public QuestionNotExist(String message, Throwable cause) {
        super(message, cause);
    }
}
