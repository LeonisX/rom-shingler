package md.leonis.crawler.moby.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String file) {
        super(file);
    }
}
