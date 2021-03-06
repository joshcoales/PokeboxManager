package uk.org.spangle.data;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "generation")
public class Generation {

    private int id;
    private String name;
    private String description;
    private List<GenerationBox> generationBoxes;

    public Generation() {
        // this form used by Hibernate
    }

    public Generation(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="generation_id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @OneToMany(mappedBy="generation")
    @OrderBy("ordinal")
    public List<GenerationBox> getGenerationBoxes() {
        return generationBoxes;
    }

    public void setGenerationBoxes(List<GenerationBox> generationBoxes) {
        this.generationBoxes = generationBoxes;
    }
}