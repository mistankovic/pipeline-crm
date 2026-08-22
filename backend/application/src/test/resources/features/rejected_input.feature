Feature: Rejecting requests the system cannot honour

  These are the failures that belong to the use-case layer rather than to a business rule:
  something referred to does not exist, or a value arrived that is not a value at all. They
  are specified here so that Stage 3 implements a decided behaviour rather than an
  improvised one.

  Background:
    Given a salesperson "Sam"
    And a company "Acme"

  Scenario: A deal cannot be created for a company that does not exist
    When Sam creates a deal "Ghost" for an unknown company worth 1000 EUR at 50% probability
    Then the request is rejected because the company does not exist

  Scenario: A deal cannot be created with an owner who does not exist
    When a deal "Ghost" is created for "Acme" with an unknown owner
    Then the request is rejected because the user does not exist

  Scenario: A deal cannot be created in an unknown currency
    When Sam creates a deal "Ghost" for "Acme" worth 1000 XYZ at 50% probability
    Then the request is rejected because the value is not a valid amount

  Scenario: A deal cannot be created with an impossible probability
    When Sam creates a deal "Ghost" for "Acme" worth 1000 EUR at 150% probability
    Then the request is rejected because the probability is out of range

  Scenario: A contact cannot be created at a company that does not exist
    When Sam creates a contact "Ghost" at an unknown company
    Then the request is rejected because the company does not exist

  Scenario: A contact cannot be created with an address that is not an email address
    When Sam creates a contact "Cara" at "Acme" with email "not-an-address"
    Then the request is rejected because the email address is not valid

  Scenario: An activity cannot be logged by an author who does not exist
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR
    When an unknown user logs a CALL against "Acme renewal"
    Then the request is rejected because the user does not exist

  Scenario: An activity cannot be logged against a contact that does not exist
    When Sam logs a NOTE against an unknown contact
    Then the request is rejected because the contact does not exist

  Scenario: A timeline cannot be read for a deal that does not exist
    When the timeline of an unknown deal is requested
    Then the request is rejected because the deal does not exist
