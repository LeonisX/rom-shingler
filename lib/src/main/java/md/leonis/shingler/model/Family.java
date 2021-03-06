package md.leonis.shingler.model;

import md.leonis.shingler.utils.Measured;

import java.io.Serializable;
import java.util.*;

// TODO Do we need Serializable, Cloneable???
public class Family implements Serializable, Cloneable {

    private static final long serialVersionUID = -3737249230697805286L;

    private String id = UUID.randomUUID().toString();

    private String name;
    private String tribe;
    private List<Name> members;
    private Name mother;
    private List<Result> relations;
    private Map<String, Integer> relationsCount;
    private Set<String> individualRelations;

    private FamilyType type;

    private boolean skip = false;

    public Family() {
        // For Jackson
    }

    public Family(String name, List<Name> members, FamilyType type) {
        this.members = members;
        this.name = name;
        this.tribe = name;
        relations = new ArrayList<>();
        relationsCount = new HashMap<>();
        individualRelations = new HashSet<>();
        this.type = type;
    }

    public Family(List<Name> members) {
        this.members = members;
        //TODO may be get with best rating right here
        this.name = members.get(0).getCleanName();
        this.tribe = this.name;
        relations = new ArrayList<>();
        relationsCount = new HashMap<>();
        individualRelations = new HashSet<>();
        this.type = FamilyType.FAMILY;
    }

    public Family(Family family) {
        setName(family.getName());
        setTribe(family.getTribe());
        setMembers(new ArrayList<>(family.getMembers()));
        setMother(family.getMother());
        setRelations(new ArrayList<>(family.getRelations()));
        setRelationsCount(new HashMap<>(family.getRelationsCount()));
        setIndividualRelations(new HashSet<>(family.getIndividualRelations()));
        setSkip(family.isSkip());
        setType(family.getType());
        this.members.sort((d1, d2) -> Double.compare(d2.getJakkardStatus(), d1.getJakkardStatus()));
    }

    public Family(String name, String tribe, List<Name> members, Name mother, List<Result> relations, Map<String, Integer> relationsCount, Set<String> individualRelations, boolean skip, FamilyType type) {
        this.name = name;
        this.tribe = tribe;
        this.members = members;
        this.mother = mother;
        this.relations = relations;
        this.relationsCount = relationsCount;
        this.individualRelations = individualRelations;
        this.skip = skip;
        this.type = type;
    }

    @Override
    public String toString() {
        return name + ": " + members;
    }

    public FamilyType getType() {
        return type;
    }

    public String getTribe() {
        return tribe;
    }

    public void setTribe(String tribe) {
        this.tribe = tribe;
    }

    public void setType(FamilyType type) {
        this.type = type;
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

    public void setMembers(List<Name> members) {
        this.members = members;
    }

    public Name getMother() {
        return mother;
    }

    public void setMother(Name mother) {
        this.mother = mother;
    }

    public void selectMother() {
        if (getMembers().size() == 1) {
            setMother(getMembers().get(0));
        } else {
            setMother(getMembers().stream().max(Comparator.comparing(Name::getJakkardStatus)).orElse(null));
        }
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Measured
    public void addRelation(Result result) {
        relations.add(result);
        /*Integer count = relationsCount.get(result.getName1().getName());
        if (count == null) {
            relationsCount.put(result.getName1().getName(), 0);
        } else {
            relationsCount.replace(result.getName1().getName(), ++count);
        }
        individualRelations.add(join(result.getName1(), result.getName2()));*/
    }

    private String join(Name name1, Name name2) {
        return name1.getName() + name2.getName();
    }

    @Measured
    public boolean containsRelation(Name name1, Name name2) {
        throw new RuntimeException("");
        /*return individualRelations.contains(join(name1, name2));*/
        //return relations.stream().filter(r -> r.getName1().getName().equals(name1.getName())).anyMatch(r -> r.getName2().getName().equals(name2.getName()));
    }

    @Measured
    public boolean hasAllRelations(Name name, int index) {
        throw new RuntimeException("");
        /*int expectedRelations = members.size() - index - 1;
        Integer count = relationsCount.get(name.getName());
        return count != null && count == expectedRelations;*/
    }

    @Measured
    public double getJakkardStatus(int index) {
        if (members.size() < 2) {
            return 0;
        }
        Name name = members.get(index);
        return name.getJakkardStatus() / (members.size() - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Family family = (Family) o;
        return Objects.equals(id, family.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
