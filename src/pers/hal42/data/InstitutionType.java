package pers.hal42.data;

public enum InstitutionType {
  HoldingCompany("H"),
  Bank("B"),
  CreditUnnion("C"),
  Branch("R");

  public String abbrev;

  InstitutionType(String abbrev) {
    this.abbrev = abbrev;
  }

}

