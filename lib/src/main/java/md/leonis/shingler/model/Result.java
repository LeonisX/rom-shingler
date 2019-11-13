package md.leonis.shingler.model;

import java.io.Serializable;

public class Result implements Serializable {

    private static final long serialVersionUID = -472204242580854693L;

    private Name name1;
    private Name name2;
    private double jakkard;

    public Result(Name name1, Name name2, double jakkard) {
        this.name1 = name1;
        this.name2 = name2;
        this.jakkard = jakkard;
    }

    public Name getName1() {
        return name1;
    }

    public Name getName2() {
        return name2;
    }

    public double getJakkard() {
        return jakkard;
    }

    @Override
    public String toString() {
        return "\"" + name1.getName() + "\",\"" + name2.getName() + "\",\"" + jakkard + "\"";
    }
}
