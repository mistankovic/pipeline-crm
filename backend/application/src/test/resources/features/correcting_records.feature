Feature: Correcting a company or a contact

  Names get typed wrong. A company that was created as "Acme" and is really "Acme Holdings"
  must be correctable without creating a second company, because everything already pointing
  at it — its contacts, its deals, its forecast line — must keep pointing at it.

  A contact's name and email can be corrected the same way. Who they work for cannot: moving
  a person between companies changes which deals they are relevant to, and that is a
  different decision. See docs/domain-decisions.md, decision D-21.

  Background:
    Given a salesperson "Sam"
    And a company "Acme"

  Scenario: A company is renamed
    When Sam renames the company "Acme" to "Acme Holdings"
    Then the companies are "Acme Holdings"

  Scenario: Renaming a company does not create a second one
    When Sam renames the company "Acme" to "Acme Holdings"
    Then the company "Acme Holdings" keeps the id it had as "Acme"

  Scenario: A deal keeps its company across a rename
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR
    When Sam renames the company "Acme" to "Acme Holdings"
    Then the deal "Acme renewal" still belongs to the company now called "Acme Holdings"

  Scenario: A contact keeps its company across a rename
    Given a contact "Cara" at "Acme"
    When Sam renames the company "Acme" to "Acme Holdings"
    Then the contact "Cara" still works for "Acme Holdings"

  Scenario: A company cannot be renamed to nothing
    When Sam tries to rename the company "Acme" to " "
    Then the request is rejected because the name is blank
    And the companies are "Acme"

  Scenario: A company that does not exist cannot be renamed
    When Sam tries to rename a company that does not exist
    Then the request is rejected because the company does not exist

  Scenario: A contact is corrected
    Given a contact "Cara" at "Acme"
    When Sam corrects the contact "Cara" to "Cara Nguyen" with email "cara.nguyen@acme.test"
    Then the contact "Cara Nguyen" has email "cara.nguyen@acme.test"
    And there is no contact called "Cara"

  Scenario: A corrected contact stays at the same company
    Given a contact "Cara" at "Acme"
    When Sam corrects the contact "Cara" to "Cara Nguyen" with email "cara.nguyen@acme.test"
    Then the contact "Cara Nguyen" still works for "Acme"

  Scenario: A contact cannot be corrected to an invalid email address
    Given a contact "Cara" at "Acme"
    When Sam tries to correct the contact "Cara" to "Cara Nguyen" with email "not-an-email"
    Then the request is rejected because the email address is not valid
    And the contact "Cara" has email "cara@example.com"

  Scenario: A contact cannot be corrected to a blank name
    Given a contact "Cara" at "Acme"
    When Sam tries to correct the contact "Cara" to " " with email "cara@acme.test"
    Then the request is rejected because the name is blank
    And the contact "Cara" has email "cara@example.com"

  Scenario: A contact that does not exist cannot be corrected
    When Sam tries to correct a contact that does not exist
    Then the request is rejected because the contact does not exist
