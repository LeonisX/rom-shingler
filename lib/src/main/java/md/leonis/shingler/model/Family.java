package md.leonis.shingler.model;

import md.leonis.shingler.utils.Measured;

import java.io.Serializable;
import java.util.*;

public class Family implements Serializable, Cloneable {

    private static final long serialVersionUID = -3737249230697805286L;

    private String name;
    private List<Name> members;
    private Name mother;
    private List<Result> relations;
    private Map<String, Integer> relationsCount;
    private Set<String> individualRelations;

    private boolean skip = false;

    public Family(String name, List<Name> members) {
        this.members = members;
        this.name = name;
        relations = new ArrayList<>();
        relationsCount = new HashMap<>();
        individualRelations = new HashSet<>();
    }

    public Family(List<Name> members) {
        this.members = members;
        name = members.get(0).getCleanName();
        relations = new ArrayList<>();
        relationsCount = new HashMap<>();
        individualRelations = new HashSet<>();
    }

    public Family(Family family) {
        setName(family.getName());
        setMembers(new ArrayList<>(family.getMembers()));
        setMother(family.getMother());
        setRelations(new ArrayList<>(family.getRelations()));
        setRelationsCount(new HashMap<>(family.getRelationsCount()));
        setIndividualRelations(new HashSet<>(family.getIndividualRelations()));
        setSkip(family.isSkip());
        this.members.sort((d1, d2) -> Double.compare(d2.getJakkardStatus(), d1.getJakkardStatus()));
    }

    @Override
    public String toString() {
        return name + ": " + members;
    }

    public boolean isSkip() {
        return skip;
    }

    void setSkip(boolean skip) {
        this.skip = skip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Name> getMembers() {
        return members;
    }

    void setMembers(List<Name> members) {
        this.members = members;
    }

    public Name getMother() {
        return mother;
    }

    public void setMother(Name mother) {
        this.mother = mother;
    }

    public List<Result> getRelations() {
        return relations;
    }

    public void setRelations(List<Result> relations) {
        this.relations = relations;
    }

    public int size() {
        return members.size();
    }

    public Name get(int i) {
        return members.get(i);
    }

    public Map<String, Integer> getRelationsCount() {
        return relationsCount;
    }

    void setRelationsCount(Map<String, Integer> relationsCount) {
        this.relationsCount = relationsCount;
    }

    public Set<String> getIndividualRelations() {
        return individualRelations;
    }

    public void setIndividualRelations(Set<String> individualRelations) {
        this.individualRelations = individualRelations;
    }

    @Measured
    public void addRelation(Result result) {
        relations.add(result);
        Integer count = relationsCount.get(result.getName1().getName());
        if (count == null) {
            relationsCount.put(result.getName1().getName(), 0);
        } else {
            relationsCount.replace(result.getName1().getName(), ++count);
        }
        individualRelations.add(join(result.getName1(), result.getName2()));
    }

    private String join(Name name1, Name name2) {
        return name1.getName() + name2.getName();
    }

    @Measured
    public boolean containsRelation(Name name1, Name name2) {
        return individualRelations.contains(join(name1, name2));
        //return relations.stream().filter(r -> r.getName1().getName().equals(name1.getName())).anyMatch(r -> r.getName2().getName().equals(name2.getName()));
    }

    @Measured
    public boolean hasAllRelations(Name name, int index) {
        int expectedRelations = members.size() - index - 1;
        Integer count = relationsCount.get(name.getName());
        return count != null && count == expectedRelations;
    }

    @Measured
    public double getJakkardStatus(int index) {
        if (members.size() < 2) {
            return 0;
        }
        Name name = members.get(index);
        return name.getJakkardStatus() / (members.size() - 1);
    }
}
