package ru.hh.nab.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ru.hh.nab.jpa.model.test.TestConverter;

@Entity
@Table(name = "test_entity")
public class TestEntity {

  @Column(name = "test_entity_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  private Integer id;

  @Convert(converter = TestConverter.class)
  @Column(name = "name", nullable = false)
  private String name;

  public TestEntity() {
  }

  public TestEntity(String name) {
    this.name = name;
  }

  public Integer getId() {
    return id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
