package com.ruzinn.dto;

import java.util.ArrayList;
import java.util.List;

public class ExperienceCatalog {
  private String currency = "AED";
  private String source;
  private List<CatalogCountry> countries = new ArrayList<>();
  private List<Experience> experiences = new ArrayList<>();
  private List<Experience> extras = new ArrayList<>();

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public List<CatalogCountry> getCountries() {
    return countries;
  }

  public void setCountries(List<CatalogCountry> countries) {
    this.countries = countries;
  }

  public List<Experience> getExperiences() {
    return experiences;
  }

  public void setExperiences(List<Experience> experiences) {
    this.experiences = experiences;
  }

  public List<Experience> getExtras() {
    return extras;
  }

  public void setExtras(List<Experience> extras) {
    this.extras = extras;
  }
}
