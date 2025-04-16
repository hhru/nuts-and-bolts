package ru.hh.nab.jpa.model.test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class PackageScanEntity {
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  private Integer id;
}
