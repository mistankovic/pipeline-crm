Feature: Who may move a deal

  A salesperson runs their own pipeline. A manager may step in on anybody's deal. Nobody
  else may touch a deal's stage, and a refused attempt changes nothing at all.

  Background:
    Given a salesperson "Sam"
    And a salesperson "Robin"
    And a manager "Mo"
    And a company "Acme"

  Scenario: The owner may move their own deal
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR
    When Sam moves "Acme renewal" to QUALIFIED
    Then the deal "Acme renewal" is in stage QUALIFIED

  Scenario: A manager may move somebody else's deal
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR
    When Mo moves "Acme renewal" to QUALIFIED
    Then the deal "Acme renewal" is in stage QUALIFIED

  Scenario: Another salesperson may not move it
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR
    When Robin tries to move "Acme renewal" to QUALIFIED
    Then the move is rejected because the user has no authority over the deal
    And the deal "Acme renewal" is in stage LEAD

  Scenario: Another salesperson may not close it either
    Given Sam owns a deal "Acme renewal" in stage NEGOTIATION worth 10000 EUR
    And a MEETING has been logged against "Acme renewal"
    When Robin tries to move "Acme renewal" to CLOSED_WON
    Then the move is rejected because the user has no authority over the deal
    And the deal "Acme renewal" is in stage NEGOTIATION

  Scenario: A manager may close somebody else's deal when the evidence is there
    Given Sam owns a deal "Acme renewal" in stage NEGOTIATION worth 10000 EUR
    And a MEETING has been logged against "Acme renewal"
    When Mo moves "Acme renewal" to CLOSED_WON
    Then the deal "Acme renewal" is in stage CLOSED_WON

  Scenario: A manager is still bound by the evidence rule
    Given Sam owns a deal "Acme renewal" in stage NEGOTIATION worth 10000 EUR
    When Mo tries to move "Acme renewal" to CLOSED_WON
    Then the move is rejected because the deal has not earned a win

  Scenario: An unknown deal cannot be moved
    When Sam tries to move an unknown deal to QUALIFIED
    Then the move is rejected because the deal does not exist

  Scenario: The board offers a salesperson no moves on somebody else's deal
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR
    When Robin looks at "Acme renewal"
    Then Robin is offered no way to move it

  Scenario: The board offers the owner the moves the pipeline allows
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR
    When Sam looks at "Acme renewal"
    Then Sam is offered the moves QUALIFIED, CLOSED_LOST

  Scenario: A manager is offered the same moves as the owner
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR
    When Mo looks at "Acme renewal"
    Then Mo is offered the moves QUALIFIED, CLOSED_LOST
